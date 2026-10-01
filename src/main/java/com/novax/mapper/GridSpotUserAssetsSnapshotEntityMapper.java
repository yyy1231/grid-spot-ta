package com.novax.mapper;

import com.novax.entity.mysql.GridSpotUserAssetsSnapshotEntity;

import java.util.List;

public interface GridSpotUserAssetsSnapshotEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GridSpotUserAssetsSnapshotEntity record);

    GridSpotUserAssetsSnapshotEntity selectByPrimaryKey(Long id);

    List<GridSpotUserAssetsSnapshotEntity> selectAll();

    int updateByPrimaryKey(GridSpotUserAssetsSnapshotEntity record);

    List<GridSpotUserAssetsSnapshotEntity> selectAssetSnapshot(List<Long> gridIds);
}