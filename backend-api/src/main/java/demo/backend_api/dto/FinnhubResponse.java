package demo.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class FinnhubResponse {
    private String type; // Will equal "trade" or "ping"

    @JsonProperty("data")
    private List<FinnhubTradeData> tradeList;
}