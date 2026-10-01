package com.novax.mapper;

import com.novax.entity.mysql.GridSpotFinanceRecordEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GridSpotFinanceRecordEntityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(GridSpotFinanceRecordEntity record);

    GridSpotFinanceRecordEntity selectByPrimaryKey(Long id);

    List<GridSpotFinanceRecordEntity> selectAll();

    int updateByPrimaryKey(GridSpotFinanceRecordEntity record);

    List<GridSpotFinanceRecordEntity> selectFinanceRecords(@Param("userId") Long userId, @Param("gridId") Long gridId, @Param("operation") int operation, @Param("currency") String currency);
}