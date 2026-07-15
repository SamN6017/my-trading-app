package demo.backend_api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stock {

    @Id
    @Column(length = 10)
    private String symbol; // Primary key is the ticker itself (e.g., 'AAPL')

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 50)
    private String sector;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}