package com.novax.entity.mysql;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class VipLevelConfigEntity {
    private Long id;

    private Integer level;

    private BigDecimal spotBtc;

    private BigDecimal swapBtc;

    private BigDecimal platform;

    private BigDecimal spotMaker;

    private BigDecimal spotTaker;

    private BigDecimal spotMakerDiscount;

    private BigDecimal spotTakerDiscount;

    private BigDecimal swapFrontMaker;

    private BigDecimal swapFrontTaker;

    private BigDecimal swapFrontMakerDiscount;

    private BigDecimal swapFrontTakerDiscount;

    private BigDecimal swapReverseMaker;

    private BigDecimal swapReverseTaker;

    private BigDecimal swapReverseMakerDiscount;

    private BigDecimal swapReverseTakerDiscount;

    private BigDecimal withdrawalMax;

    private BigDecimal withdrawalSingleLimit;

    private String withdrawalCurrency;

    private Date createTime;

    private Date updateTime;

    private Integer status;

}