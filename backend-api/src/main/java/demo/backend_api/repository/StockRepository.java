package demo.backend_api.repository;

import demo.backend_api.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {

    // Custom query to grab only active ticker strings for the WebSocket initialization array
    @Query("SELECT s.symbol FROM Stock s WHERE s.isActive = true")
    List<String> findAllActiveSymbols();
}