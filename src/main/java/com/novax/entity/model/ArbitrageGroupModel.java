package com.novax.entity.model;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ArbitrageGroupModel {
    // 套利组id
    private Long groupId;
    // 套利时间
    private Long arbitrageTime;
    // 套利金额
    private BigDecimal arbitrageAmount;
}
