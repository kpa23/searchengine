package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteT;
import searchengine.repository.IndexTRepository;
import searchengine.repository.LemmaTRepository;
import searchengine.repository.PageTRepository;
import searchengine.repository.SiteTRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {


    private final Random random = new Random();
    private final SitesList sites;
    private final SiteTRepository siteTRepository;
    private final PageTRepository pageTRepository;
    private final LemmaTRepository lemmaTRepository;
    private final IndexTRepository indexTRepository;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            List<SiteT> optSite = siteTRepository.findByName(site.getName()).orElse(null);
            if (optSite == null || optSite.isEmpty()) continue;

            SiteT siteT = optSite.get(0);

            int pages = pageTRepository.countBySiteTBySiteId(siteT);

            int lemmas = lemmaTRepository.countBySiteTBySiteId(siteT);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(siteT.getStatus().toString());
            item.setError(siteT.getLastError());
            item.setStatusTime(siteT.getStatusTime().getTime());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
