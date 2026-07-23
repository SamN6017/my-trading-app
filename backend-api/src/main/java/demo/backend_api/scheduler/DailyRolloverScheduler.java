package demo.backend_api.scheduler;

import demo.backend_api.dto.FinnhubCandleResponse;
import demo.backend_api.model.PriceHistory;
import demo.backend_api.model.Stock;
import demo.backend_api.repository.PriceHistoryRepository;
import demo.backend_api.repository.StockRepository;
import demo.backend_api.repository.TodaysPriceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyRolloverScheduler {

    private final TodaysPriceRepository todaysPriceRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StockRepository stockRepository;
    private final RestTemplate restTemplate = new RestTemplate(); // Standard HTTP Client

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    // Runs every weekday at midnight (Monday through Friday)
    @Scheduled(cron = "0 0 0 * * MON-FRI")
    @Transactional
    public void performDailyRollover() {

        log.info("⏰ Starting EOD Rollover: Fetching daily candles from Finnhub...");

        List<Stock> activeStocks = stockRepository.findAll();

        // Calculate UNIX timestamps for "yesterday" (from 00:00:00 to 23:59:59)
        LocalDate yesterday = LocalDate.now().minusDays(1);
        long startOfYesterday = yesterday.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long endOfYesterday = yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() - 1;

        for (Stock stock : activeStocks) {
            try {
                // 1. Build Finnhub Stock Candle URL (Resolution 'D' = Daily)
                String url = String.format(
                    "https://finnhub.io/api/v1/stock/candle?symbol=%s&resolution=D&from=%d&to=%d&token=%s",
                    stock.getSymbol(), startOfYesterday, endOfYesterday, finnhubApiKey
                );

                FinnhubCandleResponse response = restTemplate.getForObject(url, FinnhubCandleResponse.class);

                // 2. Map and save directly if Finnhub has the yesterday summary
                if (response != null && "ok".equals(response.getStatus()) && !response.getClosePrices().isEmpty()) {
                    PriceHistory historyRecord = PriceHistory.builder()
                        .stock(stock)
                        .openPrice(response.getOpenPrices().get(0))
                        .highPrice(response.getHighPrices().get(0))
                        .lowPrice(response.getLowPrices().get(0))
                        .closePrice(response.getClosePrices().get(0))
                        .volume(response.getVolumes().get(0))
                        .recordedDate(yesterday)
                        .build();

                    priceHistoryRepository.save(historyRecord);
                    log.info("Archived daily candle for: {}", stock.getSymbol());
                } else {
                    log.warn("No candle data found on Finnhub for {} on date {}", stock.getSymbol(), yesterday);
                }

                // ⚡ Free-tier rate-limit safety: Sleep for 1 second between requests (60 calls/min limit)
                Thread.sleep(1000);

            } catch (Exception e) {
                log.error("Failed to fetch EOD summary from Finnhub for symbol: " + stock.getSymbol(), e);
            }
        }

        // 3. 🧼 WIPE THE SLATE CLEAN for today's dynamic ticks
        todaysPriceRepository.deleteAllInBatch();
        log.info("Successfully truncated 'todays_prices' table. Ready for tomorrow's live ticks!");
    }
}