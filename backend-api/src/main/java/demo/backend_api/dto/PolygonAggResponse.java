package demo.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolygonAggResponse {

    private String ticker;
    private String status;
    private Boolean adjusted;
    private Integer queryCount;
    private Integer resultsCount;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("next_url")
    private String nextUrl;

    private List<PolygonBar> results;
}