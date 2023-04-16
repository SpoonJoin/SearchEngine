package searchengine.dto.indexing.responses;

import lombok.AllArgsConstructor;
import searchengine.dto.indexing.IndexingResponse;

@AllArgsConstructor
public class FalseIndexingResponse implements IndexingResponse {
    private String error;

    @Override
    public boolean getResult() {
        return false;
    }

    public String getError() {
        return error;
    }
}
