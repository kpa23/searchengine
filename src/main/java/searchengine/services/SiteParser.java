package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Parse;
import searchengine.model.IndexT;
import searchengine.model.PageT;
import searchengine.model.SiteT;
import searchengine.model.Status;
import searchengine.repository.*;

import java.io.IOException;
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
//    private final ParsePage;

    public SiteParser copy() {
        return new SiteParser(this.pageTRepository, this.siteTRepository, this.lemmaTRepository, this.indexTRepository, this.lemmaParser);
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
        PageParse parsedMap = new PageParse("/", domain, siteT, pageTRepository, siteTRepository, null);
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
        pageTList.forEach(this::parseSinglePage);

    }

    public void parseSinglePage(PageT pageT) {
        LemmaParser newLemmaParser = lemmaParser.copy();
        newLemmaParser.setSiteT(siteT);
        newLemmaParser.parsePage(pageT);
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


    public void deletePage(PageT pageT, SiteT siteT) {
        List<IndexT> indexTList = indexTRepository.findByPageTByPageId(pageT);
        indexTList.forEach(e ->
                lemmaTRepository
                        .findAllByLemmaId(e.getLemmaId())
                        .forEach(lemmaT -> lemmaT.setFrequency(lemmaT.getFrequency() - 1))
        );
        lemmaTRepository.deleteBySiteTBySiteIdAndFrequency(siteT, 0);
        pageTRepository.delete(pageT);
    }

    public void reloadPage(String uri) {
        PageT pageT = pageTRepository.findBySiteTBySiteIdAndPath(siteT, uri);
        if (pageT != null) {
            deletePage(pageT, siteT);
        }
        PageParse PageParse = new PageParse(uri, domain, siteT, pageTRepository, siteTRepository, null);
        try {
            PageParse.downloadAndSavePage();
        } catch (IOException ignored) {//
        }
        pageT = pageTRepository.findBySiteTBySiteIdAndPath(siteT, uri);
        parseSinglePage(pageT);
    }
}
