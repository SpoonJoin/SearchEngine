package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.FakeUser;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import searchengine.model.Status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

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
    private static List<Thread> activeThreads = new ArrayList<>();

    @Override
    public IndexingResponse startIndexing() {
        siteRepository.deleteAll();
        pageRepository.deleteAll();
        for (Site site : sitesList.getSites()) {
            Thread t = new Thread(() -> {
                searchengine.model.Site s = new searchengine.model.Site();
                s.setUrl(site.getUrl());
                s.setName(site.getName());
                s.setStatus(Status.INDEXING);
                s.setStatusTime(LocalDateTime.now());
                siteRepository.save(s);

                Executor executor = new Executor(s.getUrl(), s, pageRepository, siteRepository, fakeUser);
                ForkJoinPool pool = new ForkJoinPool(1000);
                pool.invoke(executor);
                pools.add(pool);

                s = siteRepository.findByName(s.getName());
                s.setStatus(Status.INDEXED);
                siteRepository.save(s);
            });
            t.start();

            activeThreads.add(t);
        }
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (siteRepository.findByStatus(Status.INDEXING).isEmpty()) {
            IndexingResponse response = new IndexingResponse();
            response.setResult(false);
            response.setError("Индексация не запущена");
            return response;
        }
        activeThreads.forEach(Thread::interrupt);
        pools.forEach(ForkJoinPool::shutdownNow);
        siteRepository.findByStatus(Status.INDEXING).forEach(site -> {
            site.setStatus(Status.FAILED);
            siteRepository.save(site);
        });
        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return null;
    }
}