package searchengine.dto.indexing.responses;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import searchengine.dto.indexing.IndexingResponse;

@Setter
@Getter
public class TrueIndexingResponse implements IndexingResponse {
    @Override
    public boolean getResult() {
        return true;
    }
}