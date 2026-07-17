package demo.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinnhubTradeData {
    @JsonProperty("s")
    private String symbol;

    @JsonProperty("p")
    private Double price;

    @JsonProperty("t")
    private Long timestamp;

    @JsonProperty("v")
    private Long volume;
}