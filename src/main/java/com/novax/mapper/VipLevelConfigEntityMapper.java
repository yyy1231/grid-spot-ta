package com.novax.mapper;

import com.novax.entity.mysql.VipLevelConfigEntity;

import java.util.List;

public interface VipLevelConfigEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(VipLevelConfigEntity record);

    VipLevelConfigEntity selectByPrimaryKey(Long id);

    List<VipLevelConfigEntity> selectAll();

    int updateByPrimaryKey(VipLevelConfigEntity record);
    List<VipLevelConfigEntity> selectConfigs();
}