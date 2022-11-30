package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageT;
import searchengine.model.SiteT;

import java.util.List;

@Repository
public interface PageTRepository extends CrudRepository<PageT, Integer> {
    List<PageT> findBySiteTBySiteIdAndCode(SiteT site, int code);

    PageT findBySiteTBySiteIdAndPath(SiteT site, String path);

    int countBySiteTBySiteId(SiteT siteT);

    PageT findBySiteId(int siteId);
    PageT findByPageId(int PageId);
}
