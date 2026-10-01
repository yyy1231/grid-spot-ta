package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.GridSpotUserAssetsEntity;
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
 * 网格资产-币种详情列表
 * GET/v1/gridSpotAssets/grid/desc
 * 网格状态!=4已终止
 * 包括冻结资产
 */

@Slf4j
public class QueryGridAssetsCurrencyListTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridAssetsCurrencyListTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getAssetsCurrencyList();
    }

    @DataProvider(name = "datas")
    public Object[][] getDatas() throws IOException {
        return ExcelUtil.readObjDatas("/assets/queryGridAssetsCurrencyList.xls", 0);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "datas", description = "查询网格资产币种详情列表")
    public void queryGridAssetsCurrencyListTest(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        Long userId = Long.valueOf(reqHeaders.get("userId"));
        HashMap<String, String> bodys = StringToMap.stringToMap(params);
        String currency = bodys.get("currency");

        JSONObject resp = httpClientUtil.sendGet(url, bodys, reqHeaders);

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"));
        Assert.assertEquals(resp.get("message"), JSONObject.parseObject(expect).get("message"));
        JSONArray data = resp.getJSONArray("data");
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < data.size(); i++) {
            BigDecimal available = data.getJSONObject(i).getBigDecimal("available");
            total = total.add(available);
        }

        // 断言
        List<GridSpotUserAssetsEntity> gridSpotUserAssetsEntities = UserAssetsManage.queryGridCurrencyAssets(userId, currency);
        BigDecimal totalExpect = BigDecimal.ZERO;
        for (GridSpotUserAssetsEntity entity : gridSpotUserAssetsEntities) {
            BigDecimal available = entity.getAvailable();
            BigDecimal frozen = entity.getFrozen();
            totalExpect = totalExpect.add(available).add(frozen);
        }

        Assert.assertEquals(total.compareTo(totalExpect), 0, "用户" + userId + "币种" + currency + "金额统计不一致，测试：" + total + "，预期：" + totalExpect);

    }
}
