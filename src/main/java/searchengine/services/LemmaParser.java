package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import searchengine.model.IndexT;
import searchengine.model.LemmaT;
import searchengine.model.PageT;
import searchengine.model.SiteT;
import searchengine.repository.IndexTRepository;
import searchengine.repository.LemmaTRepository;
import searchengine.repository.PageTRepository;
import searchengine.repository.SiteTRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class LemmaParser {

    private static final Logger logger = LogManager.getLogger(LemmaParser.class);
    private final PageTRepository pageTRepository;
    private final SiteTRepository siteTRepository;
    private final LemmaTRepository lemmaTRepository;
    private final IndexTRepository indexTRepository;
    private SiteT siteT;

    public LemmaParser clone() {
        return new LemmaParser(this.pageTRepository, this.siteTRepository, this.lemmaTRepository, this.indexTRepository);
    }

    public void parsePage(PageT pageT, boolean retry) {
        try {

//            String url;
//            if (pageT == null) {
////                url = page;
//            } else {
//                url = pageT.getPath();
//                if (retry) {
////                    deletePage(pageT, siteT);
//                }
//            }
            PageT finalPageT;
//            if (retry) {
//                ParsePage parsePage = new ParsePage(url, domain, siteT, pageTRepository, siteTRepository, parse, uniqueLinks);
//
//                Document d = parsePage.downloadAndSavePage();
//                finalPageT = parsePage.savePage(d.body().text(), d.title());
//            } else
            finalPageT = pageT;

            LemmaFinder l = LemmaFinder.getInstance();

            Map<String, Integer> lemmaMap = l.collectLemmas(finalPageT.getContent());
            Map<LemmaT, Integer> lemmaTList = new HashMap<>();
            List<IndexT> indexTList = new ArrayList<>();

            lemmaMap.entrySet().forEach(lemma -> lemmaTList.put(parseLemma(lemma.getKey()), lemma.getValue()));
            lemmaTRepository.saveAll(lemmaTList.keySet());
            lemmaTList.entrySet().forEach(e -> indexTList.add(new IndexT(finalPageT.getPageId(), e.getKey().getLemmaId(), e.getValue())));
            indexTRepository.saveAll(indexTList);
        } catch (IOException | NullPointerException e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
        }
    }

    public LemmaT parseLemma(String lemmaText) {
        return new LemmaT(siteT.getSiteId(), lemmaText, 1);
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

    public void setSiteT(SiteT siteT) {
        this.siteT = siteT;
    }

}
