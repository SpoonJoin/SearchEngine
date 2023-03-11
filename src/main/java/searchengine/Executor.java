package searchengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;

public class Executor extends RecursiveTask<Set<Page>> {
    private static PageRepository pageRepository;
    private static SiteRepository siteRepository;
    private String url;
    private static Site site;
    private CopyOnWriteArraySet<String> allLinks = new CopyOnWriteArraySet();

    public Executor(String url, Site site, PageRepository pageRepository, SiteRepository siteRepository) {
        this.url = url;
        Executor.site = site;
        Executor.pageRepository = pageRepository;
        Executor.siteRepository = siteRepository;
    }

    protected Set<Page> compute() {
        Set<Page> pages = new HashSet();
        Set<Executor> subTask = new HashSet();
        Logger logger = LogManager.getLogger();

        try {
            Thread.sleep(150);
            Document doc = Jsoup.connect(this.url).get();
            Elements elements = doc.select("a");
            for(Element e : elements) {
                String attr = e.attr("abs:href");
                if (!attr.isEmpty() &&
                        attr.startsWith(site.getUrl()) &&
                        !this.allLinks.contains(attr) &&
                        !attr.contains("#"))
                {
                    Page page = new Page();
                    page.setPath(attr);
                    page.setSite(site);
                    page.setSiteId(site.getId());
                    page.setContent(doc.html());
                    logger.info("SITE: " + site.getName() + " page: " + page.getPath());
                    Executor ex = new Executor(page.getPath(), site, pageRepository, siteRepository);
                    ex.fork();
                    subTask.add(ex);
                    allLinks.add(attr);
                    synchronized(pageRepository) {
                        pageRepository.save(page);
                    }
                }
            }
        } catch (IOException | InterruptedException var14) {
        }


        for(Executor p : subTask) {
            pages.addAll(p.join());
        }

        return pages;
    }
}
