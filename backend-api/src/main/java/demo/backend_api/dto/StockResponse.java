package demo.backend_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockResponse {
    private String symbol;
    private String companyName;
    private String sector;
    private Double currentPrice;
    private Double previousClose;
    private Double change;
    private Double changePercent;
}