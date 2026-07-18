package demo.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinnhubCandleResponse {

    @JsonProperty("s")
    private String status; // "ok" or "no_data"

    @JsonProperty("o")
    private List<BigDecimal> openPrices;

    @JsonProperty("h")
    private List<BigDecimal> highPrices;

    @JsonProperty("l")
    private List<BigDecimal> lowPrices;

    @JsonProperty("c")
    private List<BigDecimal> closePrices;

    @JsonProperty("v")
    private List<Long> volumes;

    @JsonProperty("t")
    private List<Long> timestamps;
}