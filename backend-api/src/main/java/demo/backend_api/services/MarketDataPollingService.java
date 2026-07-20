package demo.backend_api.services;

import demo.backend_api.dto.FinnhubQuoteResponse;
import demo.backend_api.dto.FinnhubTradeData;
import demo.backend_api.model.Stock;
import demo.backend_api.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataPollingService {
    private final StockRepository stockRepository;
    private final MarketDataBufferService marketDataBufferService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    private boolean toggleBatch = false;

    @Scheduled(cron = "0 * * * * MON-FRI")
    public void pollMarketData() {
        List<Stock> allStocks = stockRepository.findAll();
        if(allStocks.isEmpty()) {return;}

        int midpoint = Math.min(50, allStocks.size());

        List<Stock> targetBatch = (toggleBatch) ? allStocks.subList(0, midpoint)
                : allStocks.subList(midpoint, allStocks.size());
        log.info("⏱️ Polling Batch {} containing {} symbols...", (toggleBatch ? "A" : "B"), targetBatch.size());

        for(Stock stock : targetBatch) {
            try{
                String url = String.format("https://finnhub.io/api/v1/quote?symbol=%s&token=%s",
                    stock.getSymbol(), finnhubApiKey);
                FinnhubQuoteResponse response = restTemplate.getForObject(url, FinnhubQuoteResponse.class);
                if(response != null && response.getCurrentPrice() != null && response.getCurrentPrice().compareTo(java.math.BigDecimal.ZERO) > 0)
                {
                    FinnhubTradeData finnhubTradeData = new FinnhubTradeData();
                    finnhubTradeData.setSymbol(stock.getSymbol());
                    finnhubTradeData.setPrice(response.getCurrentPrice().doubleValue());
                    finnhubTradeData.setVolume(response.getVolume() != null ? response.getVolume() : 0L);
                    finnhubTradeData.setTimestamp(System.currentTimeMillis());

                    marketDataBufferService.bufferTick(finnhubTradeData);
                }

                Thread.sleep(20);
            } catch (Exception e) {
                log.error("Failed fetching price snapshot for ", stock.getSymbol(), e);
            }
        }
        toggleBatch = !toggleBatch;
        log.info("Batch poll complete. Next run will process the alternate partition.");
    }
}
