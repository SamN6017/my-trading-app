package demo.backend_api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import demo.backend_api.model.Stock;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PriceHistoryResponse {
    public long historyId;
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Stock stock;
    public BigDecimal openPrice;
    public BigDecimal closePrice;
    public BigDecimal highPrice;
    public BigDecimal lowPrice;
    public Long volume;
    public LocalDate recordedDate;
}
