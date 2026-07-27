package demo.backend_api.controllers;

import demo.backend_api.dto.StockResponse;
import demo.backend_api.model.TodaysPrice;
import demo.backend_api.services.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StocksController {

    private final StockService stockService;

    @GetMapping("/stocks")
    public ResponseEntity<List<StockResponse>> getStocks() {
        return ResponseEntity.ok(stockService.getAlltheStocks());
    }

    // Fix: Added /{symbol} to path mapping
    @GetMapping("/intraday/{symbol}")
    public ResponseEntity<List<TodaysPrice>> getIntradayStocks(@PathVariable("symbol") String symbol) {
        return ResponseEntity.ok(stockService.getTodaysStocks(symbol));
    }
}