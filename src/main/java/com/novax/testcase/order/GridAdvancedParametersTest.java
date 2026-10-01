package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.GridSpotOrderEntity;
import com.novax.entity.mysql.GridSpotStrategyEntity;
import com.novax.entity.mysql.SpotOrderEntity;
import com.novax.testcase.GridStatistics;
import com.novax.testcase.PublicParams;
import com.novax.testcase.UserAssetsManage;
import com.novax.util.*;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;


/**
 * 网格高级参数测试：上移、触发价格、止盈止损
 * 测试场景:网格触发、网格上移、网格止盈
 * 交易市场：AISI/USDT
 * 前置：准备单独账号；现货账户充值aisi & usdt
 * step1：行情最新价设置为1
 * step2：创建网格0.5-1.5 网格数10 触发价1.1 上移上限1.65 止盈1.72 待触发
 * --status=0待触发   event_type=GridSpotCreated
 * step3：以网格触发价 下买卖单 最新价达到触发价 网格状态更新到待启用
 * --status=1初始化中  event_type=GridSpotAutomatedStartUp
 * step4：下卖单(以高于最新价的第一个网格价格 * 与网格初始建仓买单数量一致) 撮合 网格状态更新到启用中
 * --status=2运行中   event_type=GridSpotInitialHoldingCompleted
 * step5: 继续下买单 撮合全部卖单 最新价更新到网格上限
 * step6: 以（网格上限价+1个网格价差）下买卖单 触发网格上移 检查网格事件
 * --event_type=GridSpotStrategyTrailingUp
 * step7: 以（网格上限价+2个网格价差）下买卖单 触发网格上移 超出上移上限 不上移
 * step8: 继续以止盈价下买卖单 触发网格止盈 检查网格事件
 * --event_type=GridSpotTerminateByTakeProfit
 */

public class GridAdvancedParametersTest extends PublicParams {
    Logger logger = Logger.getLogger(GridAdvancedParametersTest.class);
    HttpClientUtil httpClientUtil;
    String url;
    Long userId;
    Long gridId;

