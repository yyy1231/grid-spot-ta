package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.testcase.GridStatistics;
import com.novax.testcase.PublicParams;
import com.novax.util.*;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * 查询现货网格订单下的事件记录列表
 * GET/v1/gridSpotOrders/{orderId}/events
 * mongo   对应关系   api
 * put("GridSpotCreated", "GridCreated");
 * put("GridSpotAutomatedStartUp", "GridTriggerStartUp");
 * put("GridSpotManualStartUp", "GridManualStartUp");
 * put("GridSpotInitialHoldingCompleted", "Gridstarted");
 * put("GridSpotStrategyTrailingUp", "GridTrailingUp");;
 * put("GridSpotStrategyTrailingUpFailed", "GridTrailingUpFailed");
 * put("GridSpotTerminateByTakeProfit"，，"GridTakeProfit");
 * put("GridSpotTerminateByStopLoss"，"GridstopLoss");
 * put("GridSpotTerminate", "GridTerminated");
 * put("GridSpotExceptionalTerminate", "GridExceptionalTerminated");
 */
public class QueryGridEventsTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridEntrustTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyEvents();
    }

    @DataProvider(name = "all")
    public Object[][] getProvider() throws IOException {
        return ExcelUtil.readObjDatas("/order/queryGridEvents.xls", 0);
    }

    @DataProvider(name = "autoStartUp")
    public Object[][] getProvider2() throws IOException {
        return ExcelUtil.readObjDatas("/order/queryGridEvents.xls", 1);
    }

    @Test(dataProvider = "all", groups = {"smokeTest"}, description = "全部网格，检查网格创建事件")
    public void queryGridSpotOrderEvents_GridCreated_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        // int index = Integer.parseInt(tcNum.substring(tcNum.length() - 1));
        int index = Integer.parseInt(tcNum.replaceAll(".*[^\\d](?=(\\d+))", ""));
        String attributeName = allAttributeNameList.get(index-1);
        Long orderId = Long.parseLong((String) context.getAttribute(attributeName));

        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
        JSONObject resp = httpClientUtil.sendGet(requestUrl, StringToMap.stringToMap(params), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 数据库断言，查询订单事件，mongo.grid-trading.grid_spot_event
        int searchResult = GridStatistics.searchGridEvent(Long.parseLong((String) context.getAttribute(attributeName)), "GridSpotCreated");
        String event = resp.getJSONObject("data").getJSONArray("items").getJSONObject(0).getString("eventType");
        Assert.assertTrue(searchResult != 0, "网格" + orderId + "异常：无网格创建事件");
        Assert.assertEquals(event, "GridCreated", "网格" + orderId + "事件异常");

        logger.info("********************pass********************");
    }

    @Test(dataProvider = "autoStartUp", groups = {"smokeTest"}, description = "针对未设置触发价的订单，检查网格自动启动事件")
    public void queryGridSpotOrderEvents_GridStarted_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        // int index = Integer.parseInt(tcNum.substring(tcNum.length() - 1));
        int index = Integer.parseInt(tcNum.replaceAll(".*[^\\d](?=(\\d+))", ""));
        String attributeName = autoStartUpAttributeNameList.get(index-1);
        Long orderId = Long.parseLong((String) context.getAttribute(attributeName));
        // Long orderId = 518751910653718528L;

        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
        JSONObject resp = httpClientUtil.sendGet(requestUrl, StringToMap.stringToMap(params), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 数据库断言，查询订单事件，mongo.superex_grid_trading_test.grid_spot_event
        int searchResult = GridStatistics.searchGridEvent(orderId, "GridSpotInitialHoldingCompleted");
        String event = resp.getJSONObject("data").getJSONArray("items").getJSONObject(1).getString("eventType");
        Assert.assertTrue(searchResult != 0, "网格" + orderId + "异常：无自动启动事件。订单未启动，或初始化过程异常，检查初始建仓及初始委托数据");
        Assert.assertEquals(event, "GridStarted", "网格" + orderId + "事件异常，网格未启动，或初始化过程异常，检查初始建仓及初始委托数据");


        logger.info("********************pass********************");
    }
}
