package demo.backend_api.services;

import demo.backend_api.dto.PriceHistoryResponse;
import demo.backend_api.model.PriceHistory;
import demo.backend_api.repository.PriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;

    public PriceHistoryService(PriceHistoryRepository priceHistoryRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getAllHistory(String symbol, String range) {
        LocalDate cutoffDate = calculateCutoffDate(range);
        List<PriceHistory> result = priceHistoryRepository.findByStockSymbolAndRecordedDateAfterOrderByRecordedDateAsc(symbol, cutoffDate);

        return result.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private PriceHistoryResponse mapToResponse(PriceHistory entity) {
        PriceHistoryResponse priceHistoryResponse = new PriceHistoryResponse();

        // ⚡ Use setter methods instead of direct field access
        priceHistoryResponse.setHistoryId(entity.getHistoryId());

        // If PriceHistoryResponse expects a Stock object, use setStock()
        // Ensure @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) is added to Stock.java
        priceHistoryResponse.setStock(entity.getStock());

        priceHistoryResponse.setRecordedDate(entity.getRecordedDate());
        priceHistoryResponse.setOpenPrice(entity.getOpenPrice());
        priceHistoryResponse.setClosePrice(entity.getClosePrice());
        priceHistoryResponse.setHighPrice(entity.getHighPrice());
        priceHistoryResponse.setLowPrice(entity.getLowPrice());
        priceHistoryResponse.setVolume(entity.getVolume());

        return priceHistoryResponse;
    }

    private LocalDate calculateCutoffDate(String range) {
        if (range == null) {
            return LocalDate.now().minusYears(1);
        }
        return switch (range.trim().toUpperCase()) {
            case "1W" -> LocalDate.now().minusWeeks(1);
            case "1M" -> LocalDate.now().minusMonths(1);
            case "3M" -> LocalDate.now().minusMonths(3);
            case "6M" -> LocalDate.now().minusMonths(6);
            case "1Y" -> LocalDate.now().minusYears(1);
            default -> LocalDate.now().minusMonths(1);
        };
    }
}