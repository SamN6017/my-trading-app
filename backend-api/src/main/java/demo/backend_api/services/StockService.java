package demo.backend_api.services;

import demo.backend_api.dto.StockResponse;
import demo.backend_api.model.Stock;
import demo.backend_api.repository.PriceHistoryRepository;
import demo.backend_api.repository.StockRepository;
import demo.backend_api.repository.TodaysPriceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    public List<StockResponse> getAlltheStocks(){
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

    private StockResponse mapStockToStockResponse(Stock stock, Map<String, Double> previousCloses){
        Double previousClose = previousCloses.get(stock.getSymbol());

        Double currentPrice = todaysPriceRepository.findTopBySymbolOrderByTimestampDesc(stock.getSymbol());

        Double change = currentPrice - previousClose;
        Double changePercent = previousClose != 0 ? (change / previousClose) * 100.0 : 0.0;

        return StockResponse.builder()
            .symbol(stock.getSymbol())
            .symbol(stock.getSymbol())
            .companyName(stock.getCompanyName())
            .sector(stock.getSector())
            .currentPrice(currentPrice)
            .previousClose(previousClose)
            .change(Math.round(change * 100.0) / 100.0)
            .changePercent(Math.round(changePercent * 100.0) / 100.0)
            .build();
    }
}
