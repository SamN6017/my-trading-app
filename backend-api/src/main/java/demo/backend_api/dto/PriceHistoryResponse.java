package demo.backend_api.dto;

import demo.backend_api.model.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceHistoryResponse {
    public long historyId;
    public Stock stock;
    public BigDecimal openPrice;
    public BigDecimal closePrice;
    public BigDecimal highPrice;
    public BigDecimal lowPrice;
    public Long volume;
    public LocalDate recordedDate;
}
