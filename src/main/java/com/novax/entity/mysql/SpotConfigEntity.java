package com.novax.entity.mysql;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class SpotConfigEntity {
    private Long id;

    private Long tenantId;

    private String symbol;

    private String baseCurrency;

    private String quoteCurrency;

    private BigDecimal priceTick;

    private BigDecimal sizeTick;

    private BigDecimal sellMin;

    private BigDecimal sellMax;

    private BigDecimal buyMin;

    private BigDecimal buyMinPrice;

    private BigDecimal tradeMin;

    private BigDecimal tradeMax;

    private BigDecimal rise;

    private BigDecimal fall;

    private BigDecimal marketRatioBuy;

    private BigDecimal marketRatioSell;

    private BigDecimal maxNumber;

    private BigDecimal rate;

    private Integer recommend;

    private Date recommendStartTime;

    private Date recommendEndTime;

    private Integer innovate;

    private String invit;

    private Integer sort;

    private Integer isFee;

    private Integer tradeType;

    private Integer status;

    private Integer isShow;

    private Integer isDeleted;

    private Date openTime;

    private Integer isGridOpened;

    private Integer gridState;

    private Long creator;

    private Date createTime;

    private Long updater;

    private Date updateTime;

    private String tendency;
}