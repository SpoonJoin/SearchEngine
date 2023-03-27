package searchengine.dto.indexing;

import lombok.Data;

import java.util.Optional;

@Data
public class IndexingResponse {
    private boolean result;
    private String error;
}