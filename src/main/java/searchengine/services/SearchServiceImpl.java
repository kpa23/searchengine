package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.SitesList;
import searchengine.dto.search.*;
import searchengine.model.PageT;
import searchengine.model.SiteT;
import searchengine.repository.IndexTRepository;
import searchengine.repository.LemmaTRepository;
import searchengine.repository.PageTRepository;
import searchengine.repository.SiteTRepository;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SitesList sites;
    private final SiteTRepository siteTRepository;
    private final PageTRepository pageTRepository;
    private final LemmaTRepository lemmaTRepository;
    private final IndexTRepository indexTRepository;

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        SearchResponse searchResponse = new SearchResponse();
        try {
            SearchClassAllPages searchClassAllPages = new SearchClassAllPages();
            LemmaFinder l = LemmaFinder.getInstance();
            Map<String, Integer> lemmaMap = l.collectLemmas(query);
            lemmaMap.keySet().forEach(lem -> {
                SearchClassList searchClassList = new SearchClassList(lem);
                lemmaTRepository.findAllByLemma(lem)
                        .stream()
                        .map(lemT -> indexTRepository.findAllByLemmaTByLemmaId(lemT))
                        .forEach(e -> searchClassList.addList(e));
                searchClassAllPages.add(searchClassList);
            });
            searchClassAllPages.intersectAll();

            //
            //printing the sorted hashmap
            Set<Map.Entry<Integer, PageRel>> set = searchClassAllPages.getSortedMap().entrySet();
            for (Map.Entry me2 : set) {
                PageT pageT = pageTRepository.findByPageId((Integer) me2.getKey());
//                String text = pageT.getContent();
//                int pos = text.indexOf()
//                text = text.substring()
                SiteT siteT = siteTRepository.findByPageId(pageT.getPageId());
                SearchData searchData = new SearchData(siteT.getUrl(), siteT.getName(), pageT.getPath(), "title", "snippet", searchClassAllPages.getMapRank().get(me2.getKey()).getRelRank());
                searchResponse.dataAdd(searchData);
            }
            searchResponse.setResult(true);
            searchResponse.setCount(set.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchResponse;
    }
}
