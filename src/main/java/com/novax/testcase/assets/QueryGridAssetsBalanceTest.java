package com.novax.testcase.assets;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.GridSpotUserAssetsEntity;
import com.novax.testcase.PublicParams;
import com.novax.testcase.UserAssetsManage;
import com.novax.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;

/**
 * 查询用户网格订单余额
 * GET/v1/gridSpotAssets/{gridSpotOrderId}
 */

@Slf4j
public class QueryGridAssetsBalanceTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridAssetsBalanceTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getOrderBalance();
    }

    @DataProvider(name = "datas")
    public Object[][] getDatas() throws IOException {
        return ExcelUtil.readObjDatas("/assets/queryGridAssetsBalance.xls", 0);
    }

    @Test(groups = {"smokeTest"}, dataProvider = "datas", description = "查询网格订单余额")
    public void queryGridAssetsBalanceTest(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        Long userId = Long.valueOf(reqHeaders.get("userId"));
        HashMap<String, String> bodys = StringToMap.stringToMap(params);
        String currency = bodys.get("currency");

        // String attributeName = allAttributeNameList.get(1);
        Long orderId = Long.parseLong((String) context.getAttribute("orderId_manual"));
        // Long orderId = 518817546851446784L;

        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
        JSONObject resp = httpClientUtil.sendGet(requestUrl, bodys, reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"));
        Assert.assertEquals(resp.get("message"), JSONObject.parseObject(expect).get("message"));
        BigDecimal amount = resp.getJSONObject("data").getBigDecimal("amount");

        // 断言
        GridSpotUserAssetsEntity assetsEntity = UserAssetsManage.queryGridAssets(userId, orderId, currency).get(0);
        BigDecimal available = assetsEntity.getAvailable();
        Assert.assertEquals(amount.compareTo(available), 0, "网格：" + orderId + "资产统计不一致，测试：" + amount + "，预期：" + available);

        logger.info("********************pass********************");
    }
}
