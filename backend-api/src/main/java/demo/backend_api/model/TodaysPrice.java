package demo.backend_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "todays_price")
@IdClass(TodaysPriceId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodaysPrice {
    @Id
    @Column(nullable = false, length = 10)
    private String symbol;

    @Column(nullable = false, length = 10)
    private BigDecimal price;

    @Column(nullable = false)
    private Long volume;

    @Id
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
