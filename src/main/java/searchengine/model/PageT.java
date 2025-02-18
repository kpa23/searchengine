package searchengine.model;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.util.Collection;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "page_t", schema = "search_engine")
public class PageT {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "page_id", nullable = false)
    private int pageId;
    @NonNull
    @Column(name = "site_id", nullable = false)
    private int siteId;
    @NonNull
    @Basic(optional = false)
    @Column(name = "path", nullable = false, length =255)
    private String path;
    @NonNull
    @Basic(optional = false)
    @Column(name = "code", nullable = false)
    private int code;
    @NonNull
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;
    @NonNull
    @Column(name = "title", length = 255)
    private String title;
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "pageTByPageId", cascade = CascadeType.MERGE)
    private Collection<IndexT> indexTSByPageId;
    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "site_id", nullable = false, insertable = false, updatable = false)
    private SiteT siteTBySiteId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageT pageT = (PageT) o;

        if (pageId != pageT.pageId) return false;
        if (siteId != pageT.siteId) return false;
        if (code != pageT.code) return false;
        if (path != null ? !path.equals(pageT.path) : pageT.path != null) return false;
        return content != null ? content.equals(pageT.content) : pageT.content == null;
    }

    @Override
    public int hashCode() {
        int result = pageId;
        result = 31 * result + siteId;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + code;
        result = 31 * result + (content != null ? content.hashCode() : 0);
        return result;
    }
}
