package demo.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinnhubQuoteResponse {
    @JsonProperty("c")
    private BigDecimal currentPrice;

    @JsonProperty("v")
    private Long volume;
}