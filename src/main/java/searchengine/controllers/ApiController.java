package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexingService;
import searchengine.services.RequestStatus;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private final StatisticsService statisticsService;
    public final IndexingService indexingService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<?> startIndexing() {
        return new ResponseEntity<>(indexingService.startIndexing(), RequestStatus.getStatus());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<?> stopIndexing() {
        return new ResponseEntity<>(indexingService.stopIndexing(), RequestStatus.getStatus());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<?> indexPage(String url) {
        return new ResponseEntity<>(indexingService.indexPage(url), RequestStatus.getStatus());
    }
}
