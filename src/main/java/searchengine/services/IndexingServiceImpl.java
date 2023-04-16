package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.FakeUser;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.responses.FalseIndexingResponse;
import searchengine.dto.indexing.responses.TrueIndexingResponse;
import searchengine.lemmatizator.Lemmatizator;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final FakeUser fakeUser;
    private final SitesList sitesList;
    private static List<ForkJoinPool> pools = new ArrayList<>();
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    private static List<Thread> activeThreads = new ArrayList<>();

    @Override
    public IndexingResponse startIndexing() {
        IndexingResponse response;
        if (!siteRepository.findByStatus(Status.INDEXING).isEmpty()) {
            RequestStatus.setStatus(HttpStatus.BAD_REQUEST.value());
            response = new FalseIndexingResponse("Индексация уже запущена");
            return response;
        }
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        lemmaRepository.deleteAll();
        indexRepository.deleteAll();

        for (Site site : sitesList.getSites()) {
            Thread t = new Thread(() -> {
                searchengine.model.Site s = getSite(site);
                Executor executor = new Executor(s.getUrl(), s, fakeUser,
                        pageRepository, siteRepository, lemmaRepository, indexRepository);
                ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime()
                        .availableProcessors());
                pool.invoke(executor);
                pools.add(pool);
                pool.shutdown();

                if (!Thread.currentThread().isInterrupted() && Executor.isActive) {
                    synchronized (siteRepository) {
                        s = siteRepository.findByName(s.getName());
                        s.setStatus(Status.INDEXED);
                        siteRepository.save(s);
                    }
                }
            });
            t.start();
            activeThreads.add(t);
        }
        RequestStatus.setStatus(HttpStatus.OK.value());
        response = new TrueIndexingResponse();
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {
        IndexingResponse response;
        if (siteRepository.findByStatus(Status.INDEXING).isEmpty()) {
            RequestStatus.setStatus(HttpStatus.BAD_REQUEST.value());
            response = new FalseIndexingResponse("Индексация не запущена");
            return response;
        }
        activeThreads.forEach(Thread::interrupt);
        Executor.isActive = false;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        synchronized (siteRepository) {
            siteRepository.findByStatus(Status.INDEXING).forEach(s -> {
                s.setStatus(Status.FAILED);
                siteRepository.save(s);
            });
        }
        RequestStatus.setStatus(HttpStatus.OK.value());
        response = new TrueIndexingResponse();
        return response;
    }

    @Override
    public IndexingResponse indexPage(String url) {
        IndexingResponse response;
        if (!checkConfigurationSites(url)) {
            response = new FalseIndexingResponse("Данная страница " +
                    "находится за пределами сайтов, " +
                    "указанных в конфигурационном файле");
            RequestStatus.setStatus(HttpStatus.BAD_REQUEST.value());
            return response;
        }
        Lemmatizator lemmatizator = new Lemmatizator();
        try {
            Connection connection = Jsoup.connect(url);
            Document document = connection.get();
            String html = document.html();
            HashMap<String, Integer> lemmas = lemmatizator.createLemmas(html);
            addPageToDb(html, url, connection, lemmas);
            addLemmasToDb(lemmas, url);
            RequestStatus.setStatus(connection.response().statusCode());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        response = new TrueIndexingResponse();
        return response;
    }

    private boolean checkConfigurationSites(String page) {
        boolean result = false;
        String url = getSiteUrl(page);
        for (Site s : sitesList.getSites()) {
            if (s.getUrl().equals(url)) {
                result = true;
                break;
            }
        }
        return result;
    }

    private void addPageToDb(String html, String uri, Connection connection, HashMap<String, Integer> lemmas)
            throws MalformedURLException {
        for (Site s : sitesList.getSites()) {
            if (s.getUrl().equals(getSiteUrl(uri))) {
                Connection.Response response = connection.response();
                searchengine.model.Site site = getSite(s);
                Page p = pageRepository.findByPath(uri);
                if (p != null) {
                    for (Map.Entry<String,Integer> entry : lemmas.entrySet()) {
                        Lemma l = lemmaRepository.findByLemma(entry.getKey());
                        List<Index> index = indexRepository.findByLemma(l);
                        indexRepository.deleteAll(index);
                        lemmaRepository.delete(l);
                    }
                    pageRepository.delete(p);
                }
                Page page = new Page();
                page.setPath(uri);
                page.setSite(site);
                page.setCode(response.statusCode());
                RequestStatus.setStatus(response.statusCode());
                page.setSiteId(site.getId());
                page.setContent(html);
                pageRepository.save(page);
            }
        }
    }

    private searchengine.model.Site getSite(Site s) {
        searchengine.model.Site site = new searchengine.model.Site();
        if (siteRepository.findByName(s.getName()) != null) {
            return siteRepository.findByName(s.getName());
        }
        site.setUrl(s.getUrl());
        site.setName(s.getName());
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
        System.out.println("Сайт найден!");
        return site;
    }

    private void addLemmasToDb(HashMap<String, Integer> lemmas, String uri) {
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemma = entry.getKey();
            int count = entry.getValue();
            Lemma lem;
            if (lemmaRepository.findByLemma(lemma) == null) {
                lem = new Lemma();
                lem.setLemma(lemma);
                lem.setFrequency(1);
                searchengine.model.Site site =
                        siteRepository.findByName(getSiteName(uri));
                lem.setSiteId(site.getId());
            } else {
                lem = lemmaRepository.findByLemma(lemma);
                lem.setFrequency(lem.getFrequency() + 1);
            }
            lemmaRepository.save(lem);
            addToIndex(lem, count, uri);
        }
    }

    private void addToIndex (Lemma lemma, int countOnPage, String page) {
        Page p = pageRepository.findByPath(page);
        Index index = new Index();
        index.setLemma(lemma);
        index.setPage(p);
        index.setRank(countOnPage);
        indexRepository.save(index);
    }

    private String getSiteUrl (String uri) {
        URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url.getProtocol() + "://" + url.getHost();
    }

    private String getSiteName (String uri) {
        URL url;
        try {
            url = new URL(uri);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return url.getHost();
    }
}