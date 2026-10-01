package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.*;
import com.novax.mapper.GridSpotOrderEntityMapper;
import com.novax.mapper.GridSpotStrategyEntityMapper;
import com.novax.testcase.*;
import com.novax.util.*;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 终止现货网格订单
 * POST/v1/gridSpotOrders/{orderId}/terminate
 */
public class TerminateGridTest extends PublicParams {
    Logger logger = Logger.getLogger(TerminateGridTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyTerminate();
    }

    @DataProvider(name = "sell")
    public Object[][] dataProvider2() throws IOException {
        return ExcelUtil.readObjDatas("/order/terminateGrid.xls", 0);
    }

    @DataProvider(name = "notSell")
    public Object[][] dataProvider1() throws IOException {
        return ExcelUtil.readObjDatas("/order/terminateGrid.xls", 1);
    }

    @DataProvider(name = "clearData")
    public Object[][] dataProvider3() throws IOException {
        return ExcelUtil.readObjDatas("/order/terminateGrid.xls", 2);
    }


    @Test(groups = {"smokeTest"}, dataProvider = "notSell", description = "终止网格，手动终止，终止时不卖出")
    public void a_terminate_NotSell_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        Long userId = Long.valueOf(reqHeaders.get("userId"));
        long orderId = Long.parseLong((String) context.getAttribute("orderId_sellFalse"));
        // long orderId = 524985224116957184L;

        // 网格交易中委托单
        List<SpotOrderEntity> orderEntityList = GridStatistics.queryInTradingGridOrders(null, orderId, null);
        Assert.assertTrue(orderEntityList.size() != 0, "网格" + orderId + "运行异常：无交易中委托单");

        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, JSONObject.parseObject(params), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
        Thread.sleep(60000);

        // 断言委托单已撤销
        List<SpotOrderEntity> orderEntities = GridStatistics.queryInTradingGridOrders(null, orderId, null);
        Assert.assertEquals(orderEntities.size(), 0, "网格" + orderId + "终止异常异常，委托单委全部撤销；或断言早了，手动检查下");

        // 断言网格状态
        GridSpotOrderEntity grid = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        Assert.assertTrue(grid.getStatus() == 3 || grid.getStatus() == 4, "网格：" + orderId + "网格终止异常");

        // 断言网格事件 GridSpotTerminate
        int times = GridStatistics.searchGridEvent(orderId, "GridSpotTerminate");
        Assert.assertTrue(times != 0, "网格：" + orderId + "网格终止异常，未查询到终止事件");

        // 断言资产不卖掉 即终止时 交易货币不卖出 则会存在交易货币从策略账户划转至现货账户的记录
        Thread.sleep(2000);
        logger.info("网格" + orderId + ",脚本执行时间(查询网格终止资产划转记录):" + new Date());
        List<GridSpotFinanceRecordEntity> records = UserAssetsManage.queryFinanceRecords(userId, orderId, 8, "btc");
        logger.info("网格" + orderId + ",脚本执行时间(查询网格终止资产划转记录):" + new Date());
        Assert.assertTrue(records.size() != 0, "网格：" + orderId + "终止时资产划转异常，预期存在交易币的划转记录，或断言早了，手动检查下；该网格高级参数设置的是终止时不卖出，故终止时会存在交易币从网格账户到现货账户的划转");

