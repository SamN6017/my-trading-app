package demo.backend_api.dto;

import lombok.Data;
import java.util.List;

@Data
public class PolygonGroupedDailyResponse {
    private String status;
    private Integer queryCount;
    private Integer resultsCount;
    private Boolean adjusted;
    private List<PolygonBarResult> results;
}