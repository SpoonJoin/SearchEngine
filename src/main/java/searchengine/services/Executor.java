package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.FakeUser;
import searchengine.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;

public class Executor extends RecursiveAction {
    private static PageRepository pageRepository;
    private static SiteRepository siteRepository;
    private String url;
    private Site site;
    private FakeUser fakeUser;
    private static CopyOnWriteArraySet<String> allLinks = new CopyOnWriteArraySet<>();
    @Getter
    private static List<Thread> activeThreads = new ArrayList<>();

    public Executor(String url, Site site,
                    PageRepository pageRepository,
                    SiteRepository siteRepository, FakeUser fakeUser) {
        this.url = url;
        this.site = site;
        this.fakeUser = fakeUser;
        Executor.pageRepository = pageRepository;
        Executor.siteRepository = siteRepository;
    }

    protected void compute()
    {
        Set<Executor> subTask = new HashSet<>();
        Logger logger = LogManager.getLogger();
        try
        {
            Thread.sleep(150);
            Connection connection = Jsoup.connect(url)
                    .userAgent(fakeUser.getUseragent())
                    .referrer(fakeUser.getReferrer());
            Document doc = connection.get();
            Connection.Response response = connection.response();
            Elements elements = doc.select("a");
            for(Element e : elements)
            {
                String attr = e.attr("abs:href");
                if (!attr.isEmpty() && attr.contains(site.getUrl()) &&
                        !allLinks.contains(attr) && !attr.contains("#"))
                {
                    activeThreads.add(Thread.currentThread());
                    Page page = new Page();
                    page.setPath(attr);
                    page.setSite(site);
                    page.setSiteId(site.getId());
                    page.setContent(doc.html());
                    page.setCode(response.statusCode());
                    Executor ex = new Executor(page.getPath(), site, pageRepository, siteRepository, fakeUser);
                    ex.fork();
                    subTask.add(ex);
                    addToDB(page, logger);

                }
            }
        }
        catch (IOException | InterruptedException exception) {
        }
        for (Executor ex : subTask) {
            ex.join();
        }
    }

    private void addToDB (Page page, Logger logger)
    {
        synchronized (allLinks)
        {
            if (!allLinks.contains(page.getPath()))
            {
                pageRepository.save(page);
                logger.info("SITE: " + page.getSite().getName() + " page: " + page.getPath());
                allLinks.add(page.getPath());
            }
        }
        synchronized (siteRepository)
        {
            Site s = siteRepository.findByName(site.getName());
            s.setStatusTime(LocalDateTime.now());
            if (page.getCode() >= 400) {
                s.setStatus(Status.FAILED);
                s.setLastError("Ошибка " + page.getCode() + " : \nID: " +
                        page.getId() + "\nPATH: " + page.getPath());
            }
            siteRepository.save(s);
        }
    }
}