package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.Executor;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.model.PageRepository;
import searchengine.model.SiteRepository;
import searchengine.model.Status;

import java.time.LocalDateTime;
import java.util.Iterator;
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
        this.siteRepository.deleteAll();
        this.pageRepository.deleteAll();
        Iterator var1 = this.sitesList.getSites().iterator();

        while (var1.hasNext()) {
            Site site = (Site) var1.next();
            (new Thread(() -> {
                searchengine.model.Site s = new searchengine.model.Site();
                s.setUrl(site.getUrl());
                s.setName(site.getName());
                s.setStatus(Status.INDEXING);
                s.setStatusTime(LocalDateTime.now());
                this.siteRepository.save(s);
                Executor executor = new Executor(s.getUrl(), s, this.pageRepository, this.siteRepository);
                ForkJoinPool pool = new ForkJoinPool(1000);
                pool.invoke(executor);
                s.setStatus(Status.INDEXED);
            })).start();
        }

        IndexingResponse response = new IndexingResponse();
        response.setResult(true);
        return response;
    }
}
