package com.novax.mapper;

import com.novax.entity.mysql.GridSpotOrderTransactionRecordEntity;

import java.math.BigDecimal;
import java.util.List;

public interface GridSpotOrderTransactionRecordEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GridSpotOrderTransactionRecordEntity record);

    GridSpotOrderTransactionRecordEntity selectByPrimaryKey(Long id);

    List<GridSpotOrderTransactionRecordEntity> selectAll();

    int updateByPrimaryKey(GridSpotOrderTransactionRecordEntity record);

    BigDecimal selectOrderFee(Long orderId);
}