package searchengine.dto.search.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import searchengine.dto.search.SearchResponse;

@Data
public class TrueSearchResponse implements SearchResponse {
    @Override
    public boolean getResult() {
        return true;
    }
    private int count;
    private searchengine.dto.search.responses.detailedData.Data data;
}
