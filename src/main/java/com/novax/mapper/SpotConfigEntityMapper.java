package com.novax.mapper;

import com.novax.entity.mysql.SpotConfigEntity;

import java.util.List;

public interface SpotConfigEntityMapper {
    SpotConfigEntity selectBySymbol(String symbol);

    int deleteByPrimaryKey(Long id);

    int insert(SpotConfigEntity record);

    SpotConfigEntity selectByPrimaryKey(Long id);

    List<SpotConfigEntity> selectAll();

    int updateByPrimaryKey(SpotConfigEntity record);
}