package searchengine.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaT;
import searchengine.model.SiteT;

import java.util.List;

@Repository
public interface LemmaTRepository extends CrudRepository<LemmaT, Integer> {

    List<LemmaT> findAllByLemmaId(int lemmaId);

    List<LemmaT> findAllByLemma(String lemma);
    LemmaT findByLemmaAndSiteTBySiteId(String lemma, SiteT siteT);
    @Transactional
    void deleteBySiteTBySiteIdAndFrequency(SiteT siteT, int frequency);

    int countBySiteTBySiteId(SiteT siteT);

}
