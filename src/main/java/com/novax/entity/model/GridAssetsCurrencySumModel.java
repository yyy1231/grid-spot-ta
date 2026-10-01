package com.novax.entity.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GridAssetsCurrencySumModel {
    private Long userId;
    private String currency;
    // available+frozen
    private BigDecimal total;
    //折U资产
    private BigDecimal toUsdtAssets;
    //折BTC资产
    private BigDecimal toBtcAssets;

}
