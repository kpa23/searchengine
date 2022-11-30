package searchengine.dto.search;

import lombok.Data;
import searchengine.model.IndexT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Data
public class SearchClassList {
    private List<SearchClassData> data = new ArrayList<>();
    private final String searchLemma;

    public void addList(List<IndexT> list) {
        data = Stream.concat(data.stream(), list
                .stream()
                .map(e -> new SearchClassData(e.getPageId(), e.getRank()))
        ).toList();
    }

    public SearchClassList(String searchLemma) {
        this.searchLemma = searchLemma;
    }

    public void intersectData(List<SearchClassData> list) {
        data = data.stream()
                .filter(list::contains)
                .toList();
    }

}
