package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.model.PageT;
import searchengine.model.SiteT;
import searchengine.model.Status;
import searchengine.repository.IndexTRepository;
import searchengine.repository.LemmaTRepository;
import searchengine.repository.PageTRepository;
import searchengine.repository.SiteTRepository;

@RequiredArgsConstructor
@Service
public class WebParseServiceImpl implements WebParseService, Runnable {

    private final SiteT siteT;
    private final PageTRepository pageTRepository;
    private final SiteTRepository siteTRepository;
    private final LemmaTRepository lemmaTRepository;
    private final IndexTRepository indexTRepository;
    @Override
    public void parseAllPages() {

    }

    @Override
    public void parseSinglePage(PageT pageT) {

    }

    @Override
    public void run() {
//        SiteParser sp = new SiteParser();
//        sp.init(siteT, 3);
//        sp.getLinks();
        if (siteT.getStatus() != Status.FAILED) {
            siteT.setStatus(Status.INDEXED);
            siteTRepository.save(siteT);
        }
//        poolList.remove(pool);
    }
}
