package demo.backend_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_history", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"symbol", "recorded_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    // Links this record to the stocks table using the "symbol" foreign key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol", nullable = false)
    private Stock stock;

    @Column(name = "open_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal closePrice;

    @Column(nullable = false)
    private Long volume;

    @Column(name = "recorded_date", nullable = false)
    private LocalDate recordedDate;
}