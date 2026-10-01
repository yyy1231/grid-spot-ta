package com.novax.mapper;

import com.novax.entity.condition.GridSpotOrderSelectEntity;
import com.novax.entity.mysql.GridSpotOrderEntity;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface GridSpotOrderEntityMapper {
    int count(GridSpotOrderSelectEntity condition);
    List<GridSpotOrderEntity> selectByCondition(GridSpotOrderSelectEntity condition);
    int deleteByPrimaryKey(Long id);

    int insert(GridSpotOrderEntity record);

    GridSpotOrderEntity selectByPrimaryKey(Long id);

    List<GridSpotOrderEntity> selectAll();

    int updateByPrimaryKey(@Param("id")Long id);

    List<GridSpotOrderEntity> selectGridsOnRunning(Long userId);

    BigDecimal selectInvestment(List<Long> grids);

    List<GridSpotOrderEntity> selectAllGrids(@Param("userId") Long userId,@Param("status") Integer status);
}