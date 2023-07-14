package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.search.responses.FalseSearchResponse;
import searchengine.dto.search.responses.TrueSearchResponse;
import searchengine.dto.search.responses.detailedData.Data;
import searchengine.lemmatizator.Lemmatizator;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    private static int count = 0;
    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        if (query.isEmpty() || query.isBlank()) {
            RequestStatus.setStatus(HttpStatus.BAD_REQUEST.value());
            return new FalseSearchResponse("Задан пустой поисковый запрос");
        }
        if (siteRepository.findByUrl(site) == null) {
            RequestStatus.setStatus(HttpStatus.BAD_REQUEST.value());
            return new FalseSearchResponse("Указанный сайт не проиндексирован");
        }
        List<Lemma> l = new ArrayList<>();
        l = lemmaRepository.findAll();
        TrueSearchResponse response = new TrueSearchResponse();
        Data data = new Data();

        Lemmatizator lemmatizator = new Lemmatizator();
        List<Index> indexList = indexRepository.findAll();
        List<Site> siteList = siteRepository.findByStatus(Status.INDEXED);
        Set<String> lemmas = new HashSet<>();
        String[] str = query.split("\\s+");
        for (String s : str) {
            lemmas.addAll(lemmatizator.getLemmasOfWord(s));
        }
        LinkedHashMap<String, Integer> sortedMap = getSortedMap(lemmas);

        if (sortedMap.isEmpty()) System.out.println("ПУСТАЯ МАПА((");
        else System.out.println("ЗАПОЛНЕННАЯ МАПА, УРА!!");

        for (Map.Entry<String, Integer> entry : sortedMap.entrySet()) {
            String lemma = entry.getKey();
            int frequency = entry.getValue();
            for (Index index : indexList) {
                if (index.getLemma().getLemma().equals(lemma)) {
                    data.setSite(site);
                    data.setSiteName(siteRepository.findByUrl(site).getName());
                    data.setUri(index.getPage().getPath());
                    data.setTitle(getTitle(site));
                    data.setSnippet(getHTMLSnippet(index.getPage().getPath(), lemma));
                    data.setRelevance(1);
                }
            }
        }
        response.setCount(count);
        response.setData(data);
        return response;
    }

    @Override
    public SearchResponse searchAll(String query, String site, int offset, int limit) {
        if (query.isEmpty() || query.isBlank()) {
            RequestStatus.setStatus(HttpStatus.BAD_REQUEST.value());
            return new FalseSearchResponse("Задан пустой поисковый запрос");
        }
        return null;
    }

    private LinkedHashMap<String, Integer> getSortedMap(Set<String> queryLemmas) {
        Map<String, Integer> map = new HashMap<>();
        for (String queryLemma : queryLemmas) {
            for (Lemma repLemma : lemmaRepository.findAll()) {
                if (repLemma.getLemma().equals(queryLemma)) {
                    map.put(repLemma.getLemma(), repLemma.getFrequency());
                }
            }
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (o1, o2) -> o1, LinkedHashMap::new));
    }

    private String getTitle(String site) {
        String title;
        try {
            Document doc = Jsoup.connect(site).get();
            title = Objects.requireNonNull(doc.select("head > title").first()).text();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return title;
    }

    private String getHTMLSnippet(String page, String lemma) {
        Lemmatizator lemmatizator = new Lemmatizator();
        StringBuilder builder = new StringBuilder();
        try {
            Document document = Jsoup.connect(page).get();
            Elements e = document.select("body");
            String s = e.text();
            Set<String> words = lemmatizator.createLemmas(s).keySet();
            if (!words.contains(lemma)) {
                return null;
            }
            int startIndex = s.indexOf(lemma);
            int endIndex = s.indexOf(lemma) + lemma.length();
            if (startIndex > 40) {
                builder.append("...");
                s = s.substring(startIndex - 40);
                while (!s.startsWith(" ")) {
                    s = s.substring(1);
                }
            }
            builder.append(s, 0, startIndex).append("<b>").append(lemma).append("</b>");
            if (endIndex > 40) {
                s = s.substring(0, s.length() - 40);
                while (!s.endsWith(" ")) {
                    s = s.substring(0, s.length() - 1);
                }
                builder.append(s.substring(endIndex)).append("...");
            } else {
                builder.append(s.substring(endIndex));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return builder.toString();
    }
}