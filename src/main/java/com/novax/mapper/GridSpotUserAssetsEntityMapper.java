package com.novax.mapper;

import com.novax.entity.model.GridAssetsCurrencySumModel;
import com.novax.entity.mysql.GridSpotUserAssetsEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GridSpotUserAssetsEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GridSpotUserAssetsEntity record);

    GridSpotUserAssetsEntity selectByPrimaryKey(Long id);

    List<GridSpotUserAssetsEntity> selectAll();

    int updateByPrimaryKey(GridSpotUserAssetsEntity record);

    List<GridSpotUserAssetsEntity> selectGridAssets(@Param("userId") Long userId, @Param("gridId") Long gridId, @Param("currency") String currency);

    List<GridSpotUserAssetsEntity> selectGridCurrencyAssets(@Param("userId") Long userId, @Param("currency") String currency);

    List<GridAssetsCurrencySumModel> selectGridCurrencyAssetsSum(Long userId);

    List<GridAssetsCurrencySumModel> selectGridCurrencyAssetsAll(Long userId);

    List<GridAssetsCurrencySumModel> selectGridCurrencyAssetsSum2(Long userId);
}