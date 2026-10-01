package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.GridSpotOrderEntity;
import com.novax.entity.mysql.GridSpotStrategyEntity;
import com.novax.mapper.GridSpotOrderEntityMapper;
import com.novax.mapper.GridSpotStrategyEntityMapper;
import com.novax.testcase.PublicParams;
import com.novax.util.*;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 修改现货网格订单
 * PUT/v1/gridSpotOrders/{orderId}
 */
public class UpdateGridTest extends PublicParams {
    Logger logger = Logger.getLogger(UpdateGridTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyUpdate();
    }

    @DataProvider(name = "datas_success")
    public Object[][] dataProvider1() throws IOException {
        return ExcelUtil.readObjDatas("/order/updateGrid.xls", 0);
    }

    @DataProvider(name = "datas_fail")
    public Object[][] dataProvider2() throws IOException {
        return ExcelUtil.readObjDatas("/order/updateGrid.xls", 1);
    }

    @Test(dataProvider = "datas_success", groups = {"smokeTest"}, description = "修改现货网格订单")
    public void updateGridSpotOrder_trigger_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        Long orderId;
        String requestUrl;
        if (tcDescp.equals("修改现货网格订单-待触发状态订单")) {
            orderId = Long.parseLong((String) context.getAttribute("orderId_triggerUp"));
            // orderId = 518109777315885056L;
            requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
            logger.info("订单号,orderId_triggerUp:" + orderId);
        } else {
            orderId = Long.parseLong((String) context.getAttribute("orderId_manual"));
            // orderId = 518109598831472640L;
            requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
            logger.info("订单号,orderId_manual:" + orderId);
        }

        GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));

        String symbol = orderEntity.getSymbol();
        String value = RedisUtil.getJedis().hget("spot:new:price", symbol);
        BigDecimal newestPrice = JSONObject.parseObject(value).getBigDecimal("price");
        logger.info(symbol + ",最新价：" + value);

        JSONObject requestParams = JSONObject.parseObject(params);
        if (tcDescp.equals("修改现货网格订单-待触发状态订单")) {
            // 修改触发价，仅待触发状态订单
            requestParams.put("runningTriggerPrice", strategyEntity.getRunningTriggerPrice().add(BigDecimal.TEN).setScale(0, RoundingMode.FLOOR));
        }
        // 修改止盈、止损价
        requestParams.put("stopLossPrice", strategyEntity.getLowerLimitPrice().subtract(BigDecimal.TEN).setScale(0, RoundingMode.FLOOR));
        requestParams.put("takeProfitPrice", strategyEntity.getUpperLimitPrice().add(BigDecimal.TEN).setScale(0, RoundingMode.FLOOR));
        // 修改终止时是否卖出
        requestParams.put("sellWhileTerminating", !requestParams.getBoolean("sellWhileTerminating"));

        JSONObject resp = httpClientUtil.sendPut(requestUrl, requestParams, reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + requestParams);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"),
                "策略修改失败:" + resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 数据库断言，grid_spot_strategy，runningTriggerPrice 网格触发价格
        GridSpotStrategyEntity strategyEntity_update = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        if (tcDescp.equals("修改现货网格订单-待触发状态订单")) {
            Assert.assertEquals(new BigDecimal(strategyEntity_update.getRunningTriggerPrice().stripTrailingZeros().toPlainString()),
                    new BigDecimal(requestParams.getBigDecimal("runningTriggerPrice").stripTrailingZeros().toPlainString()),
                    "触发价修改失败");
        }
        // stopLossPrice止损终止价格
        Assert.assertEquals(new BigDecimal(strategyEntity_update.getStopLossPrice().stripTrailingZeros().toPlainString()),
                new BigDecimal(requestParams.getBigDecimal("stopLossPrice").stripTrailingZeros().toPlainString()));
        // takeProfitPrice止盈终止价格
        Assert.assertEquals(new BigDecimal(strategyEntity_update.getTakeProfitPrice().stripTrailingZeros().toPlainString()),
                new BigDecimal(requestParams.getBigDecimal("takeProfitPrice").stripTrailingZeros().toPlainString()));
        // sellWhileTerminating终止时是否卖出币种
        Assert.assertEquals(strategyEntity_update.getShouldSellWhileTerminating() != 0,
                requestParams.getBoolean("sellWhileTerminating"));

        logger.info("********************pass********************");
    }

    @Test(dataProvider = "datas_fail", description = "修改现货网格订单异常场景")
    public void updateGridSpotOrder_Fail_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        String requestUrl = VariableReplace.replace(url, (String) context.getAttribute("orderId_manual"));
        JSONObject resp = httpClientUtil.sendPut(requestUrl, JSONObject.parseObject(params), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
    }
}
