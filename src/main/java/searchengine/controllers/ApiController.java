package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.RequestStatus;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.io.IOError;
import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchService searchService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return new ResponseEntity<>(indexingService.startIndexing(), RequestStatus.getStatus());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return new ResponseEntity<>(indexingService.stopIndexing(), RequestStatus.getStatus());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(String url) {
        return new ResponseEntity<>(indexingService.indexPage(url), RequestStatus.getStatus());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String site,
            @RequestParam(required=false, defaultValue="0") int offset,
            @RequestParam(required=false, defaultValue="20") int limit)
            throws IOException
    {
        System.out.println("\nЗапрос: " + query +
                "\nСайт: " + site +
                "\nСдвиг: " + offset +
                "\nКол-во результатов: " + limit);

        if (site == null) {
            return new ResponseEntity<>(searchService.searchAll(query, site, offset, limit),
                    RequestStatus.getStatus());
        }
        return new ResponseEntity<>(searchService.search(query, site, offset, limit),
                RequestStatus.getStatus());
    }
}