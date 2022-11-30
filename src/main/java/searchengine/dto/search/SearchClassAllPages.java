package searchengine.dto.search;

import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class SearchClassAllPages {
    List<SearchClassList> allPages = new ArrayList<>();
    HashMap<Integer, PageRel> mapRank = new HashMap<>();
    LinkedHashMap<Integer, PageRel> sortedMap;
    double maxRank = Double.MIN_VALUE;

    public void add(SearchClassList searchClassList) {
        allPages.add(searchClassList);
    }

    public void intersectAll() {
        SearchClassList searchClassList = allPages.get(0);
        // intersect first page with all others
        for (int i = 1; i < allPages.size(); i++) {
            searchClassList.intersectData(allPages.get(i).getData());
        }
        // intersect all others with first one
        allPages.set(0, searchClassList);
        for (int i = 1; i < allPages.size(); i++) {
            allPages.get(i).intersectData(searchClassList.getData());
        }
        // sum the ranks. Make page hashmap
        for (SearchClassList allPage : allPages) {
            allPage.getData()
                    .forEach(e -> {
                                PageRel s = mapRank.getOrDefault(e.getPageId(), new PageRel(0.0));
                                s.setRankSum(s.getRankSum() + e.getRank());
                                if (maxRank < s.getRankSum()) maxRank = s.getRankSum();
                                mapRank.put(e.getPageId(), s);
                            }
                    );
        }
        mapRank.entrySet().forEach(e -> e.getValue().setRelRank(e.getValue().getRankSum() / maxRank));
        // make sorted hashmap
        Comparator<PageRel> byRel = Comparator.comparingDouble(PageRel::getRelRank);
        sortedMap = mapRank.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder(byRel)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        System.out.println("---");
    }
}
