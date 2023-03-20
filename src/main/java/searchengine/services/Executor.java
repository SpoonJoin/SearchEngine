package searchengine.services;

import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import searchengine.config.FakeUser;
import searchengine.model.Page;
import searchengine.model.PageRepository;
import searchengine.model.Site;
import searchengine.model.SiteRepository;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;
@ConfigurationProperties
public class Executor extends RecursiveAction {
    private static PageRepository pageRepository;
    private static SiteRepository siteRepository;
    private String url;
    private Site site;
    private static FakeUser fakeUser;
    private static CopyOnWriteArraySet<String> allLinks = new CopyOnWriteArraySet<>();

    public Executor(String url, Site site, PageRepository pageRepository, SiteRepository siteRepository) {
        this.url = url;
        this.site = site;
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
            Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36 " +
                            "(compatible; YandexMedianaBot/1.0; +http://yandex.com/bots)")
                    .referrer("https://www.google.com").get();
            Elements elements = doc.select("a");
            for(Element e : elements)
            {
                String attr = e.attr("abs:href");
                if (!attr.isEmpty() &&
                        attr.contains(site.getUrl()) &&
                        !allLinks.contains(attr) &&
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

                    if (pageRepository.findByPath(page.getPath()).isEmpty()) {
                        pageRepository.save(page);
                        allLinks.add(page.getPath());
                    }
                }
            }
        }
        catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        }
        for (Executor ex : subTask) {
            ex.join();
        }
    }

    @SneakyThrows
    private boolean isInTable (String path)
    {
        Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/search_engine" +
                        "?user=root" + "&password=password" + "?useSSL=false&" +
                "requireSSL=false&allowPublicKeyRetrieval=true");
        String query = "SELECT `path` FROM search_engine.page " +
                "WHERE `path` = '" + path + "'";
        ResultSet rs = connection.createStatement().executeQuery(query);
        String result = rs.getString(1);
        return result != null;
    }
}