        logger.info("********************pass********************");
    }

    @Test(groups = {"smokeTest"}, dataProvider = "sell", description = "终止网格，手动终止，终止时卖出")
    public void b_terminate_Sell_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        Long userId = Long.valueOf(reqHeaders.get("userId"));
        long orderId = Long.parseLong((String) context.getAttribute("orderId_sellTrue"));
        // long orderId = 524977641155063808L;

        // 网格交易中委托单
        List<SpotOrderEntity> orderEntityList = GridStatistics.queryInTradingGridOrders(null, orderId, null);
        Assert.assertTrue(orderEntityList.size() != 0, "网格" + orderId + "运行异常：无交易中委托单");

        // 网格资产
        List<GridSpotUserAssetsEntity> gridSpotUserAssetsEntities = UserAssetsManage.queryGridAssets(userId, orderId, null);
        Map<String, BigDecimal> gridAssets = AssetConvert.convert(gridSpotUserAssetsEntities);
        BigDecimal gridAssetsOfU = gridAssets.get("usdt");

        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, JSONObject.parseObject(params), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 断言委托单已撤销
        List<SpotOrderEntity> orderEntities = null;
        for (int i = 0; i < 12; i++) {
            orderEntities = GridStatistics.queryInTradingGridOrders(null, orderId, null);
            int size = orderEntities.size();
            if (size == 0) break;
            Thread.sleep(5000);
        }
        Assert.assertEquals(orderEntities.size(), 0, "网格" + orderId + "终止异常，委托单未全部撤销；或断言早了，手动检查下");

        // 断言划转金额
        logger.info("网格" + orderId + ",脚本执行时间(查询网格终止资产划转记录):" + new Date());
        List<GridSpotFinanceRecordEntity> record = UserAssetsManage.queryFinanceRecords(userId, orderId, 8, "usdt");
        logger.info("网格" + orderId + ",脚本执行时间(查询网格终止资产划转记录):" + new Date());
        Assert.assertTrue(record.size() != 0, "网格" + orderId + "终止，未查询到从策略账户往现货账户的划转记录；或断言早了，手动检查下");
        BigDecimal amount = record.get(0).getAmount();
        Assert.assertTrue(amount.subtract(gridAssetsOfU).abs().compareTo(BigDecimal.valueOf(1)) <= 0, "网格" + orderId + "终止划转金额计算不一致，测试：" + amount + "，预期：" + gridAssetsOfU);

        // 断言网格状态
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        Assert.assertTrue(orderEntity.getStatus() == 3 || orderEntity.getStatus() == 4, "网格：" + orderId + "网格终止异常");

        // 断言网格事件 GridSpotTerminate
        int times = GridStatistics.searchGridEvent(orderId, "GridSpotTerminate");
        Assert.assertTrue(times != 0, "网格终止异常，未查询到终止事件");

        logger.info("********************pass********************");

    }

    @Test(groups = {"smokeTest"}, dataProvider = "clearData", description = "网格终止，其它订单，清理数据")
    public void terminate_ClearData_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        // int index = Integer.parseInt(tcNum.substring(tcNum.length() - 1));
        int index = Integer.parseInt(tcNum.replaceAll(".*[^\\d](?=(\\d+))", ""));
        String attributeName = clearDataAttributeNameList.get(index - 1);
        Long orderId = Long.parseLong((String) context.getAttribute(attributeName));
        // Long orderId = 518460522531778560L;

        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));

        // 查看执行终止操作时订单状态
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        Byte status = orderEntity.getStatus();
        Assert.assertTrue(status == 0 || status == 1 || status == 2, "网格：" + orderId + "，状态" + status + "已终止或终止中");

        JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, JSONObject.parseObject(params), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 断言订单状态，是否已终止
        Thread.sleep(3000);
        GridSpotOrderEntity orderEntity1 = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        Assert.assertTrue(orderEntity1.getStatus() == 3 || orderEntity1.getStatus() == 4, "网格：" + orderId + "网格终止异常");

        // 断言网格事件 GridSpotTerminate
        int times = GridStatistics.searchGridEvent(orderId, "GridSpotTerminate");
        Assert.assertTrue(times != 0, "网格：" + orderId + "网格终止异常，未查询到GridSpotTerminate事件；或断言早了，手动检查下");

        logger.info("********************pass********************");
    }

    // 清理未终止的网格
    @Test
    public void terminateGrid() throws Exception {
        Long userId = 28678286L;
        // 查运行中的网格
        List<GridSpotOrderEntity> grids = GridStatistics.queryGridsOnRunning(userId);
        JSONObject reqParams = new JSONObject();
        reqParams.put("sellWhileTerminating", true);

        for (GridSpotOrderEntity grid : grids) {
            Long orderId = grid.getId();
            Byte status = grid.getStatus();
            String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
            JSONObject resp = httpClientUtil.sendPostByJson(requestUrl, reqParams, reqHeaders);

            logger.info("-url:" + requestUrl);
            logger.info("-响应数据:" + resp);

            logger.info("orderId:" + orderId + ":" + resp.get("msg"));
            Assert.assertEquals(resp.get("code"), 200);
        }

    }

    // @Test(groups = {"smokeTest"}, description = "终止网格，止损终止", enabled = false)
    public void terminate_stopLossPrice_Test(ITestContext context) throws IOException, InterruptedException {
        Long orderId = Long.parseLong((String) context.getAttribute("orderId_stopLoss"));
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        String symbol = orderEntity.getSymbol();

        // 获取订单止损价格
        GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        BigDecimal stopLossPrice = strategyEntity.getStopLossPrice();

        // 调用工具，推动行情到止损价格
        AdjustMarketPrice2.adjustPrice(symbol, stopLossPrice, 1);

        // 断言订单状态，是否已终止
        Thread.sleep(20000);
        GridSpotOrderEntity orderEntity1 = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        Assert.assertTrue(orderEntity1.getStatus() == 3 || orderEntity1.getStatus() == 4, "网格：" + orderId + "止损终止异常；或行情未推动到止损价格；或等待时间不够断言早了，手动检查下");

        // 断言网格事件 GridSpotTerminateByStopLoss
        int times = GridStatistics.searchGridEvent(orderId, "GridSpotTerminateByStopLoss");
        Assert.assertTrue(times != 0, "网格：" + orderId + "止损终止异常，未查询到GridSpotTerminateByStopLoss事件");

        logger.info("********************pass********************");
    }

    // @Test(groups = {"smokeTest"}, description = "终止网格，止盈终止，大于网格上限", enabled = false)
    public void terminate_takeProfit_priceUp_Test(ITestContext context) throws IOException, InterruptedException {
        Long orderId = Long.parseLong((String) context.getAttribute("orderId_takeProfit_priceUp"));
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        String symbol = orderEntity.getSymbol();

        // 获取订单止盈价格
        GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        BigDecimal takeProfitPrice = strategyEntity.getTakeProfitPrice();

        // 调用工具，推动行情到止盈价格
        AdjustMarketPrice2.adjustPrice(symbol, takeProfitPrice, 1);
        // 断言订单状态，是否已终止
        Thread.sleep(20000);
        GridSpotOrderEntity orderEntity1 = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        Assert.assertTrue(orderEntity1.getStatus() == 3 || orderEntity1.getStatus() == 4, "网格：" + orderId + "止盈终止异常；或行情未推动到止损价格；或等待时间不够断言早了，手动检查下");

        // 断言网格事件 GridSpotTerminateByTakeProfit
        int times = GridStatistics.searchGridEvent(orderId, "GridSpotTerminateByTakeProfit");
        Assert.assertTrue(times != 0, "网格：" + orderId + "止盈终止异常，未查询到GridSpotTerminateByTakeProfit事件");

        logger.info("********************pass********************");
    }

    // @Test(groups = {"smokeTest"}, description = "终止网格，止盈终止，大于上移上限", enabled = false)
    public void terminate_takeProfit_trailingUp_Test(ITestContext context) throws IOException, InterruptedException {
        Long orderId = Long.parseLong((String) context.getAttribute("orderId_takeProfit_trailingUp"));
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        String symbol = orderEntity.getSymbol();

        // 获取订单止盈价格
        GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        BigDecimal takeProfitPrice = strategyEntity.getTakeProfitPrice();

        // 调用工具，推动行情到止盈价格
        AdjustMarketPrice2.adjustPrice(symbol, takeProfitPrice, 1);
        // 断言订单状态，是否已终止
        Thread.sleep(20000);
        GridSpotOrderEntity orderEntity1 = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        Assert.assertTrue(orderEntity1.getStatus() == 3 || orderEntity1.getStatus() == 4, "网格：" + orderId + "止盈终止异常；或行情未推动到止损价格；或等待时间不够断言早了，手动检查下");

        // 断言网格事件 GridSpotTerminateByTakeProfit
        int times = GridStatistics.searchGridEvent(orderId, "GridSpotTerminateByTakeProfit");
        Assert.assertTrue(times != 0, "网格：" + orderId + "止盈终止异常，未查询到GridSpotTerminateByTakeProfit事件");

        logger.info("********************pass********************");
    }
}

