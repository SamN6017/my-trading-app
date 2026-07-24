package demo.backend_api.scheduler;

import demo.backend_api.dto.PolygonBarResult;
import demo.backend_api.dto.PolygonGroupedDailyResponse;
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

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyRolloverScheduler {

    private final TodaysPriceRepository todaysPriceRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final StockRepository stockRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${polygon.api.key}")
    private String polygonApiKey;

    // Runs every weekday at midnight (00:00 UTC, Monday through Friday)
    @Scheduled(cron = "0 0 0 * * MON-FRI")
    @Transactional
    public void performDailyRollover() {
        // Calculate target trading day (yesterday, or Friday if triggered on Monday)
        LocalDate targetDate = LocalDate.now().minusDays(1);
        if (targetDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            targetDate = targetDate.minusDays(2);
        } else if (targetDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
            targetDate = targetDate.minusDays(1);
        }

        String formattedDate = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE); // e.g. "2026-07-22"
        log.info("⏰ Starting EOD Rollover: Fetching Massive/Polygon Grouped Daily candles for date: {}", formattedDate);

        try {
            // 1. Single API request fetching EOD candles for all US stocks
            String url = String.format(
                "https://api.massive.com/v2/aggs/grouped/locale/us/market/stocks/%s?adjusted=true&apiKey=%s",
                formattedDate, polygonApiKey
            );

            // 2. Fetch payload
            PolygonGroupedDailyResponse response = restTemplate.getForObject(url, PolygonGroupedDailyResponse.class);

            if (response != null && "OK".equalsIgnoreCase(response.getStatus()) && response.getResults() != null) {

                // Map ticker -> PolygonBarResult for instant O(1) memory lookup
                Map<String, PolygonBarResult> candleMap = response.getResults().stream()
                    .filter(bar -> bar.getTicker() != null)
                    .collect(Collectors.toMap(
                        PolygonBarResult::getTicker,
                        Function.identity(),
                        (existing, replacement) -> existing
                    ));

                List<Stock> activeStocks = stockRepository.findAll();
                int archivedCount = 0;

                // 3. Match active stocks in your DB and store daily OHLCV
                for (Stock stock : activeStocks) {
                    PolygonBarResult bar = candleMap.get(stock.getSymbol());

                    if (bar != null) {
                        PriceHistory historyRecord = PriceHistory.builder()
                            .stock(stock)
                            .openPrice(BigDecimal.valueOf(bar.getOpen()))
                            .highPrice(BigDecimal.valueOf(bar.getHigh()))
                            .lowPrice(BigDecimal.valueOf(bar.getLow()))
                            .closePrice(BigDecimal.valueOf(bar.getClose()))
                            .volume(bar.getVolume())
                            .recordedDate(targetDate)
                            .build();

                        priceHistoryRepository.save(historyRecord);
                        archivedCount++;
                    } else {
                        log.warn("No EOD candle found on Massive for symbol: {} on date {}", stock.getSymbol(), targetDate);
                    }
                }

                log.info("Successfully archived {} stock daily candles into price_history for date: {}", archivedCount, targetDate);

            } else {
                log.error("Failed to fetch market candles from Massive API. Status: {}", response != null ? response.getStatus() : "NULL");
            }

        } catch (Exception e) {
            log.error("Error executing EOD daily rollover from Massive API", e);
        }

        // 4. Wipe 'todays_price' table clean for tomorrow's live ticks
        todaysPriceRepository.deleteAllInBatch();
        log.info("Successfully truncated 'todays_price' table. Ready for tomorrow's live ticks!");
    }
}