package demo.backend_api.controllers;


import demo.backend_api.dto.StockResponse;
import demo.backend_api.services.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
