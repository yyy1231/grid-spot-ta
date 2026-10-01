package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.*;
import com.novax.mapper.GridSpotOrderEntityMapper;
import com.novax.mapper.GridSpotStrategyEntityMapper;
import com.novax.mapper.GridSpotStrategyTemplateEntityMapper;
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
import java.math.RoundingMode;
import java.util.*;

/**
 * #创建现货网格
 * POST/v1/gridSpotOrders
 */
public class CreateGridTest extends PublicParams {
    Logger logger = Logger.getLogger(CreateGridTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyCreate();
    }

    @DataProvider(name = "datas_success")
    public Object[][] dataProvider1() throws IOException {
        return ExcelUtil.readObjDatas("/order/createGrid.xls", 0);
    }

    @DataProvider(name = "datas_fail")
    public Object[][] dataProvider2() throws IOException {
        return ExcelUtil.readObjDatas("/order/createGrid.xls", 1);
    }

    @Test(dataProvider = "datas_success", groups = {"smokeTest"}, description = "创建网格")
    public void createGridSpotOrder_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        Long userId = Long.valueOf(reqHeaders.get("userId"));
        JSONObject bodys = JSONObject.parseObject(params);
        String symbol = bodys.getString("symbol");
        int gridQuantity = bodys.getIntValue("gridQuantity");

        // 查询策略模板id
        if (tcName.equals("创建网格现货订单-AI策略")) {
            Long strategyTemplateId = Long.parseLong((String) context.getAttribute(symbol + "_30_name"));
            bodys.put("strategyTemplateId", strategyTemplateId);
        }

