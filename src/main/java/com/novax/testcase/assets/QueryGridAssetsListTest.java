package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.entity.model.GridAssetsCurrencySumModel;
import com.novax.testcase.PublicParams;
import com.novax.testcase.UserAssetsManage;
import com.novax.util.ExcelUtil;
import com.novax.util.HttpClientUtil;
import com.novax.util.NewPriceUtil;
import com.novax.util.UrlProperty;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 网格资产列表
 * GET/v1/gridSpotAssets/grid/list
 * 网格状态!=4已终止
 * 包括冻结资产
 */
public class QueryGridAssetsListTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridAssetsListTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getAssetsList();
    }

    @DataProvider(name = "datas")
    public Object[][] getDatas() throws IOException {
        return ExcelUtil.readObjDatas("/assets/queryGridAssetsList.xls", 0);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "datas", description = "查询网格资产列表")
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

        JSONArray data = resp.getJSONArray("data");
        int size = data.size();

        // 断言
        List<GridAssetsCurrencySumModel> currencySumModels = UserAssetsManage.queryGridCurrencyAssetsSum(userId);
        for (GridAssetsCurrencySumModel model : currencySumModels) {
            String currency = model.getCurrency();
            if (Objects.equals(currency, "usdt")) {
                model.setToUsdtAssets(model.getTotal());
            } else {
                BigDecimal newPrice = NewPriceUtil.newPriceOfSymbol(currency + "_usdt");
                model.setToUsdtAssets(model.getTotal().multiply(newPrice));
            }
        }

        int num = currencySumModels.size();
        Assert.assertEquals(size, num, "用户" + userId + "网格资产币种数量不一致");


        for (int i = 0; i < size; i++) {
            JSONObject jsonObject = data.getJSONObject(i);
            String currency = jsonObject.getString("currency");
            for (int j = 0; j < size; j++) {
                GridAssetsCurrencySumModel model = currencySumModels.get(j);
                if (currency.equals(model.getCurrency())) {
                    BigDecimal available = jsonObject.getBigDecimal("available");
                    BigDecimal usdAssets = jsonObject.getBigDecimal("usdAssets");
                    BigDecimal total = model.getTotal();
                    BigDecimal usdtAssets = model.getToUsdtAssets();
                    Assert.assertTrue(available.subtract(total).abs().compareTo(BigDecimal.valueOf(0.0001)) <= 0, "用户" + userId + "币种" + currency + "资产统计不一致，测试：" + available + "，预期：" + total);
                    Assert.assertTrue(usdAssets.subtract(usdtAssets).abs().compareTo(BigDecimal.valueOf(1)) <= 0, "用户" + userId + "币种" + currency + "折合U资产统计不一致，测试：" + usdAssets + "，预期：" + usdtAssets);
                    break;
                }

            }

        }
    }
}
