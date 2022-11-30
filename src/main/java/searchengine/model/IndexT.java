package searchengine.model;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "index_t", schema = "search_engine")
public class IndexT {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "index_id", nullable = false)
    private int indexId;
    @NonNull
    @Column(name = "page_id", nullable = false)
    private int pageId;
    @NonNull
    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;
    @NonNull
    @Column(name = "rank_index", nullable = false)
    private double rank;
    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "page_id", nullable = false, insertable = false, updatable = false)
    private PageT pageTByPageId;
    @ManyToOne
    @JoinColumn(name = "lemma_id", referencedColumnName = "lemma_id", nullable = false, insertable = false, updatable = false)
    private LemmaT lemmaTByLemmaId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexT indexT = (IndexT) o;

        if (indexId != indexT.indexId) return false;
        if (pageId != indexT.pageId) return false;
        if (lemmaId != indexT.lemmaId) return false;
        return Double.compare(indexT.rank, rank) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = indexId;
        result = 31 * result + pageId;
        result = 31 * result + lemmaId;
        temp = Double.doubleToLongBits(rank);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

}
