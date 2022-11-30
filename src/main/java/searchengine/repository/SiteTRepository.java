package searchengine.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteT;
import searchengine.model.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteTRepository extends CrudRepository<SiteT, Integer> {
    Optional<List<SiteT>> findByName(String name);

    void deleteAllByName(String name);

    int countByNameAndStatus(String name, Status status);

    int countByStatus(Status status);

    Optional<List<SiteT>> findAllByStatus(Status status);

    SiteT findByUrl(String url);

    @Query(value = "select s from SiteT s join PageT p on s.siteId = p.siteId where p.pageId =?1")
    SiteT findByPageId(int page);
}
