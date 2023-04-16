package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.config.FakeUser;
import searchengine.lemmatizator.Lemmatizator;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveAction;

public class Executor extends RecursiveAction {
    private static PageRepository pageRepository;
    private static SiteRepository siteRepository;
    private static LemmaRepository lemmaRepository;
    private static IndexRepository indexRepository;
    private String url;
    private Site site;
    private FakeUser fakeUser;
    public static boolean isActive = true;
    private static CopyOnWriteArraySet<String> allLinks = new CopyOnWriteArraySet<>();

    public Executor(String url, Site site, FakeUser fakeUser,
                    PageRepository pageRepository,
                    SiteRepository siteRepository,
                    LemmaRepository lemmaRepository,
                    IndexRepository indexRepository) {
        this.url = url;
        this.site = site;
        this.fakeUser = fakeUser;
        Executor.pageRepository = pageRepository;
        Executor.siteRepository = siteRepository;
        Executor.lemmaRepository = lemmaRepository;
        Executor.indexRepository = indexRepository;
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
                    Page page = new Page();
                    page.setPath(attr);
                    page.setSite(site);
                    page.setSiteId(site.getId());
                    page.setContent(doc.html());
                    page.setCode(response.statusCode());
                    Executor ex = new Executor(page.getPath(), site, fakeUser,
                            pageRepository, siteRepository, lemmaRepository, indexRepository);

                    Lemmatizator lemmatizator = new Lemmatizator();
                    if (isActive) {
                        ex.fork();
                        subTask.add(ex);
                        addPageToDB(page, logger);
                        addLemmasToDb(lemmatizator.createLemmas(page.getContent()), page);
                    }
                }
            }
        }
        catch (IOException | InterruptedException exception) {
        }
        for (Executor ex : subTask) {
            ex.join();
        }
    }

    private void addPageToDB (Page page, Logger logger)
    {
        synchronized (allLinks) {
            if (!allLinks.contains(page.getPath())) {
                pageRepository.save(page);
                logger.info("SITE: " + page.getSite().getName() + " page: " + page.getPath());
                allLinks.add(page.getPath());
            }
        }
        synchronized (siteRepository) {
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

    private void addLemmasToDb (HashMap<String, Integer> lemmas, Page page) {
        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemma = entry.getKey();
            int count = entry.getValue();
            Lemma lem;
            if (lemmaRepository.findByLemma(lemma) == null) {
                lem = new Lemma();
                lem.setLemma(lemma);
                lem.setFrequency(1);
                lem.setSiteId(site.getId());
            } else {
                lem = lemmaRepository.findByLemma(lemma);
                lem.setFrequency(lem.getFrequency() + 1);
            }
            synchronized (lemmaRepository) {
                lemmaRepository.save(lem);
            }
            addToIndex(lem, count, page);
        }
    }

    private void addToIndex (Lemma lemma, int count, Page page) {
        synchronized (indexRepository) {
            Index index = new Index();
            index.setLemma(lemma);
            index.setPage(page);
            index.setRank(count);
            indexRepository.save(index);
        }
    }
}