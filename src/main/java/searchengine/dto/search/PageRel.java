package searchengine.dto.search;

import lombok.Data;

import java.util.Objects;

@Data
public class PageRel {
    private double rankSum;
    private double relRank;

    public PageRel(double rank) {
        this.rankSum = rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageRel pageRel = (PageRel) o;
        return Double.compare(pageRel.relRank, relRank) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(relRank);
    }
}
