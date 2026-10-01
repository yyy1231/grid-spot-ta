package com.novax.entity.mysql;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class GridSpotOrderTransactionRecordEntity {
    private Long id;

    private Long tenantId;

    private Date createTime;

    private Long transactionBatchId;

    private Long userId;

    private Long gridSpotOrderId;

    private Long spotOrderId;

    private String symbol;

    private Byte tradeType;

    private BigDecimal tradePrice;

    private BigDecimal tradeNumber;

    private String quoteCurrency;

    private BigDecimal quoteAmount;

    private String feeCurrency;

    private BigDecimal fee;
}