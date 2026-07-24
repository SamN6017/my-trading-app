package demo.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolygonBar {

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

    @JsonProperty("vw")
    private Double vwap;

    @JsonProperty("t")
    private Long timestamp; // Unix millisecond epoch

    @JsonProperty("n")
    private Integer numberOfTrades;
}