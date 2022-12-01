package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import searchengine.config.Parse;
import searchengine.model.PageT;
import searchengine.model.SiteT;
import searchengine.model.Status;
import searchengine.repository.*;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class SiteParser implements Runnable {
    private static final Logger logger = LogManager.getLogger(SiteParser.class);
    private static ConcurrentLinkedQueue<ForkJoinPool> poolList = new ConcurrentLinkedQueue<>();
    private ForkJoinPool pool;
    private String domain;
    private String url;
    private int parallelism;
    private SiteT siteT;
    private final PageTRepository pageTRepository;
    private final SiteTRepository siteTRepository;
    private final LemmaTRepository lemmaTRepository;
    private final IndexTRepository indexTRepository;
    private final LemmaParser lemmaParser;
    private final Parse parse;

    public SiteParser clone() {
        return new SiteParser(this.pageTRepository, this.siteTRepository, this.lemmaTRepository, this.indexTRepository, this.lemmaParser, this.parse);
    }

    public void init(SiteT siteT, int parallelism) {
        this.url = siteT.getUrl();
        this.domain = Utils.getProtocolAndDomain(url);
        this.parallelism = parallelism;
        this.siteT = siteT;
    }

    public void getLinks() {
        pool = new ForkJoinPool(this.parallelism);
        poolList.add(pool);
        ParsePage parsedMap = new ParsePage("/", domain, siteT, pageTRepository, siteTRepository, parse, null);
        StringBuilder sb = new StringBuilder();
        sb.append("************** \n\t%s *******************\n");
        sb.append("Main: Parallelism: %d\n");
        sb.append("Main: Active Threads: %d\n");
        sb.append("Main: Task Count: %d\n");
        sb.append("Main: Steal Count: %d\n");
        sb.append("******************************************\n");
        pool.execute(parsedMap);
        do {
            logger.log(Level.INFO, () -> String.format(sb.toString(), this.domain
                    , pool.getParallelism()
                    , pool.getActiveThreadCount()
                    , pool.getQueuedTaskCount()
                    , pool.getStealCount()));
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        } while (!parsedMap.isDone());
        //Shut down ForkJoinPool using the shutdown() method.
        pool.shutdown();
        List<String> results;
        try {
            results = parsedMap.join();
            logger.log(Level.INFO, () -> String.format("%s: %d links found.", url, results.size()));
            parseAllPages();
            logger.info("Lemmization complete.");
        } catch (CancellationException e) {
            logger.info("All tasks canceled");
        }
    }

    public void parseAllPages() {
        List<PageT> pageTList = pageTRepository.findBySiteTBySiteIdAndCode(siteT, 200);
        pageTList.forEach(pageT -> {
            LemmaParser newLemmaParser = lemmaParser.clone();
            newLemmaParser.setSiteT(siteT);
            newLemmaParser.parsePage(pageT, false);
        });

    }


    @Override
    public void run() {
        this.getLinks();
        if (siteT.getStatus() != Status.FAILED) {
            siteT.setStatus(Status.INDEXED);
            siteTRepository.save(siteT);
        }
        poolList.remove(pool);
    }

    public static void forceStop() {
        if (poolList != null && !poolList.isEmpty()) {
            poolList.forEach(ForkJoinPool::shutdownNow);
            poolList = new ConcurrentLinkedQueue<>();
        }
    }
}
