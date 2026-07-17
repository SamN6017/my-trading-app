package demo.backend_api.services;

import demo.backend_api.dto.PriceHistoryResponse;
import demo.backend_api.model.PriceHistory;
import demo.backend_api.repository.PriceHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PriceHistoryService {
    private final PriceHistoryRepository priceHistoryRepository;
    public PriceHistoryService(PriceHistoryRepository priceHistoryRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public List<PriceHistoryResponse> getAllHistory(String symbol, String range){
        LocalDate cutoffDate = calculateCutoffDate(range);
        List<PriceHistory> result = priceHistoryRepository.findByStockSymbolAndRecordedDateAfterOrderByRecordedDateAsc(symbol,cutoffDate);
        return result.stream().map(entity -> {
            PriceHistoryResponse priceHistoryResponse = new PriceHistoryResponse();
            priceHistoryResponse.historyId = entity.getHistoryId();
            priceHistoryResponse.recordedDate = entity.getRecordedDate();
            priceHistoryResponse.closePrice = entity.getClosePrice();
            priceHistoryResponse.highPrice = entity.getHighPrice();
            priceHistoryResponse.lowPrice = entity.getLowPrice();
            priceHistoryResponse.volume = entity.getVolume();
            return priceHistoryResponse;
        }).collect(Collectors.toList());
    }

    private LocalDate calculateCutoffDate(String range){
        if(range == null){
            return LocalDate.now().minusYears(1);
        }
        return switch (range.toUpperCase()) {
            case "1W" -> LocalDate.now().minusWeeks(1);
            case "3M" -> LocalDate.now().minusMonths(3);
            case "6M" -> LocalDate.now().minusMonths(6);
            case "1Y" -> LocalDate.now().minusYears(1);
            default -> LocalDate.now().minusMonths(1);
        };
    }
}
