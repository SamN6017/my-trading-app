package demo.backend_api.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class TodaysPriceId implements Serializable {
    private String symbol;
    private LocalDateTime timestamp;
}