        // 获取最新价
        String value = RedisUtil.getJedis().hget("spot:new:price", symbol);
        BigDecimal newestPrice = JSONObject.parseObject(value).getBigDecimal("price").setScale(2, RoundingMode.HALF_UP);
        logger.info(tcNum + "-最新价：" + value);
        // 相关价格关联最新价
        if (tcName.equals("创建网格现货订单-设置上移上限-eth")) {
            bodys.put("upperLimitPrice", newestPrice.add(new BigDecimal("550")));
            bodys.put("lowerLimitPrice", newestPrice.subtract(new BigDecimal("450")));
        } else {
            bodys.put("upperLimitPrice", newestPrice.add(new BigDecimal("1600")).setScale(2, RoundingMode.HALF_UP));
            bodys.put("lowerLimitPrice", newestPrice.subtract(new BigDecimal("1400")).setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal upperLimitPrice = bodys.getBigDecimal("upperLimitPrice");
        BigDecimal lowerLimitPrice = bodys.getBigDecimal("lowerLimitPrice");

        if (tcDescp.equals("创建网格订单-设置上移上限-网格上限+2*网格价差+1")) {
            // 网格价差priceDif
            BigDecimal priceDif = upperLimitPrice.subtract(lowerLimitPrice).divide(BigDecimal.valueOf(bodys.getShort("gridQuantity")), 5, RoundingMode.HALF_UP);
            bodys.put("trailingUpLimit", upperLimitPrice.add(priceDif.multiply(new BigDecimal("2"))).add(BigDecimal.ONE));
            logger.info(tcNum + "-网格价差：" + priceDif);
            logger.info(tcNum + "-上移上限：" + bodys.getBigDecimal("trailingUpLimit"));
        }
        if (tcDescp.equals("创建网格订单-设置触发价-最新价加1000-手动触发")) {
            bodys.put("runningTriggerPrice", newestPrice.add(new BigDecimal("1000")));
        }
        if (tcDescp.equals("创建网格订单-设置触发价-最新价加800-自动触发")) {
            bodys.put("runningTriggerPrice", newestPrice.add(new BigDecimal("800")));
        }
        if (tcDescp.equals("创建网格订单-设置触发价-最新价减800-自动触发")) {
            bodys.put("runningTriggerPrice", newestPrice.subtract(new BigDecimal("800")));
        }
        if (tcDescp.equals("创建网格订单-设置止损价-min(最新价,网格下限)减200")) {
            bodys.put("stopLossPrice", newestPrice.min(bodys.getBigDecimal("lowerLimitPrice")).subtract(new BigDecimal("200")));
        }
        if (tcDescp.equals("创建网格订单-设置止盈价-网格上限加100")) {
            bodys.put("takeProfitPrice", upperLimitPrice.add(new BigDecimal("100")));
        }
        if (tcDescp.equals("创建网格订单-设置止盈价-上移上限加1")) {
            bodys.put("trailingUpLimit", upperLimitPrice.add(new BigDecimal("500")));
            bodys.put("takeProfitPrice", bodys.getBigDecimal("trailingUpLimit").add(BigDecimal.ONE));
        }

        JSONObject resp = httpClientUtil.sendPostByJson(url, bodys, reqHeaders);

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + bodys);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
        Thread.sleep(3000);

        // orderId写入context
        if (tcName.equals("创建网格现货订单-AI策略")) {
            context.setAttribute("orderId_AI", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-manual")) {
            context.setAttribute("orderId_manual", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-等比")) {
            context.setAttribute("orderId_geometrical", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-设置上移上限-eth")) {
            context.setAttribute("orderId_trailingUp", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-设置触发价-手动触发")) {
            context.setAttribute("orderId_startUp", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-设置触发价-up")) {
            context.setAttribute("orderId_triggerUp", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-设置触发价-down")) {
            context.setAttribute("orderId_triggerDown", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-设置止损价")) {
            context.setAttribute("orderId_stopLoss", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-设置止盈价-网格上限加100")) {
            context.setAttribute("orderId_takeProfit_priceUp", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-设置止盈价-上移上限加1")) {
            context.setAttribute("orderId_takeProfit_trailingUp", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-终止时是否卖出-是")) {
            context.setAttribute("orderId_sellTrue", resp.getJSONObject("data").get("id"));
        } else if (tcName.equals("创建网格现货订单-终止时是否卖出-否")) {
            context.setAttribute("orderId_sellFalse", resp.getJSONObject("data").get("id"));
        } else {
            context.setAttribute("orderId" + new Random().nextInt(100), resp.getJSONObject("data").get("id"));
        }

        // 数据库断言superex.grid_spot_strategy，superex.grid_spot_order落表
        Long gridSpotOrderId = resp.getJSONObject("data").getLong("id");
        GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(gridSpotOrderId));
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(gridSpotOrderId));

        if (tcName.equals("创建网格现货订单-AI策略")) {
            Long strategyTemplateId = Long.parseLong((String) context.getAttribute(symbol + "_30_name"));
            GridSpotStrategyTemplateEntity strategyTemplateEntity = MyBatisUtil.execute(GridSpotStrategyTemplateEntityMapper.class, m -> m.selectByPrimaryKey(strategyTemplateId));
            Assert.assertEquals(strategyEntity.getGridQuantity(), strategyTemplateEntity.getGridQuantity());
            Assert.assertEquals(strategyEntity.getLowerLimitPrice(), strategyTemplateEntity.getLowerLimitPrice(), "");
            Assert.assertEquals(strategyEntity.getUpperLimitPrice(), strategyTemplateEntity.getUpperLimitPrice());
        } else {
            Assert.assertEquals(strategyEntity.getGridQuantity(), bodys.getShort("gridQuantity"));
            Assert.assertEquals(new BigDecimal(strategyEntity.getLowerLimitPrice().stripTrailingZeros().toPlainString()), new BigDecimal(bodys.getBigDecimal("lowerLimitPrice").stripTrailingZeros().toPlainString()));
            Assert.assertEquals(new BigDecimal(strategyEntity.getUpperLimitPrice().stripTrailingZeros().toPlainString()), new BigDecimal(bodys.getBigDecimal("upperLimitPrice").stripTrailingZeros().toPlainString()));
        }

        Assert.assertEquals(orderEntity.getSymbol(), symbol);
        Assert.assertEquals(orderEntity.getCurrency(), bodys.getString("investmentCurrency"));
        Assert.assertEquals(new BigDecimal(orderEntity.getInvestment().stripTrailingZeros().toPlainString()), bodys.getBigDecimal("investmentAmount"));

        // 财务划转
        BigDecimal investmentAmount = bodys.getBigDecimal("investmentAmount");
        List<GridSpotFinanceRecordEntity> gridSpotFinanceRecordEntities = UserAssetsManage.queryFinanceRecords(userId, gridSpotOrderId, 7, null);
        Assert.assertTrue(gridSpotFinanceRecordEntities.size() != 0, "用户" + userId + "网格创建：" + gridSpotOrderId + "资产划转异常");
        BigDecimal amount = gridSpotFinanceRecordEntities.get(0).getAmount();
        Assert.assertEquals(amount.compareTo(investmentAmount), 0, "用户" + userId + "网格创建：" + gridSpotOrderId + "资产划转金额有误，测试：" + amount + "，预期：" + investmentAmount);

        // 网格账户资产
        if (tcDescp.contains("设置触发价")) {
            List<GridSpotUserAssetsEntity> assets = UserAssetsManage.queryGridAssets(userId, gridSpotOrderId, "usdt");
            Assert.assertEquals(assets.size(), 1, "网格创建：" + gridSpotOrderId + "资产账户异常；待触发的网格，预期存在计价货币U一个策略账户");
            BigDecimal available = assets.get(0).getAvailable();
            Assert.assertEquals(available.compareTo(investmentAmount), 0, "用户" + userId + "网格创建：" + gridSpotOrderId + "资产划转异常");
        } else {
            List<GridSpotUserAssetsEntity> assets = UserAssetsManage.queryGridAssets(userId, gridSpotOrderId, null);
            Assert.assertEquals(assets.size(), 2, "用户" + userId + "网格创建：" + gridSpotOrderId + "资产账户异常，或断言早了，手动检查下；预期存在交易、计价货币U两种资产的策略账户");
        }

        // 断言订单状态
        Thread.sleep(1000);
        if (tcDescp.equals("创建网格订单-设置触发价-最新价加1000-手动触发") || tcDescp.equals("创建网格订单-设置触发价-最新价加800-自动触发") || tcDescp.equals("创建网格订单-设置触发价-最新价减800-自动触发")) {
            Assert.assertEquals((byte) orderEntity.getStatus(), 0, "用户" + userId + "网格" + gridSpotOrderId + "状态异常(非0，已触发)，或由于行情波动较大，导致主动推动行情前已触发");
        } else {
            Assert.assertTrue(orderEntity.getStatus() == 1 || orderEntity.getStatus() == 2, "用户" + userId + "网格" + gridSpotOrderId + "或资金划转（现货》策略账户）或初始化建仓、委托未完成或异常终止，导致订单创建后自启动失败状态异常");
        }

        // 断言初始建仓(初始买入&初始委托)
        if (!tcName.contains("设置触发价") && !tcName.contains("等比") && !tcName.contains("AI策略")) {
            int initialPositionsNum = gridQuantity; // 初始建仓数量
            // 网格价差
            BigDecimal priceDif = upperLimitPrice.subtract(lowerLimitPrice).divide(BigDecimal.valueOf(gridQuantity), 5, RoundingMode.HALF_UP);
            logger.info("网格价差：" + priceDif);
            // 每个网格的价格
            ArrayList<BigDecimal> pricePerGrid = new ArrayList<>();
            for (int i = 0; i < gridQuantity + 1; i++) {
                BigDecimal price = lowerLimitPrice.add(priceDif.multiply(BigDecimal.valueOf(i)));
                pricePerGrid.add(price);
                if (price.compareTo(newestPrice) < 0) {
                    initialPositionsNum -= 1;
                }
            }
            // 单网格买入量
            BigDecimal sumPrice = new BigDecimal("0.00000");
            for (int i = 0; i < gridQuantity; i++) {
                sumPrice = sumPrice.add(pricePerGrid.get(i));
            }
            BigDecimal amountPerGrid = investmentAmount.divide(sumPrice, 5, RoundingMode.FLOOR);
            logger.info("单网格买入量：" + amountPerGrid);

            // 初始建仓
            Thread.sleep(3000);
            List<SpotOrderEntity> orderEntityList = GridStatistics.queryInitialPositionsNum(userId, gridSpotOrderId);
            int size = orderEntityList.size();
            Assert.assertEquals(size, initialPositionsNum,
                    "用户" + userId + "网格" + gridSpotOrderId + "初始买入数量异常，测试：" + size + "，预期：" + initialPositionsNum + "；或价格波动较大，获取的最新价较后端差距较大，导致计算出的初始减仓数量不一致，手动检查下");
            BigDecimal fee = Optional.ofNullable(GridStatistics.queryOrderFee(orderEntityList.get(0).getId())).orElse(BigDecimal.ZERO);
            logger.info("现货订单：" + orderEntityList.get(0).getId() + "交易手续费：" + fee);

            // 单网格卖出量
            BigDecimal amountPerGridOfSell = amountPerGrid.subtract(fee).setScale(4, RoundingMode.FLOOR);

            // 初始委托
            int sellNum = 0;
            int buyNum = 0;
            List<SpotOrderEntity> orderEntityOfSell = null;
            List<SpotOrderEntity> orderEntityOfBuy = null;
            for (int i = 0; i < 12; i++) {
                orderEntityOfSell = GridStatistics.queryInitialEntrust(userId, gridSpotOrderId, 2);
                orderEntityOfBuy = GridStatistics.queryInitialEntrust(userId, gridSpotOrderId, 1);
                sellNum = orderEntityOfSell.size();
                buyNum = orderEntityOfBuy.size();
                if (sellNum + buyNum == gridQuantity) break;
                Thread.sleep(5000);
            }

            int buyNumExpect = gridQuantity - initialPositionsNum;
            Assert.assertEquals(sellNum, initialPositionsNum, "用户" + userId + "网格" + gridSpotOrderId + "初始委托卖数量异常，测试：" + sellNum + "，预期：" + initialPositionsNum + "；或断言早了，手动检查下");
            Assert.assertEquals(buyNum, buyNumExpect, "用户" + userId + "网格" + gridSpotOrderId + "初始委托买数量异常，测试：" + sellNum + "，预期：" + buyNumExpect);
            for (int i = 0; i < buyNum; i++) {
                SpotOrderEntity entity = orderEntityOfBuy.get(i);
                Long orderId = entity.getId();
                BigDecimal price = entity.getPrice();
                BigDecimal orderNumber = entity.getOrderNumber();
                BigDecimal gridPrice = pricePerGrid.get(i);
                Assert.assertEquals(price.compareTo(gridPrice), 0,
                        "用户" + userId + "网格" + gridSpotOrderId + "初始委托买单" + orderId + "委托价异常，测试：" + price + "，预期：" + gridPrice);
                Assert.assertTrue(orderNumber.subtract(amountPerGrid).abs().compareTo(amountPerGrid) < 0,
                        "用户" + userId + "网格" + gridSpotOrderId + "初始委托买单" + orderId + "委托量异常，测试：" + orderNumber + "，预期：" + amountPerGrid);
            }
            for (int i = 0; i < sellNum; i++) {
                SpotOrderEntity entity = orderEntityOfSell.get(i);
                Long orderId = entity.getId();
                BigDecimal price = entity.getPrice();
                BigDecimal orderNumber = entity.getOrderNumber();
                BigDecimal gridPrice = pricePerGrid.get(buyNum + 1 + i);
                Assert.assertEquals(price.compareTo(gridPrice), 0,
                        "用户" + userId + "网格" + gridSpotOrderId + "初始委托卖单" + orderId + "委托价异常，测试：" + price + "，预期：" + gridPrice);
                Assert.assertTrue(orderNumber.subtract(amountPerGridOfSell).abs().compareTo(BigDecimal.valueOf(0.0001)) <= 0,
                        "用户" + userId + "网格" + gridSpotOrderId + "初始委托卖单" + orderId + "委托量异常，测试：" + orderNumber + "，预期：" + amountPerGridOfSell);
            }
        }

        logger.info("********************pass********************");

        for (String attributeName : context.getAttributeNames()) {
            System.out.println(attributeName + ":" + context.getAttribute(attributeName));
        }
    }

    @Test(dataProvider = "datas_fail", description = "创建网格异常场景")
    public void createGridSpotOrder_Fail_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        JSONObject resp = httpClientUtil.sendPostByJson(url, JSONObject.parseObject(params), reqHeaders);

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
    }

    @Test(groups = {"smokeTest"}, description = "全部网格")
    public void getAttributeNames_all_Test(ITestContext context) {
        Set<String> attributeNames = context.getAttributeNames();
        for (String attributeName : attributeNames) {
            if (attributeName.contains("orderId")) {
                allAttributeNameList.add(attributeName);
            }
        }
        logger.info("allAttributeNameList:" + allAttributeNameList);
    }

    @Test(groups = {"smokeTest"}, description = "自启动网格")
    public void getAttributeNames_autoStartUp_Test(ITestContext context) {
        Set<String> attributeNames = context.getAttributeNames();
        for (String attributeName : attributeNames) {
            if (attributeName.contains("orderId") && !attributeName.equals("orderId_triggerDown") && !attributeName.equals("orderId_triggerUp") && !attributeName.equals("orderId_startUp")) {
                autoStartUpAttributeNameList.add(attributeName);
            }
        }
        logger.info("autoStartUpAttributeNameList:" + autoStartUpAttributeNameList);
    }

    @Test(groups = {"smokeTest"}, description = "需手动终止的网格，清理数据")
    public void getAttributeNames_clearData_Test(ITestContext context) {
        Set<String> attributeNames = context.getAttributeNames();
        for (String attributeName : attributeNames) {
            if (attributeName.contains("orderId")
                    // && !attributeName.equals("orderId_stopLoss")
                    // && !attributeName.equals("orderId_takeProfit_priceUp")
                    // && !attributeName.equals("orderId_takeProfit_trailingUp")
                    && !attributeName.equals("orderId_sellFalse") && !attributeName.equals("orderId_sellTrue")) {
                clearDataAttributeNameList.add(attributeName);
            }
        }
        logger.info("clearDataAttributeNameList:" + clearDataAttributeNameList);
    }
}
