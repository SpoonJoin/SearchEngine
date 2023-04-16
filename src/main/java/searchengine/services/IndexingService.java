package searchengine.services;

import org.springframework.http.HttpStatus;
import searchengine.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse startIndexing();
    IndexingResponse stopIndexing();
    IndexingResponse indexPage(String url);
}
