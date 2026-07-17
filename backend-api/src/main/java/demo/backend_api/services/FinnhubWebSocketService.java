package demo.backend_api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import demo.backend_api.dto.FinnhubResponse;
import demo.backend_api.dto.FinnhubTradeData;
import demo.backend_api.repository.StockRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinnhubWebSocketService {

    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${finnhub.api.key}")
    private String finnhubApiKey;

    @PostConstruct
    public void connectToFinnhub() {
        String url = "wss://ws.finnhub.io?token=" + finnhubApiKey;
        StandardWebSocketClient client = new StandardWebSocketClient();

        log.info("Initiating WebSocket handshake to Finnhub...");

        try {
            client.execute(new TextWebSocketHandler() {

                @Override
                public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                    log.info("====== WebSocket Connection Established Successfully ======");

                    // Pull active symbols from your DB
                    List<String> symbols = stockRepository.findAllActiveSymbols();

                    // Finnhub free tier limit is 50 symbols
                    int limit = Math.min(symbols.size(), 50);
                    log.info("Found {} active stocks in DB. Subscribing to the first {}...", symbols.size(), limit);

                    for (int i = 0; i < limit; i++) {
                        String symbol = symbols.get(i);
                        String subscribePayload = "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}";
                        session.sendMessage(new TextMessage(subscribePayload));
                    }
                    log.info("Sent subscriptions for all {} tickers.", limit);
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    try {
                        String payload = message.getPayload();

                        // Parse JSON into our DTO structures
                        FinnhubResponse response = objectMapper.readValue(payload, FinnhubResponse.class);

                        if ("trade".equals(response.getType()) && response.getTradeList() != null) {
                            for (FinnhubTradeData trade : response.getTradeList()) {
                                // 🟢 CLEAR VISUAL CONSOLE PRINT
//                                System.out.printf("[📈 TRADE TICK] Symbol: %-6s | Price: $%8.2f | Volume: %-5d%n",
//                                    trade.getSymbol(), trade.getPrice(), trade.getVolume());
                            }
                        } else if ("ping".equals(response.getType())) {
                            log.debug("Received heartbeat ping from Finnhub");
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse incoming market payload: {}", e.getMessage());
                    }
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    log.error("WebSocket transport error occurred: ", exception);
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    log.warn("WebSocket connection closed. Status: {}", status);
                }

            }, url);

        } catch (Exception e) {
            log.error("Unable to execute WebSocket connection: ", e);
        }
    }
}