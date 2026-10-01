package com.novax.entity.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GridArbitrageModel {
    // 套利组
    private List<ArbitrageGroupModel> list;
    // 套利次数
    private int arbitrageTimes;
}
