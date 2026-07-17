package demo.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinnhubResponse {
    private String type; // Will equal "trade" or "ping"

    @JsonProperty("data")
    private List<FinnhubTradeData> tradeList;
}