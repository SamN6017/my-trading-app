package demo.backend_api.repository;

import demo.backend_api.model.TodaysPrice;
import demo.backend_api.model.TodaysPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TodaysPriceRepository extends JpaRepository<TodaysPrice, TodaysPriceId> {
    @Query("SELECT t FROM TodaysPrice t WHERE t.symbol = :symbol ORDER BY t.timestamp ASC")
    List<TodaysPrice> findBySymbolOrderByTimestampAsc(@Param("symbol") String symbol);
}
