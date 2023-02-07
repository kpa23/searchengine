package searchengine.model;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Collection;

@Getter
@Setter
@RequiredArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "site_t", schema = "search_engine")
public class SiteT {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "site_id", nullable = false)
    private int siteId;
    @NonNull
    @Basic(optional = false)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')")
    @Enumerated(EnumType.STRING)
    private Status status;
    @NonNull
    @Basic(optional = false)
    @Column(name = "status_time", nullable = false)
    private Timestamp statusTime;
    @Basic
    @Column(name = "last_error", length = 2000)
    private String lastError;
    @NonNull
    @Basic(optional = false)
    @Column(name = "url", nullable = false)
    private String url;
    @NonNull
    @Basic(optional = false)
    @Column(name = "name", nullable = false)
    private String name;
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "siteTBySiteId", cascade = CascadeType.MERGE)
    private Collection<LemmaT> lemmaTSBySiteId;
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "siteTBySiteId", cascade = CascadeType.MERGE)
    private Collection<PageT> pageTSBySiteId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SiteT siteT = (SiteT) o;

        if (siteId != siteT.siteId) return false;
        if (status != null ? !status.equals(siteT.status) : siteT.status != null) return false;
        if (statusTime != null ? !statusTime.equals(siteT.statusTime) : siteT.statusTime != null) return false;
        if (lastError != null ? !lastError.equals(siteT.lastError) : siteT.lastError != null) return false;
        if (url != null ? !url.equals(siteT.url) : siteT.url != null) return false;
        return name != null ? name.equals(siteT.name) : siteT.name == null;
    }

    @Override
    public int hashCode() {
        int result = siteId;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (statusTime != null ? statusTime.hashCode() : 0);
        result = 31 * result + (lastError != null ? lastError.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
