package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import searchengine.model.Status;

import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sitesList;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

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
                synchronized (pageRepository) {
                    Executor executor = new Executor(s.getUrl(), s, pageRepository, siteRepository);
                    ForkJoinPool pool = new ForkJoinPool(1000);
                    pool.invoke(executor);
                }
            });
            t.start();
        }

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }
}
