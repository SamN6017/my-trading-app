package demo.backend_api.services;

import demo.backend_api.dto.StockResponse;
import demo.backend_api.model.Stock;
import demo.backend_api.model.TodaysPrice;
import demo.backend_api.repository.PriceHistoryRepository;
import demo.backend_api.repository.StockRepository;
import demo.backend_api.repository.TodaysPriceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockRepository stockRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TodaysPriceRepository todaysPriceRepository;

    @Transactional()
    public List<StockResponse> getAlltheStocks() {
        List<Stock> stocks = stockRepository.findAll();

        Map<String, Double> previousCloses = priceHistoryRepository.findLatestClosingPricesForAllStocks()
            .stream()
            .collect(Collectors.toMap(
                ph -> ph.getStock().getSymbol(),
                ph -> ph.getClosePrice() != null ? ph.getClosePrice().doubleValue() : 0.0,
                (existing, replacement) -> existing
            ));
        return stocks.stream().map(stock -> mapStockToStockResponse(stock, previousCloses)).collect(Collectors.toList());
    }

    private StockResponse mapStockToStockResponse(Stock stock, Map<String, Double> previousCloses) {
        // 1. Safely handle missing historical close price (fallback to 0.0 if null)
        Double previousClose = previousCloses.get(stock.getSymbol());
        if (previousClose == null) {
            previousClose = 0.0;
        }

        // 2. Fetch current price from todays_prices, defaulting to previousClose if empty/null
        Double currentPrice = todaysPriceRepository.findTopBySymbolOrderByTimestampDesc(stock.getSymbol())
            .map(tp -> tp.getPrice() != null ? tp.getPrice().doubleValue() : null)
            .orElse(previousClose);

        // 3. Final safety check in case currentPrice is still null
        if (currentPrice == null) {
            currentPrice = 0.0;
        }

        // 4. Calculate price change & percentage safely
        double change = currentPrice - previousClose;
        double changePercent = previousClose != 0.0 ? (change / previousClose) * 100.0 : 0.0;

        return StockResponse.builder()
            .symbol(stock.getSymbol())
            .companyName(stock.getCompanyName())
            .sector(stock.getSector())
            .currentPrice(Math.round(currentPrice * 100.0) / 100.0)
            .previousClose(Math.round(previousClose * 100.0) / 100.0)
            .change(Math.round(change * 100.0) / 100.0)
            .changePercent(Math.round(changePercent * 100.0) / 100.0)
            .build();
    }
}