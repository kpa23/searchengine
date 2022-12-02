package searchengine.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexT;
import searchengine.model.LemmaT;
import searchengine.model.PageT;
import searchengine.model.SiteT;

import java.util.List;

@Repository
public interface IndexTRepository extends CrudRepository<IndexT, Integer> {
    List<IndexT> findByPageTByPageId(PageT pageT);

    List<IndexT> findAllByLemmaTByLemmaId(LemmaT lemmaT);

    @Query("select i from IndexT i join PageT  p on p.pageId = i.pageId where i.lemmaTByLemmaId = ?1 and p.siteTBySiteId = ?2")
    List<IndexT> findAllByLemmaTByLemmaIdAndSiteId(LemmaT lemmaT, SiteT siteT);
}
