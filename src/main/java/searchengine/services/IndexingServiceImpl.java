package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.*;
import searchengine.repository.*;

import javax.transaction.Transactional;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final SiteTRepository siteTRepository;
    private final PageTRepository pageTRepository;
    private final SiteParser siteParser;
    private final List<SiteT> siteTList = new ArrayList<>();

    @Override
    @Transactional
    public IndexingResponse getStartIndexing() {
        IndexingResponse response = new IndexingResponse();
        List<Site> sitesList = sites.getSites();
        if (sitesList.stream()
                .map(e -> siteTRepository.countByNameAndStatus(e.getName(), Status.INDEXING))
                .reduce(0, Integer::sum) > 0) {
            response.setResult(false);
            String ERROR_STARTED = "Индексация уже запущена";
            response.setError(ERROR_STARTED);
        } else {

            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
            executor.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
            sitesList.forEach(e -> {
                SiteParser sp = siteParser.clone();
                String name = e.getName();
                Optional<List<SiteT>> byName = siteTRepository.findByName(name);
                if (byName.isPresent()) {
                    siteTRepository.deleteAllByName(name);
                }
                SiteT siteT = new SiteT(Status.INDEXING, Utils.getTimeStamp(), e.getUrl(), e.getName());
                siteTRepository.save(siteT);
                siteTList.add(siteT);
                sp.init(siteT, 1);
                executor.execute(sp);

            });
            response.setResult(true);
            response.setError(sitesList.toString()); //TODO: fix response
        }
        return response;
    }



    @Override
    public IndexingResponse getStopIndexing() {
        IndexingResponse response = new IndexingResponse();
        try {
//            List<SiteT> siteTList = siteTRepository.findAllByStatus(Status.INDEXING).orElseThrow();
            long size = siteTList.stream().filter(e -> e.getStatus() == Status.INDEXING).count();
            if (size == 0) {
                response.setResult(false);
                String ERROR_STOPPED = "Индексация не запущена";
                response.setError(ERROR_STOPPED);
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
        SiteParser sp = siteParser.clone();
        String domain = Utils.getProtocolAndDomain(url);
        String page = url.replace(domain, "");
        SiteT siteT = siteTRepository.findByUrl(domain);
        PageT pageT = pageTRepository.findBySiteTBySiteIdAndPath(siteT, page);
        if (siteT != null ) {
            sp.init(siteT, 1);
            sp.parsePage(pageT, page);
        }
        else {
            response.setResult(false);
            String ERROR_SINGLE = "Данная страница находится за пределами сайтов,\n" +
                    "указанных в конфигурационном файле";
            response.setError(ERROR_SINGLE);
        }
        System.out.println("page complete");

        return response;
    }


}