    String spotBaseUrl = "http://" + PropertyUtil.getSpotServerUrl() + ":" + PropertyUtil.getSpotServerPort();

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyCreate();
    }

    @DataProvider(name = "createGrid")
    public Object[][] dataProvider1() throws IOException {
        return ExcelUtil.readObjDatas("/order/gridAdvancedParameters.xls", 0);
    }

    @DataProvider(name = "spotOrder")
    public Object[][] dataProvider2() throws IOException {
        return ExcelUtil.readObjDatas("/order/gridAdvancedParameters.xls", 1);
    }

    @DataProvider(name = "sellOrder")
    public Object[][] dataProvider3() throws IOException {
        return ExcelUtil.readObjDatas("/order/gridAdvancedParameters.xls", 2);
    }

    @DataProvider(name = "buyOrder")
    public Object[][] dataProvider4() throws IOException {
        return ExcelUtil.readObjDatas("/order/gridAdvancedParameters.xls", 3);
    }

    @DataProvider(name = "spotOrder2")
    public Object[][] dataProvider5() throws IOException {
        return ExcelUtil.readObjDatas("/order/gridAdvancedParameters.xls", 4);
    }

    @DataProvider(name = "spotOrder3")
    public Object[][] dataProvider6() throws IOException {
        return ExcelUtil.readObjDatas("/order/gridAdvancedParameters.xls", 5);
    }

    @DataProvider(name = "spotOrder4")
    public Object[][] dataProvider7() throws IOException {
        return ExcelUtil.readObjDatas("/order/gridAdvancedParameters.xls", 6);
    }

    @Test(groups = {"smokeTest"}, description = "用户资产准备")
    public void a_userReady() {
        userId = Long.valueOf(reqHeaders.get("userId"));
        UserAssetsManage.updateSpotAccount(userId, "aisi", BigDecimal.valueOf(500));
    }

    // @Test(groups = {"smokeTest"}, description = "step1:更新现货市场aisi_usdt的最新价为1，方便后续网格及下单委托价的设置；清存量委托单", enabled = false)
    public void b_newPriceReady() throws Exception {
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

    @Test(dataProvider = "createGrid", groups = {"smokeTest"}, description = "step2：创建网格")
    public void c_createGrid(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        reqHeaders.put("Content-type", "application/json");
        JSONObject bodys = JSONObject.parseObject(params);
        JSONObject resp = httpClientUtil.sendPostByJson(url, bodys, reqHeaders);

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
        gridId = resp.getJSONObject("data").getLong("id");
        Thread.sleep(1000);

        context.setAttribute("aisi", resp.getJSONObject("data").get("id"));
        allAttributeNameList.add("aisi");
        logger.info("allAttributeNameList:" + allAttributeNameList);

        // 断言网格状态
        GridSpotOrderEntity grid = GridStatistics.queryGrid(gridId);
        byte status = grid.getStatus();
        Assert.assertEquals(status, 0, "网格" + gridId + "状态异常，预期0待触发");

        logger.info("********************pass********************");
    }

    @Test(dataProvider = "spotOrder", groups = {"smokeTest"}, description = "step3：提交买卖委托，造盘口行情，触发网格")
    public void d_spot_order_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        reqHeaders.put("userId", String.valueOf(userId));
        JSONObject bodys = JSONObject.parseObject(params);

        String requestUrl = spotBaseUrl + UrlProperty.getSpotOrder();
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, bodys, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + requestUrl);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 断言网格状态 买卖单撮合后断言
        // TODO tips:这里的断言去掉 网格触发后初始化(初始建仓)有时间限制，尽快进入下一步，避免初始建仓失败网格异常终止
        // if (tcNum.equals("TC2")) {
        //     Thread.sleep(3000);
        //     GridSpotOrderEntity grid = GridStatistics.queryGrid(gridId);
        //     byte status = grid.getStatus();
        //     Assert.assertEquals(status, 1, "网格" + gridId + "状态异常，预期1初始化中");
        // }

        logger.info("********************pass********************");
    }

    @Test(dataProvider = "sellOrder", groups = {"smokeTest"}, description = "step4：下卖单，与网格初始买单撮合，完成初始化建仓")
    public void e_spot_order_sell_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        reqHeaders.put("userId", String.valueOf(userId));
        JSONObject bodys = JSONObject.parseObject(params);

        String requestUrl = spotBaseUrl + UrlProperty.getSpotOrder();
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, bodys, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + requestUrl);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 断言网格状态
        byte status = 1;
        for (int i = 0; i < 10; i++) {
            GridSpotOrderEntity grid = GridStatistics.queryGrid(gridId);
            status = grid.getStatus();
            if (status == 2) break;
            Thread.sleep(5000);
        }
        Assert.assertEquals(status, 2, "网格" + gridId + "状态异常，预期2运行中；或断言早了，手动检查下");

        logger.info("********************pass********************");
    }

    @Test(dataProvider = "buyOrder", groups = {"smokeTest"}, description = "step5：下买单，撮合网格所有卖单，最新价更新到网格上限")
    public void f_spot_order_buy_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        reqHeaders.put("userId", String.valueOf(userId));
        JSONObject bodys = JSONObject.parseObject(params);

        String requestUrl = spotBaseUrl + UrlProperty.getSpotOrder();
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, bodys, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + requestUrl);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 断言网格状态
        Thread.sleep(2000);
        GridSpotOrderEntity grid = GridStatistics.queryGrid(gridId);
        byte status = grid.getStatus();
        Assert.assertEquals(status, 2, "网格" + gridId + "状态异常，预期2运行中，测试：" + status);

        logger.info("********************pass********************");
    }

    @Test(dataProvider = "spotOrder2", groups = {"smokeTest"}, description = "step6：以（网格上限价+1个网格价差）提交买卖委托，造盘口行情，触发网格上移")
    public void g_spot_order2_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        reqHeaders.put("userId", String.valueOf(userId));
        JSONObject bodys = JSONObject.parseObject(params);

        String requestUrl = spotBaseUrl + UrlProperty.getSpotOrder();
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, bodys, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + requestUrl);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 买卖单撮合后断言
        if (tcNum.equals("TC2")) {
            Thread.sleep(20000);
            // 断言上移后的策略形态
            GridSpotStrategyEntity strategyEntity = GridStatistics.queryGridStrategy(gridId);
            BigDecimal lowerLimitPrice = strategyEntity.getLowerLimitPrice();
            BigDecimal upperLimitPrice = strategyEntity.getUpperLimitPrice();
            Assert.assertEquals(lowerLimitPrice.compareTo(BigDecimal.valueOf(0.6)), 0, "网格" + gridId + "上移后，网格下限异常，测试：" + lowerLimitPrice + "，预期：0.6");
            Assert.assertEquals(upperLimitPrice.compareTo(BigDecimal.valueOf(1.6)), 0, "网格" + gridId + "上移后，网格上限异常，测试：" + upperLimitPrice + "，预期：1.6");
            // 断言上移事件
            int num = GridStatistics.searchGridEvent(gridId, "GridSpotStrategyTrailingUp");
            Assert.assertEquals(num, 1, "网格" + gridId + "上移失败；或断言早了，手动检查下");
        }

        logger.info("********************pass********************");
    }

    @Test(dataProvider = "spotOrder3", groups = {"smokeTest"}, description = "step7：以（网格上限价+2个网格价差）提交买卖委托，造盘口行情，触发网格上移，超出上移上限，不上移")
    public void h_spot_order3_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        reqHeaders.put("userId", String.valueOf(userId));
        JSONObject bodys = JSONObject.parseObject(params);

        String requestUrl = spotBaseUrl + UrlProperty.getSpotOrder();
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, bodys, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + requestUrl);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 买卖单撮合后断言 超出上移上限 不再上移
        if (tcNum.equals("TC2")) {
            Thread.sleep(20000);
            // 断言上移后的策略形态
            GridSpotStrategyEntity strategyEntity = GridStatistics.queryGridStrategy(gridId);
            BigDecimal lowerLimitPrice = strategyEntity.getLowerLimitPrice();
            BigDecimal upperLimitPrice = strategyEntity.getUpperLimitPrice();
            Assert.assertEquals(lowerLimitPrice.compareTo(BigDecimal.valueOf(0.6)), 0, "网格" + gridId + "上移后，网格下限异常，测试：" + lowerLimitPrice + "，预期：0.6");
            Assert.assertEquals(upperLimitPrice.compareTo(BigDecimal.valueOf(1.6)), 0, "网格" + gridId + "上移后，网格上限异常，测试：" + upperLimitPrice + "，预期：1.6");
        }

        logger.info("********************pass********************");
    }

    @Test(dataProvider = "spotOrder4", groups = {"smokeTest"}, description = "step8：继续以止盈价下买卖单，触发网格止盈")
    public void i_spot_order4_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        reqHeaders.put("userId", String.valueOf(userId));
        JSONObject bodys = JSONObject.parseObject(params);

        String requestUrl = spotBaseUrl + UrlProperty.getSpotOrder();
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, bodys, reqHeaders);

        logger.info(tcNum + "-requestUrl:" + requestUrl);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 断言网格事件 买卖单撮合后断言
        if (tcNum.equals("TC2")) {
            int num = 0;
            for (int i = 0; i < 12; i++) {
                num = GridStatistics.searchGridEvent(gridId, "GridSpotTerminateByTakeProfit");
                if (num > 0) break;
                Thread.sleep(5000);
            }
            // 断言网格状态
            GridSpotOrderEntity grid = GridStatistics.queryGrid(gridId);
            byte status = grid.getStatus();
            Assert.assertEquals(status, 4, "网格" + gridId + "状态异常，预期4已终止;终止流程较耗时，或断言早了，手动检查下");
            Assert.assertEquals(num, 1, "网格" + gridId + "止盈失败；或断言早了，手动检查下");
        }

        logger.info("********************pass********************");
    }

}
