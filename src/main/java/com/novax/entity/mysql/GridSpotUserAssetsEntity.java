package com.novax.entity.mysql;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class GridSpotUserAssetsEntity {
    private Long id;

    private Long tenantId;

    private Long userId;

    private Long gridSpotOrderId;

    private String currency;

    private BigDecimal available;

    private BigDecimal frozen;

    private Date createTime;

    private Date updateTime;

}