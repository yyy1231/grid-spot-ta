package com.novax.testcase;

import com.novax.entity.model.GridAssetsCurrencySumModel;
import com.novax.entity.mysql.GridSpotFinanceRecordEntity;
import com.novax.entity.mysql.GridSpotUserAssetsEntity;
import com.novax.entity.mysql.SpotUserAssetsEntity;
import com.novax.mapper.GridSpotFinanceRecordEntityMapper;
import com.novax.mapper.GridSpotOrderEntityMapper;
import com.novax.mapper.GridSpotUserAssetsEntityMapper;
import com.novax.mapper.SpotUserAssetsEntityMapper;
import com.novax.util.MyBatisUtil;
import org.testng.Assert;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class UserAssetsManage {
    // 查现货网格资产
    public static List<GridSpotUserAssetsEntity> queryGridAssets(Long userId, Long gridId, String currency) {
        return MyBatisUtil.execute(GridSpotUserAssetsEntityMapper.class, m -> m.selectGridAssets(userId, gridId, currency));
    }

    // 查现货网格资产 全部 按currency分组
    public static List<GridAssetsCurrencySumModel> queryGridCurrencyAssetsAll(Long userId) {
        return MyBatisUtil.execute(GridSpotUserAssetsEntityMapper.class, m -> m.selectGridCurrencyAssetsAll(userId));
    }

    // 查现货网格资产 运行中的(status=0,1,2,3) 按currency分组
    public static List<GridAssetsCurrencySumModel> queryGridCurrencyAssetsSum(Long userId) {
        return MyBatisUtil.execute(GridSpotUserAssetsEntityMapper.class, m -> m.selectGridCurrencyAssetsSum(userId));
    }

    // 查现货网格资产 运行中的(status=0,1,2) 按currency分组
    public static List<GridAssetsCurrencySumModel> queryGridCurrencyAssetsSum2(Long userId) {
        return MyBatisUtil.execute(GridSpotUserAssetsEntityMapper.class, m -> m.selectGridCurrencyAssetsSum2(userId));
    }

    // 查现货网格资产 运行中的
    public static List<GridSpotUserAssetsEntity> queryGridCurrencyAssets(Long userId, String currency) {
        return MyBatisUtil.execute(GridSpotUserAssetsEntityMapper.class, m -> m.selectGridCurrencyAssets(userId, currency));
    }

    // 计算网格投入资产
    public static BigDecimal queryInvestment(List<Long> gridIds) {
        return MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectInvestment(gridIds));
    }

    // 资产划转
    public static List<GridSpotFinanceRecordEntity> queryFinanceRecords(Long userId, Long gridId, int operation, String currency) {
        return MyBatisUtil.execute(GridSpotFinanceRecordEntityMapper.class, m -> m.selectFinanceRecords(userId, gridId, operation, currency));
    }

    // 查现货资产
    public static List<SpotUserAssetsEntity> queryAssets(Long userId, String currency) {
        return MyBatisUtil.execute(SpotUserAssetsEntityMapper.class, m -> m.selectByCurrency(userId, currency));
    }

    // 创建现货账户
    public static void createSpotAccount(Long userId, String currency, BigDecimal amount) {
        MyBatisUtil.executeVoid(SpotUserAssetsEntityMapper.class, mapper -> {
            SpotUserAssetsEntity spotUserAssetsEntity = mapper.selectByUserId(userId, currency, amount);
            if (spotUserAssetsEntity == null) {
                SpotUserAssetsEntity assetsEntity = SpotUserAssetsEntity.builder()
                        .tenantId(1L)
                        .userId(userId)
                        .currency(currency)
                        .available(amount)
                        .frozen(BigDecimal.ZERO)
                        .createTime(new Date())
                        .updateTime(new Date())
                        .build();
                int insert = mapper.insert(assetsEntity);
                Assert.assertEquals(insert, 1, "用户：" + userId + "现货账户创建失败");
            }
        });
    }

    // 更新现货账户资产
    public static void updateSpotAccount(Long userId, String currency, BigDecimal amount) {
        MyBatisUtil.executeVoid(SpotUserAssetsEntityMapper.class, mapper -> {
            List<SpotUserAssetsEntity> spotUserAssetsEntities = mapper.selectByCurrency(userId, currency);
            if (spotUserAssetsEntities.size() == 0) {
                createSpotAccount(userId, currency, amount);
            } else {
                SpotUserAssetsEntity assetsEntity = spotUserAssetsEntities.get(0);
                assetsEntity.setAvailable(amount);
                int i = mapper.updateByPrimaryKey(assetsEntity);
                Assert.assertEquals(i, 1, "用户：" + userId + "现货账户余额更新失败");
            }
        });
    }

    public static void main(String[] args) throws IOException {
        // System.out.println(queryGridAssets(28649983L, 518817546851446784L, "btc"));
        // System.out.println(queryGridCurrencyAssets(28649983L, "btc"));
        System.out.println(queryFinanceRecords(28649983L, 521750205768527872L, 8, "btc"));
    }
}
