package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.entity.model.GridAssetsCurrencySumModel;
import com.novax.testcase.PublicParams;
import com.novax.testcase.UserAssetsManage;
import com.novax.util.ExcelUtil;
import com.novax.util.HttpClientUtil;
import com.novax.util.StringToMap;
import com.novax.util.UrlProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

/**
 * 按币种分类查询运行中的现货网格订单资产
 * GET/v1/gridSpotAssets
 */

@Slf4j
public class QueryGridAssetsTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridAssetsTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getAssets();
    }

    @DataProvider(name = "datas")
    public Object[][] getDatas() throws IOException {
        return ExcelUtil.readObjDatas("/assets/queryGridAssets.xls", 0);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "datas", description = "按币种分类查询网格资产")
    public void queryGridSpotAssetsTest(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        HashMap<String, String> bodys = StringToMap.stringToMap(params);
        Long userId = Long.valueOf(reqHeaders.get("userId"));
        JSONObject resp = httpClientUtil.sendGet(url, bodys, reqHeaders);

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"));
        Assert.assertEquals(resp.get("message"), JSONObject.parseObject(expect).get("message"));
        JSONArray data = resp.getJSONArray("data");
        int size = data.size();

        List<GridAssetsCurrencySumModel> currencySumModels = UserAssetsManage.queryGridCurrencyAssetsSum(userId);
        int num = currencySumModels.size();

        Assert.assertEquals(size, num, "用户" + userId + "网格资产币种数量统计不一致");

        for (int i = 0; i < size; i++) {
            JSONObject jsonObject = data.getJSONObject(i);
            String currency = jsonObject.getString("currency");
            for (int j = 0; j < size; j++) {
                GridAssetsCurrencySumModel model = currencySumModels.get(j);
                if (currency.equals(model.getCurrency())) {
                    BigDecimal totalAmount = jsonObject.getBigDecimal("totalAmount");
                    BigDecimal total = model.getTotal();
                    Assert.assertTrue(totalAmount.subtract(total).abs().compareTo(BigDecimal.valueOf(0.0001)) <= 0, "用户" + userId + "币种" + currency + "资产统计不一致，测试：" + totalAmount + "，预期：" + total);
                    break;
                }

            }

        }
    }
}
