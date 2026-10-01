package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.model.GridAssetsCurrencySumModel;
import com.novax.entity.mysql.GridSpotOrderEntity;
import com.novax.entity.mysql.GridSpotUserAssetsSnapshotEntity;
import com.novax.testcase.GridStatistics;
import com.novax.testcase.PublicParams;
import com.novax.testcase.UserAssetsManage;
import com.novax.util.ExcelUtil;
import com.novax.util.HttpClientUtil;
import com.novax.util.NewPriceUtil;
import com.novax.util.UrlProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 网格总资产收益
 * GET/v1/gridSpotAssets/total/grid
 */

@Slf4j
public class QueryGridTotalIncomeTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridTotalIncomeTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getTotalIncome();
    }

    @DataProvider(name = "datas")
    public Object[][] getDatas() throws IOException {
        return ExcelUtil.readObjDatas("/assets/queryGridTotalIncome.xls", 0);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "datas", description = "查询网格总资产收益")
    public void queryGridSpotAssetsTest(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        Long userId = Long.valueOf(reqHeaders.get("userId"));
        JSONObject resp = httpClientUtil.sendGet(url, new HashMap<>(), reqHeaders);

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + null);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"));
        Assert.assertEquals(resp.get("message"), JSONObject.parseObject(expect).get("message"));

        JSONObject data = resp.getJSONObject("data");
        // 当前总资产
        BigDecimal currentAssetUsd = data.getBigDecimal("currentAssetUsd");
        BigDecimal totalBtc = data.getBigDecimal("totalBtc");
        // 当前收益
        BigDecimal currentEarningsUsd = data.getBigDecimal("currentEarningsUsd");
        BigDecimal currentEarningsBtc = data.getBigDecimal("currentEarningsBtc");
        // 总收益
        BigDecimal totalRevenueUsd = data.getBigDecimal("totalRevenueUsd");
        BigDecimal totalRevenueBtc = data.getBigDecimal("totalRevenueBtc");

        // 断言
        // 1、断言总资产 用户全部网格的总资产
        List<GridAssetsCurrencySumModel> gridAssetsCurrencySumModels = UserAssetsManage.queryGridCurrencyAssetsAll(userId);
        Map<String, BigDecimal> result = assetsCalculate(gridAssetsCurrencySumModels);
        BigDecimal currentAssetUsdExpect = result.get("usdt");
        BigDecimal totalBtcExpect = result.get("btc");

        Assert.assertTrue(currentAssetUsd.subtract(currentAssetUsdExpect).abs().compareTo(BigDecimal.valueOf(20)) <= 0,
                "用户" + userId + "全部网格总资产折U计算不一致，测试：" + currentAssetUsd + "，预期：" + currentAssetUsdExpect);
        Assert.assertTrue(totalBtc.subtract(totalBtcExpect).abs().compareTo(BigDecimal.valueOf(0.001)) <= 0,
                "用户" + userId + "全部网格总资产折BTC计算不一致，测试：" + totalBtc + "，预期：" + totalBtcExpect);

        // 2、断言当前收益 运行中网格总资产-运行中网格总投入
        // 运行中的网格
        List<GridSpotOrderEntity> gridSpotOrderEntities = GridStatistics.queryGridsOnRunning(userId);
        BigDecimal currentEarningsUsdExpect = BigDecimal.ZERO;
        BigDecimal currentEarningsBtcExpect = BigDecimal.ZERO;
        BigDecimal btcNewPrice = NewPriceUtil.newPriceOfSymbol("btc_usdt");
        if (gridSpotOrderEntities.size() != 0) {
            List<Long> runningGridIds = gridSpotOrderEntities.stream()
                    .map(GridSpotOrderEntity::getId)
                    .collect(Collectors.toList());
            // 运行中网格总资产
            List<GridAssetsCurrencySumModel> gridAssetsCurrencySumModels1 = UserAssetsManage.queryGridCurrencyAssetsSum2(userId);
            Map<String, BigDecimal> result1 = assetsCalculate(gridAssetsCurrencySumModels1);
            BigDecimal usdtAssets = result1.get("usdt");
            // 运行中网格总投入
            BigDecimal investmentUsdt = UserAssetsManage.queryInvestment(runningGridIds);
            currentEarningsUsdExpect = usdtAssets.subtract(investmentUsdt);
            currentEarningsBtcExpect = currentEarningsUsdExpect.divide(btcNewPrice, 8, RoundingMode.FLOOR);

        }
        Assert.assertTrue(currentEarningsUsd.subtract(currentEarningsUsdExpect).abs().compareTo(BigDecimal.valueOf(1)) <= 0,
                "用户" + userId + "运行中网格当前收益折U计算不一致，测试：" + currentEarningsUsd + "，预期：" + currentEarningsUsdExpect);
        Assert.assertTrue(currentEarningsBtc.subtract(currentEarningsBtcExpect).abs().compareTo(BigDecimal.valueOf(0.0001)) <= 0,
                "用户" + userId + "运行中网格当前收益折BTC计算不一致，测试：" + currentEarningsBtc + "，预期：" + currentEarningsBtcExpect);

        // 3、断言总收益 运行中网格收益(当前资产-投入)+已终止网格的收益(资产快照event1-event0)
        // TODO 异常场景 网格状态最终停留在status=3终止中 或没有event1快照记录 作为已终止网格去统计收益不准确
        // 已终止网格
        BigDecimal terminatedGridProfitUsd = BigDecimal.ZERO;
        List<GridSpotOrderEntity> gridSpotOrderEntities2 = GridStatistics.queryAllGrids(userId, 4);
        if (gridSpotOrderEntities2.size() != 0) {
            List<Long> terminatedGridIds = gridSpotOrderEntities2.stream()
                    .map(GridSpotOrderEntity::getId)
                    .collect(Collectors.toList());
            // 已终止网格event=1 资产之和
            BigDecimal usdtAssets2 = BigDecimal.ZERO;
            List<GridSpotUserAssetsSnapshotEntity> gridSpotUserAssetsSnapshotEntities = GridStatistics.queryAssetSnapshot(terminatedGridIds);
            for (GridSpotUserAssetsSnapshotEntity snapShot : gridSpotUserAssetsSnapshotEntities) {
                BigDecimal baseCurrencyAvailable = snapShot.getBaseCurrencyAvailable();
                BigDecimal baseCurrencyFrozen = snapShot.getBaseCurrencyFrozen();
                BigDecimal baseCurrencyToUsdtNewPrice = snapShot.getBaseCurrencyToUsdtNewPrice();
                BigDecimal quoteCurrencyAvailable = snapShot.getQuoteCurrencyAvailable();
                BigDecimal quoteCurrencyFrozen = snapShot.getQuoteCurrencyFrozen();
                BigDecimal quoteCurrencyToUsdtNewPrice = snapShot.getQuoteCurrencyToUsdtNewPrice();
                BigDecimal baseCurrencyOfU = baseCurrencyAvailable.add(baseCurrencyFrozen).multiply(baseCurrencyToUsdtNewPrice);
                BigDecimal quoteCurrencyOfU = quoteCurrencyAvailable.add(quoteCurrencyFrozen).multiply(quoteCurrencyToUsdtNewPrice);
                usdtAssets2 = usdtAssets2.add(baseCurrencyOfU).add(quoteCurrencyOfU).setScale(8, RoundingMode.DOWN);
            }
            // 已终止网格投入之和
            BigDecimal investmentUsdt2 = UserAssetsManage.queryInvestment(terminatedGridIds);

            terminatedGridProfitUsd = usdtAssets2.subtract(investmentUsdt2);
        }

        BigDecimal totalRevenueUsdExpect = terminatedGridProfitUsd.add(currentEarningsUsdExpect);
        BigDecimal totalRevenueBtcExpect = totalRevenueUsdExpect.divide(btcNewPrice, 8, RoundingMode.FLOOR);

        Assert.assertTrue(totalRevenueUsd.subtract(totalRevenueUsdExpect).abs().compareTo(BigDecimal.valueOf(1)) <= 0,
                "用户" + userId + "全部网格总收益折U计算不一致，测试：" + totalRevenueUsd + "，预期：" + totalRevenueUsdExpect);
        Assert.assertTrue(totalRevenueBtc.subtract(totalRevenueBtcExpect).abs().compareTo(BigDecimal.valueOf(0.0002)) <= 0,
                "用户" + userId + "全部网格总收益折BTC计算不一致，测试：" + totalRevenueBtc + "，预期：" + totalRevenueBtcExpect);

    }

    public static Map<String, BigDecimal> assetsCalculate(List<GridAssetsCurrencySumModel> entities) {
        Map<String, BigDecimal> result = new HashMap<>();
        BigDecimal currentAssetUsdExpect = BigDecimal.ZERO;
        BigDecimal totalBtcExpect = BigDecimal.ZERO;
        BigDecimal btcNewPrice = NewPriceUtil.newPriceOfSymbol("btc_usdt");
        for (GridAssetsCurrencySumModel entity : entities) {
            String currency = entity.getCurrency();
            BigDecimal usdtAssets;
            if (Objects.equals(currency, "usdt")) {
                usdtAssets = entity.getTotal();
            } else {
                BigDecimal newPrice = NewPriceUtil.newPriceOfSymbol(currency + "_usdt");
                usdtAssets = entity.getTotal().multiply(newPrice);
            }
            // 折BTC
            BigDecimal btcAssets = usdtAssets.divide(btcNewPrice, 8, RoundingMode.FLOOR);

            currentAssetUsdExpect = currentAssetUsdExpect.add(usdtAssets);
            totalBtcExpect = totalBtcExpect.add(btcAssets);
        }
        result.put("usdt", currentAssetUsdExpect);
        result.put("btc", totalBtcExpect);
        return result;
    }

    public static void main(String[] args) {
        BigDecimal a = new BigDecimal(2);
        BigDecimal b = new BigDecimal(3);
        BigDecimal c = new BigDecimal(4);
        System.out.println(a.add(b).multiply(c));

    }
}
