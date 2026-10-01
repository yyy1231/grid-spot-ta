package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.SpotOrderEntity;
import com.novax.testcase.GridStatistics;
import com.novax.testcase.PublicParams;
import com.novax.util.HttpClientUtil;
import com.novax.util.NewPriceUtil;
import com.novax.util.PropertyUtil;
import com.novax.util.UrlProperty;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;

public class ClearAISIOrdersTest extends PublicParams {
    Logger logger = Logger.getLogger(GridAdvancedParametersTest.class);
    HttpClientUtil httpClientUtil;
    String url;
    String spotBaseUrl = "http://" + PropertyUtil.getSpotServerUrl() + ":" + PropertyUtil.getSpotServerPort();

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyCreate();
    }

    @Test(groups = {"smokeTest"}, description = "更新现货市场aisi_usdt的最新价为1，方便后续网格及下单委托价的设置；清存量委托单")
    public void aisi_newPriceReady() throws Exception {
        // 撤销存量现货委托单
        List<SpotOrderEntity> orders = GridStatistics.queryInTradingOrders("aisi_usdt");
        if (orders.size() != 0) {
            HashMap<String, String> bodys = new HashMap<>();
            reqHeaders.put("Content-type", "application/x-www-form-urlencoded");
            String requestUrl = spotBaseUrl + UrlProperty.getRepeaAlllOrders();
            JSONObject resp = httpClientUtil.sendPostByForm(requestUrl, bodys, reqHeaders);
            logger.info("aisi_usdt清理数据:" + resp.get("msg"));
        }

        // 撤销因网格终止异常交易中的委托单
        List<SpotOrderEntity> gridOrders = GridStatistics.queryInTradingGridOrders("aisi_usdt", null, null);
        if (gridOrders.size() != 0) {
            HashMap<String, String> bodys = new HashMap<>();
            reqHeaders.put("Content-type", "application/x-www-form-urlencoded");
            String requestUrl = spotBaseUrl + UrlProperty.getRepeaAllGridOrders();
            JSONObject resp = httpClientUtil.sendPostByForm(requestUrl, bodys, reqHeaders);
            logger.info("aisi_usdt清理数据:" + resp.get("msg"));
        }

        // 最新价置为1
        long result = NewPriceUtil.updateSpotPrice("spot:new:price", "aisi_usdt", 1.0);
        Assert.assertTrue(result == 1 || result == 0, "现货市场aisi_usdt最新价修改失败");
    }
}
