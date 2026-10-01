package com.novax.mapper;

import com.novax.entity.mysql.GridSpotStrategyEntity;

import java.util.List;

public interface GridSpotStrategyEntityMapper {
    GridSpotStrategyEntity selectByGridSpotOrderId(Long gridSpotOrderId);

    int deleteByPrimaryKey(Long id);

    int insert(GridSpotStrategyEntity record);

    GridSpotStrategyEntity selectByPrimaryKey(Long id);

    List<GridSpotStrategyEntity> selectAll();

    int updateByPrimaryKey(GridSpotStrategyEntity record);
}