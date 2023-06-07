package searchengine.dto.search.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import searchengine.dto.search.SearchResponse;

@Data
@AllArgsConstructor
public class FalseSearchResponse implements SearchResponse {
    private String error;
    @Override
    public boolean getResult() {
        return false;
    }
    public String getError() {
        return error;
    }
}
