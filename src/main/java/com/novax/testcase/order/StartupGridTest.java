package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.GridSpotOrderEntity;
import com.novax.mapper.GridSpotOrderEntityMapper;
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
import java.util.HashMap;

/**
 * 启动现货网格订单
 * POST/v1/gridSpotOrders/{orderId}/startup
 */
public class StartupGridTest extends PublicParams {
    Logger logger = Logger.getLogger(StartupGridTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyStartup();
    }

    @DataProvider(name = "datas_success")
    public Object[][] dataProvider1() throws IOException {
        return ExcelUtil.readObjDatas("/order/startupGrid.xls", 0);
    }

    @DataProvider(name = "datas_fail")
    public Object[][] dataProvider2() throws IOException {
        return ExcelUtil.readObjDatas("/order/startupGrid.xls", 1);
    }

    @Test(dataProvider = "datas_success", groups = {"smokeTest"}, description = "手动启动网格")
    public void startupGridSpotOrder_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        long orderId = Long.parseLong((String) context.getAttribute("orderId_startUp"));
        logger.info("订单号,orderId_startUp:" + orderId);
        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, new HashMap<>(), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), "网格启动失败：" + resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 断言订单状态
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        Assert.assertTrue(orderEntity.getStatus() != 0, "网格启动失败，订单状态断言不通过");

        // 断言网格事件 GridSpotManualstartUp
        Thread.sleep(10000);
        int result = GridStatistics.searchGridEvent(orderId, "GridSpotManualStartUp");
        Assert.assertTrue(result != 0, "网格：" + orderId + "网格启动异常，未查询到GridSpotManualStartUp事件；或断言早了，手动检查下");

        logger.info("********************pass********************");
    }

    @Test(dataProvider = "datas_fail", description = "手动启动网格异常场景")
    public void startupGridSpotOrder_Fail_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        String requestUrl = VariableReplace.replace(url, (String) context.getAttribute("orderId_startUp"));
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, new HashMap<>(), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
    }
}
