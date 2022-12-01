package searchengine.model;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.SQLInsert;

import javax.persistence.*;
import java.util.Collection;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "lemma_t", schema = "search_engine")
@SQLInsert(sql = "insert into lemma_t(frequency,lemma, site_id ) values (?, ?, ?) on duplicate key update frequency = lemma_t.frequency + 1")
public class LemmaT {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "lemma_id", nullable = false)
    private int lemmaId;
    @NonNull
    @Column(name = "site_id", nullable = false)
    private int siteId;
    @NonNull
    @Column(name = "lemma", nullable = false)
    private String lemma;
    @NonNull
    @Column(name = "frequency", nullable = false)
    private int frequency;
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "lemmaTByLemmaId", cascade = CascadeType.ALL)
    private Collection<IndexT> indexTSByLemmaId;
    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "site_id", nullable = false, insertable = false, updatable = false)
    private SiteT siteTBySiteId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LemmaT lemmaT = (LemmaT) o;

        if (lemmaId != lemmaT.lemmaId) return false;
        if (siteId != lemmaT.siteId) return false;
        if (frequency != lemmaT.frequency) return false;
        return lemma != null ? lemma.equals(lemmaT.lemma) : lemmaT.lemma == null;
    }

    @Override
    public int hashCode() {
        int result = lemmaId;
        result = 31 * result + siteId;
        result = 31 * result + (lemma != null ? lemma.hashCode() : 0);
        result = 31 * result + frequency;
        return result;
    }
}
