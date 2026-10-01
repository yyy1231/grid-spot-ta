package com.novax.entity.condition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GridSpotOrderSelectEntity {
    private Long userId;
    private String symbol;
    private boolean isRunning;
    private Date startTime;
    private Date endTime;
    private int page;
    private int pageSize;

}
