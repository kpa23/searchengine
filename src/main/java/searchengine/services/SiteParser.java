package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Parse;
import searchengine.model.*;
import searchengine.repository.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
    private final Parse parse;
    private final ConcurrentHashMap<String, ParsePage> uniqueLinks = new ConcurrentHashMap<>();

    public SiteParser clone() {
        return new SiteParser(this.pageTRepository, this.siteTRepository, this.lemmaTRepository, this.indexTRepository, this.parse);
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
        ParsePage parsedMap = new ParsePage("/", domain, siteT, pageTRepository, siteTRepository, parse, uniqueLinks);

        pool.execute(parsedMap);
        do {
            System.out.printf("************** %s *******************\n", this.domain);
            System.out.printf("Main: Parallelism: %d\n", pool.getParallelism());
            System.out.printf("Main: Active Threads: %d\n", pool.getActiveThreadCount());
            System.out.printf("Main: Task Count: %d\n", pool.getQueuedTaskCount());
            System.out.printf("Main: Steal Count: %d\n", pool.getStealCount());
            System.out.print("******************************************\n");
            try {
                TimeUnit.SECONDS.sleep(5);
//                pool.shutdownNow();
            } catch (InterruptedException e) {
                logger.error(ExceptionUtils.getStackTrace(e));
            }
        } while (!parsedMap.isDone());
        //Shut down ForkJoinPool using the shutdown() method.
        pool.shutdown();
        List<String> results;
        try {
            results = parsedMap.join();
            System.out.printf("%s: %d links found.\n", url, results.size());
            parseAllPages();
            System.out.printf("Lemmization complete.\n");
        } catch (CancellationException e) {
            System.out.printf("All tasks canceled\n");
        }

    }

    public void parseAllPages() {
        List<PageT> pageTList = pageTRepository.findBySiteTBySiteIdAndCode(siteT, 200);
        pageTList.forEach(pageT -> parsePage(pageT, null));

    }

    public void parsePage(PageT pageT, String page) {
        try {

            String url;
            if (pageT == null) {
                url = page;
            } else {
                url = pageT.getPath();
                deletePage(pageT, siteT);
            }

            ParsePage parsePage = new ParsePage(url, domain, siteT, pageTRepository, siteTRepository, parse, uniqueLinks);


            PageT finalPageT = parsePage.savePage(parsePage.downloadAndSavePage().text());

            LemmaFinder l = LemmaFinder.getInstance();
            Map<LemmaT, Integer> lemmaTList = new HashMap<>();
            List<IndexT> indexTList = new ArrayList<>();
            Map<String, Integer> lemmaMap = l.collectLemmas(finalPageT.getContent());


            lemmaMap.entrySet().forEach(lemma -> lemmaTList.put(parseLemma(lemma.getKey()), lemma.getValue()));
            lemmaTRepository.saveAll(lemmaTList.keySet());
            lemmaTList.entrySet().forEach(e -> indexTList.add(new IndexT(finalPageT.getPageId(), e.getKey().getLemmaId(), e.getValue())));


            indexTRepository.saveAll(indexTList);
        } catch (IOException|NullPointerException e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
        }
    }

    public LemmaT parseLemma(String lemmaText) {
        LemmaT lemmaT;
        lemmaT = lemmaTRepository.findBySiteTBySiteIdAndLemma(siteT, lemmaText);
        if (lemmaT == null) {
            lemmaT = new LemmaT(siteT.getSiteId(), lemmaText, 1);
        } else {
            lemmaT.setFrequency(lemmaT.getFrequency() + 1);
        }
        return lemmaT;
    }

    @Transactional
    public void deletePage(PageT pageT, SiteT siteT) {
        List<IndexT> indexTList = indexTRepository.findByPageTByPageId(pageT);
        indexTList.forEach(e ->
                lemmaTRepository
                        .findAllByLemmaId(e.getLemmaId())
                        .forEach(lemmaT -> lemmaT.setFrequency(lemmaT.getFrequency() - 1))
        );

        lemmaTRepository.deleteBySiteTBySiteIdAndFrequency(siteT, 0);
//        indexTRepository.deleteIndexTSByPageTByPageId(pageT);
        pageTRepository.delete(pageT);
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
        if (poolList != null && poolList.size() > 0) {
            poolList.forEach(ForkJoinPool::shutdownNow);
            poolList = new ConcurrentLinkedQueue<>();
        }
    }
}
