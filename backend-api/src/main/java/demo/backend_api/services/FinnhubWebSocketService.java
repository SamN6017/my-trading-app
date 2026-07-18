package demo.backend_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.backend_api.dto.FinnhubResponse;
import demo.backend_api.dto.FinnhubTradeData;
import demo.backend_api.model.Stock;
import demo.backend_api.repository.StockRepository;
import demo.backend_api.services.MarketDataBufferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinnhubWebSocketService extends TextWebSocketHandler {

    private final MarketDataBufferService marketDataBufferService;
    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    private WebSocketSession currentSession;

    @EventListener(ApplicationReadyEvent.class)
    public void connectToFinnhub() {
        try {
            log.info("Initializing Finnhub Real-Time Ticker WebSocket client...");
            WebSocketClient client = new StandardWebSocketClient();
            String webSocketUrl = "wss://ws.finnhub.io?token=" + finnhubApiKey;

            client.execute(this, webSocketUrl).get();
        } catch (Exception e) {
            log.error("Fatal failure attempting initialization of Finnhub stream handler", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.currentSession = session;
        log.info("WebSocket tunnel to Finnhub established safely.");

        // Pull active tracked master stock catalog from your PostgreSQL RDS instance
        List<Stock> activeStocks = stockRepository.findAll().stream()
            .filter(Stock::getIsActive)
            .toList();

        log.info("Registering subscriptions for {} active tracker items...", activeStocks.size());
        for (Stock stock : activeStocks) {
            subscribeToSymbol(stock.getSymbol());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String payload = message.getPayload();
            FinnhubResponse response = objectMapper.readValue(payload, FinnhubResponse.class);

            // Verify payload type matches trade actions and list is not empty
            if ("trade".equals(response.getType()) && response.getTradeList() != null) {
                for (FinnhubTradeData trade : response.getTradeList()) {

                    // ⚡ Offload instantly to your AWS ElastiCache buffer queue
                    marketDataBufferService.bufferTick(trade);

                }
            }
        } catch (Exception e) {
            log.error("Failed to parse incoming market payload: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport level failure experienced on stream channel", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("Finnhub stream socket connection closed. Status code details: {}", status);
        this.currentSession = null;

        // Basic reconnection trigger fallback mechanism
        log.info("Triggering stream reconnect sequence...");
        connectToFinnhub();
    }

    /**
     * Sends a raw subscription text frame down the open WebSocket pipeline.
     */
    private void subscribeToSymbol(String symbol) {
        if (currentSession != null && currentSession.isOpen()) {
            try {
                String subPayload = String.format("{\"type\":\"subscribe\",\"symbol\":\"%s\"}", symbol);
                currentSession.sendMessage(new TextMessage(subPayload));
                log.info("Successfully subscribed to real-time feed for symbol: {}", symbol);
            } catch (IOException e) {
                log.error("Failed downstream frame dispatch for channel subscription: " + symbol, e);
            }
        } else {
            log.warn("Subscription request ignored for {}. Session closed or invalid.", symbol);
        }
    }
}