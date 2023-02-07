package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.SiteT;
import searchengine.model.Status;
import searchengine.repository.SiteTRepository;
import searchengine.repository.Utils;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final SiteTRepository siteTRepository;
    private final SiteParser siteParser;
    private final List<SiteT> siteTList = new ArrayList<>();
    private static final Logger logger = LogManager.getLogger(IndexingServiceImpl.class);

    @Override
    @Transactional
    public IndexingResponse getStartIndexing() {
        IndexingResponse response = new IndexingResponse();
        List<Site> sitesList = sites.getSites();
        if (sitesList.stream()
                .map(e -> siteTRepository.countByNameAndStatus(e.getName(), Status.INDEXING))
                .reduce(0, Integer::sum) > 0) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
        } else {

            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);
            executor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
            sitesList.forEach(e -> {
                SiteParser sp = siteParser.copy();
                String name = e.getName();
                Optional<List<SiteT>> byName = siteTRepository.findByName(name);
                if (byName.isPresent()) {
                    siteTRepository.deleteAllByName(name);
                }
                SiteT siteT = new SiteT(Status.INDEXING, Utils.getTimeStamp(), e.getUrl(), e.getName());
                siteTRepository.save(siteT);
                siteTList.add(siteT);
                sp.init(siteT, 3);
                executor.execute(sp);

            });
            response.setResult(true);
        }
        return response;
    }


    @Override
    public IndexingResponse getStopIndexing() {
        IndexingResponse response = new IndexingResponse();
        try {
            long size = siteTList.stream().filter(e -> e.getStatus() == Status.INDEXING).count();
            if (size == 0) {
                response.setResult(false);
                response.setError("Индексация не запущена");
            } else {
                SiteParser.forceStop();
                siteTList.stream()
                        .filter(e -> e.getStatus() == Status.INDEXING)
                        .forEach(e -> {
                            e.setStatus(Status.FAILED);
                            e.setStatusTime(Utils.getTimeStamp());
                            e.setLastError("Индексация остановлена пользователем");
                        });
                siteTRepository.saveAll(siteTList);

                response.setResult(true);
            }
        } catch (Exception e) {
            response.setResult(false);
            response.setError(e.getMessage());
        }
        return response;
    }

    @Override
    public IndexingResponse indexPage(String url) {
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        SiteParser sp = siteParser.copy();
        try {
            String domain = Utils.getProtocolAndDomain(url);

            String uri = url.replace(domain, "");
            SiteT siteT = siteTRepository.findByUrl(domain);
            if (siteT != null) {
                sp.init(siteT, 1);
                sp.reloadPage(uri);
            } else {
                throw new IllegalArgumentException("bad site");
            }
            logger.info("page complete");
        } catch (Exception e) {
            response.setResult(false);
            response.setError("Данная страница находится за пределами сайтов,\n" +
                    "указанных в конфигурационном файле");
        }
        return response;
    }


}
