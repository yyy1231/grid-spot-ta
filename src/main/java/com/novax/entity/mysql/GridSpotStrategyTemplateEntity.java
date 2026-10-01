package com.novax.entity.mysql;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class GridSpotStrategyTemplateEntity {
    private Long id;

    private String symbol;

    private Short backtestingPeriod;

    private String name;

    private String description;

    private Byte enabled;

    private Byte gridType;

    private BigDecimal upperLimitPrice;

    private BigDecimal lowerLimitPrice;

    private Short gridQuantity;

    private BigDecimal minProfitRatePerGrid;

    private BigDecimal maxProfitRatePerGrid;

    private BigDecimal backtestingProfitRate;
    private BigDecimal actualBacktestingProfitRate;

    private BigDecimal backtestingApr;

    private Date createTime;

    private Date updateTime;
}