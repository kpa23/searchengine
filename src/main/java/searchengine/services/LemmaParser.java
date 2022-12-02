package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public LemmaParser copy() {
        return new LemmaParser(this.pageTRepository, this.siteTRepository, this.lemmaTRepository, this.indexTRepository);
    }

    @Transactional
    public void parsePage(PageT pageT) {
        try {
            LemmaFinder l = LemmaFinder.getInstance();
            Map<String, Integer> lemmaMap = l.collectLemmas(pageT.getContent());
            Map<LemmaT, Integer> lemmaTList = new HashMap<>();
            List<IndexT> indexTList = new ArrayList<>();
            lemmaMap.entrySet().forEach(lemma -> lemmaTList.put(parseLemma(lemma.getKey()), lemma.getValue()));
            lemmaTRepository.saveAll(lemmaTList.keySet());
            lemmaTList.entrySet().forEach(e -> indexTList.add(new IndexT(pageT.getPageId(), e.getKey().getLemmaId(), e.getValue())));
            indexTRepository.saveAll(indexTList);
        } catch (NullPointerException e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
        }
    }

    public LemmaT parseLemma(String lemmaText) {
        LemmaT result = lemmaTRepository.findByLemmaAndSiteTBySiteId(lemmaText, siteT);
        if (result == null) {
            result = new LemmaT(siteT.getSiteId(), lemmaText, 1);
        } else {
            result.setFrequency(result.getFrequency() + 1);
        }
        return result;
    }


    public void setSiteT(SiteT siteT) {
        this.siteT = siteT;
    }

}
