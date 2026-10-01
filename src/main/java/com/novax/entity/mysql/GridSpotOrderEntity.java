package com.novax.entity.mysql;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class GridSpotOrderEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private String symbol;

    private String currency;

    private BigDecimal investment;

    private Byte status;

    private Date createTime;

    private Date updateTime;

}