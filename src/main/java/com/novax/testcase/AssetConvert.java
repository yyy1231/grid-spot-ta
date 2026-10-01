package com.novax.testcase;

import com.novax.entity.mysql.GridSpotUserAssetsEntity;
import com.novax.util.NewPriceUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AssetConvert {
    public static Map<String, BigDecimal> convert(List<GridSpotUserAssetsEntity> assets) {
        Map<String, BigDecimal> result = new HashMap<>();
        BigDecimal assetOfU = BigDecimal.ZERO;
        BigDecimal assetOfBtc = BigDecimal.ZERO;
        BigDecimal btcNewPrice = NewPriceUtil.newPriceOfSymbol("btc_usdt");
        for (GridSpotUserAssetsEntity entity : assets) {
            String currency = entity.getCurrency();
            BigDecimal usdtAssets;
            if (Objects.equals(currency, "usdt")) {
                usdtAssets = entity.getAvailable().add(entity.getFrozen());
            } else {
                BigDecimal newPrice = NewPriceUtil.newPriceOfSymbol(currency + "_usdt");
                usdtAssets = entity.getAvailable().add(entity.getFrozen()).multiply(newPrice);
            }
            // 折BTC
            BigDecimal btcAssets = usdtAssets.divide(btcNewPrice, 8, RoundingMode.FLOOR);

            assetOfU = assetOfU.add(usdtAssets);
            assetOfBtc = assetOfBtc.add(btcAssets);
        }
        result.put("usdt", assetOfU);
        result.put("btc", assetOfBtc);
        return result;
    }
}
