package com.novax.mapper;

import com.novax.entity.mysql.GridSpotStrategyTemplateEntity;

import java.util.List;

public interface GridSpotStrategyTemplateEntityMapper {
    List<GridSpotStrategyTemplateEntity> selectBySymbol(String symbol);
    int deleteByPrimaryKey(Long id);

    int insert(GridSpotStrategyTemplateEntity record);

    GridSpotStrategyTemplateEntity selectByPrimaryKey(Long id);

    List<GridSpotStrategyTemplateEntity> selectAll();

    int updateByPrimaryKey(GridSpotStrategyTemplateEntity record);
}