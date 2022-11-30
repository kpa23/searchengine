package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexT;
import searchengine.model.LemmaT;
import searchengine.model.PageT;
import searchengine.model.SiteT;

import java.util.List;

@Repository
public interface IndexTRepository extends CrudRepository<IndexT, Integer> {
    @Transactional
    void deleteIndexTSByPageTByPageId(PageT pageT);
    List<IndexT> findByPageTByPageId(PageT pageT);
    List<IndexT> findAllByLemmaTByLemmaId(LemmaT lemmaT);
}
