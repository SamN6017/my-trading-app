package demo.backend_api.controllers;

import demo.backend_api.dto.PriceHistoryResponse;
import demo.backend_api.services.PriceHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/prices")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @GetMapping("history/{symbol}")
    public ResponseEntity<List<PriceHistoryResponse> > getHistory(@PathVariable("symbol") String symbol,
                                                          @RequestParam(required = false, defaultValue = "1Y") String range){
        List<PriceHistoryResponse> result =  priceHistoryService.getAllHistory(symbol, range);
        return ResponseEntity.ok(result);
    }
}
