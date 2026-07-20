package demo.backend_api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.backend_api.model.TodaysPrice;
import demo.backend_api.repository.TodaysPriceRepository;
import demo.backend_api.dto.FinnhubTradeData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataBufferService {

    private final TodaysPriceRepository todaysPriceRepository;
    private final StringRedisTemplate  stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REDIS_QUEUE_KEY = "market:ticks:queue";

    public void bufferTick(FinnhubTradeData trade){
        try{
            String json = objectMapper.writeValueAsString(trade);
            stringRedisTemplate.opsForList().rightPush(REDIS_QUEUE_KEY, json);
        }catch (Exception e){
            log.error("Failed to buffer tick in AWS Redis", e);
        }
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void writeToPostgres(){
        Long queuSize = stringRedisTemplate.opsForList().size(REDIS_QUEUE_KEY);
        if(queuSize == null || queuSize == 0){
            return;
        }

        log.info("Draining {} from Redis to postgres", queuSize);

        List<TodaysPrice> batch = new ArrayList<>();

        for(int i = 0; i < queuSize; i++){
            String json = stringRedisTemplate.opsForList().leftPop(REDIS_QUEUE_KEY);
            if(json == null){ break;}

            try{
                FinnhubTradeData trade = objectMapper.readValue(json, FinnhubTradeData.class);

                TodaysPrice todaysPrice = TodaysPrice.builder()
                    .symbol(trade.getSymbol())
                    .price(BigDecimal.valueOf(trade.getPrice()))
                    .timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(trade.getTimestamp()),
                        ZoneId.systemDefault()))
                    .volume(trade.getVolume())
                    .build();
                batch.add(todaysPrice);
            }catch (Exception e){
                log.error("Error decoding while pushing data in db from redis ", e);
            }


        }
        if(!batch.isEmpty()){
            todaysPriceRepository.saveAll(batch);
            log.info("Added {} todays price in db from redis ", batch.size());
        }
    }

}
