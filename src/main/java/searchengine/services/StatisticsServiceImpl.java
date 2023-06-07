package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Status;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    private final Random random = new Random();
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
        String[] errors = {
                "Ошибка индексации: главная страница сайта не доступна",
                "Ошибка индексации: сайт не доступен",
                ""
        };

        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for(int i = 0; i < sitesList.size(); i++) {
            Site site = sitesList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages;
            int lemmas;
            if (!siteRepository.findAll().isEmpty()) {
                searchengine.model.Site s = siteRepository.findByName(site.getName());
                pages = pageRepository.findBySiteId(s.getId()).size();
                lemmas = lemmaRepository.findBySiteId(s.getId()).size();
            } else {
                pages = 0;
                lemmas = 0;
            }
            item.setPages(pages);
            item.setLemmas(lemmas);

            if (siteRepository.findAll().isEmpty() ||
                    siteRepository.findByName(site.getName()) == null) {
                item.setStatus(statuses[2]);
                item.setError(errors[2]);
            } else if (siteRepository.findByName(site.getName()).getStatus().equals(Status.INDEXED)) {
                item.setStatus(statuses[0]);
            } else if (siteRepository.findByName(site.getName()).getStatus().equals(Status.FAILED)) {
                if (pageRepository.findByPath(site.getUrl()) == null) {
                    item.setError(errors[2]);
                } else if (pageRepository.findByPath(site.getUrl()).getCode() == 404) {;
                    item.setError(errors[0]);
                } else if (pageRepository.findByPath(site.getUrl()).getCode() == 403) {
                    item.setError(errors[1]);
                }
                item.setStatus(statuses[1]);
            } else {
                item.setStatus(statuses[2]);
                item.setError(errors[2]);
            }
            ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault());
            long date = zdt.toInstant().toEpochMilli();
            item.setStatusTime(date);
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