package com.novax.entity.mysql;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class GridSpotFinanceRecordEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private Long gridSpotOrderId;

    private Long transferOrderId;

    private Long spotOrderId;

    private String symbol;

    private String currency;

    private BigDecimal price;

    private Integer operation;

    private BigDecimal amount;

    private Integer status;

    private Date createTime;

    private Long inviteeId;
}