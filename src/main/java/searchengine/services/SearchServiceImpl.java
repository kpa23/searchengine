package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import searchengine.dto.search.*;
import searchengine.model.IndexT;
import searchengine.model.PageT;
import searchengine.model.SiteT;
import searchengine.repository.IndexTRepository;
import searchengine.repository.LemmaTRepository;
import searchengine.repository.PageTRepository;
import searchengine.repository.SiteTRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private final SiteTRepository siteTRepository;
    private final PageTRepository pageTRepository;
    private final LemmaTRepository lemmaTRepository;
    private final IndexTRepository indexTRepository;
    private LemmaFinder lemmaFinder = LemmaFinder.getInstance();
    private List<String> queryLemmas = new ArrayList<>();
    private static final Logger logger = LogManager.getLogger(SearchServiceImpl.class);

    @Override
    public SearchResponse search(String query, String site, Integer offset, Integer limit) {
        queryLemmas = new ArrayList<>();
        SearchResponse searchResponse = new SearchResponse();
        try {
            SearchClassAllPages searchClassAllPages = new SearchClassAllPages();

            SiteT searchSite = siteTRepository.findByUrl(site);
            Map<LemmaItem, Integer> lemmaMap = lemmaFinder.collectLemmasMap(query);
            lemmaMap.keySet().forEach(lem -> {
                queryLemmas.add(lem.getLemma());
                SearchClassList searchClassList = new SearchClassList(lem.getLemma());
                lemmaTRepository.findAllByLemma(lem.getLemma())
                        .stream()
                        .map(lemT -> {
                            List<IndexT> res;
                            if (site == null) {
                                res = indexTRepository.findAllByLemmaTByLemmaId(lemT);
                            } else {
                                res = indexTRepository.findAllByLemmaTByLemmaIdAndSiteId(lemT, searchSite);
                            }
                            return res;
                        })
                        .forEach(searchClassList::addList);
                searchClassAllPages.add(searchClassList);
            });
            if (searchClassAllPages.size() >= 1) {
                searchClassAllPages.intersectAll();
            }
            if (searchClassAllPages.size() == 0) {
                throw new IOException("bad request");
            }
            //
            //printing the sorted hashmap
            Set<Map.Entry<Integer, PageRel>> set = searchClassAllPages.getSortedMap().entrySet();

            int cnt = 0;
            for (Map.Entry<Integer, PageRel> me2 : set) {
                cnt++;
                if (cnt <= offset) continue;
                if (cnt > limit + offset) break;
                PageT pageT = pageTRepository.findByPageId(me2.getKey());
                String text = pageT.getContent().replaceAll("\\s{2,}", " ").trim();
                Map<Integer, String> textLemList = lemmaFinder.collectLemmasList(text);
                String word = lemmaMap.keySet().stream().findAny().orElseThrow().getLemma();
                int pos = textLemList.entrySet()
                        .stream()
                        .filter(e -> e.getValue().toLowerCase(Locale.ROOT).equals(word.toLowerCase(Locale.ROOT)))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse(0);
                String snippet = "..." + getAt(text, pos) + "...";
                SiteT siteT = siteTRepository.findByPageId(pageT.getPageId());
                SearchData searchData = new SearchData(siteT.getUrl(), siteT.getName(), pageT.getPath(), pageT.getTitle(), snippet, searchClassAllPages.getMapRank().get(me2.getKey()).getRelRank());
                searchResponse.dataAdd(searchData);
            }
            logger.log(Level.forName("DIAG", 350), "query = \t" + query);
            searchResponse.setResult(true);
            searchResponse.setCount(set.size());
        } catch (IOException e) {
            logger.log(Level.forName("BAD", 380), "wrong query = \t" + query);
            searchResponse.setResult(false);
        }
        return searchResponse;
    }


    public String getAt(String st, int pos) {
        StringBuilder sb = new StringBuilder();
        String[] tokens = st.split(" ");

        int pre = 10;
        int post = 10;
        if (pos < pre) {
            pre = pos;
            post = post + 10 - pos;
        }
        if (pos > tokens.length - post) {
            post = tokens.length - post - 1;
            pre = pre + (10 - post);
        }

        for (int i = pos - pre; i < pos + post; i++) {
            if (lemmaFinder.collectLemmas(tokens[i]).size() > 0 &&
                    queryLemmas.contains(lemmaFinder.collectLemmas(tokens[i]).keySet().stream().findFirst().orElse(""))) {
                sb.append("<b>").append(tokens[i]).append("</b>");
            } else {
                sb.append(tokens[i]);
            }
            sb.append(" ");
        }

        return sb.toString();
    }

}
