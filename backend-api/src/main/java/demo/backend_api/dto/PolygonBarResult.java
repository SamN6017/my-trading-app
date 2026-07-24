package demo.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PolygonBarResult {

    @JsonProperty("T")
    private String ticker;

    @JsonProperty("o")
    private Double open;

    @JsonProperty("h")
    private Double high;

    @JsonProperty("l")
    private Double low;

    @JsonProperty("c")
    private Double close;

    @JsonProperty("v")
    private Long volume;
}