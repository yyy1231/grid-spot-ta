package com.novax.mapper;

import com.novax.entity.mysql.SpotOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SpotOrderEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SpotOrderEntity record);

    SpotOrderEntity selectByPrimaryKey(Long id);

    List<SpotOrderEntity> selectAll();

    int updateByPrimaryKey(SpotOrderEntity record);

    List<SpotOrderEntity> selectInTradingOrders(String symbol);

    List<SpotOrderEntity> selectInTradingGridOrders(@Param("symbol") String symbol, @Param("gridId") Long gridId, @Param("tradeType") Integer tradeType);

    List<SpotOrderEntity> selectInitialOrders(@Param("userId") Long userId, @Param("gridId") Long gridId);

    List<SpotOrderEntity> selectInitialEntrust(@Param("userId") Long userId, @Param("gridId") Long gridId, @Param("tradeType") int tradeType);
}