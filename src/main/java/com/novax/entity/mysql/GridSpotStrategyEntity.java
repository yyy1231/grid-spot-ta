package com.novax.entity.mysql;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class GridSpotStrategyEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private Long gridSpotOrderId;

    private Byte source;

    private Long strategyTemplateId;

    private Short gridQuantity;

    private BigDecimal upperLimitPrice;

    private BigDecimal lowerLimitPrice;

    private Byte gridType;

    private Byte trailingUpEnabled;

    private BigDecimal trailingUpLimit;

    private BigDecimal runningTriggerPrice;

    private BigDecimal stopLossPrice;

    private BigDecimal takeProfitPrice;

    private Byte shouldSellWhileTerminating;

    private Date createTime;

    private Date updateTime;

}