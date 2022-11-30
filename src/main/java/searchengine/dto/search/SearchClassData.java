package searchengine.dto.search;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
@Data
public class SearchClassData implements Comparable<SearchClassData> {
    private final int pageId;
    private final double rank;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchClassData that = (SearchClassData) o;
        return pageId == that.pageId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageId);
    }

    @Override
    public int compareTo(SearchClassData o) {
        return 0;
    }
}
