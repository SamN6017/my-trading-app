package demo.backend_api.repository;

import demo.backend_api.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByStockSymbolAndRecordedDateAfterOrderByRecordedDateAsc(String symbol, LocalDate date);
}
