package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.enums.GridTypeEnum;
import com.novax.entity.mysql.GridSpotOrderEntity;
import com.novax.entity.mysql.GridSpotStrategyEntity;
import com.novax.entity.mysql.GridSpotUserAssetsEntity;
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
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 查询现货网格订单详情
 * GET/v1/gridSpotOrders/{orderId}
 */
public class QueryGridDetailTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridDetailTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyDetail();
    }

    @DataProvider(name = "datas_success")
    public Object[][] getProvider1() throws IOException {
        return ExcelUtil.readObjDatas("/order/queryGridDetail.xls", 0);
    }


    @Test(dataProvider = "datas_success", groups = {"smokeTest"}, description = "查询网格详情")
    public void queryGridSpotOrderDetail_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        // int index = Integer.parseInt(tcNum.substring(tcNum.length() - 1));
        int index = Integer.parseInt(tcNum.replaceAll(".*[^\\d](?=(\\d+))", ""));
        String attributeName = allAttributeNameList.get(index - 1);
        Long orderId = Long.parseLong((String) context.getAttribute(attributeName));
        // Long orderId = 525983711646314496L;

        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
        JSONObject resp = httpClientUtil.sendGet(requestUrl, new HashMap<>(), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        JSONObject data = resp.getJSONObject("data");
        JSONObject strategyAdvancedOptions = data.getJSONObject("strategyAdvancedOptions");
        JSONObject profitDetail = data.getJSONObject("profit");

        GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));

        if (!tcDescp.contains("aisi")) {
            // 断言网格策略基础参数
            // 1、upperLimitPrice网格上限价格
            BigDecimal upperLimitPrice = new BigDecimal(data.getBigDecimal("upperLimitPrice").stripTrailingZeros().toPlainString());
            // 2、lowerLimitPrice网格下限价格
            BigDecimal lowerLimitPrice = new BigDecimal(data.getBigDecimal("lowerLimitPrice").stripTrailingZeros().toPlainString());
            // 3、gridQuantity网格数量
            int gridQuantity = data.getInteger("gridQuantity");
            // 4、gridType网格类型
            String gridType = data.getString("gridType");
            // 5、maxProfitRatePerGrid单网格最高收益率
            BigDecimal maxProfitRatePerGrid = data.getBigDecimal("maxProfitRatePerGrid");
            // 6、minProfitRatePerGrid单网格最低收益率
            BigDecimal minProfitRatePerGrid = data.getBigDecimal("minProfitRatePerGrid");
            // 7、amountPerGrid单网格买入量
            BigDecimal amountPerGrid = data.getBigDecimal("amountPerGrid");

            BigDecimal upperLimitPriceExpect = new BigDecimal(strategyEntity.getUpperLimitPrice().stripTrailingZeros().toPlainString());
            BigDecimal lowerLimitPriceExpect = new BigDecimal(strategyEntity.getLowerLimitPrice().stripTrailingZeros().toPlainString());
            int gridQuantityExpect = strategyEntity.getGridQuantity();
            String gridTypeExpect = GridTypeEnum.getDescByType(strategyEntity.getGridType());
            Map<String, BigDecimal> result = GridStrategyAnalysis.calculate(strategyEntity.getUpperLimitPrice(), strategyEntity.getLowerLimitPrice(), gridQuantityExpect, orderEntity.getSymbol(), orderEntity.getInvestment(), gridTypeExpect);
            BigDecimal maxProfitRatePerGridExpect = result.get("maxProfitRatePerGrid");
            BigDecimal minProfitRatePerGridExpect = result.get("minProfitRatePerGrid");
            BigDecimal amountPerGridExpect = result.get("amountPerGrid");

            Assert.assertEquals(upperLimitPrice, upperLimitPriceExpect,
                    "网格" + orderId + "上限价格查询不一致，测试：" +upperLimitPrice + "，预期：" + upperLimitPriceExpect);
            Assert.assertEquals(lowerLimitPrice, lowerLimitPriceExpect,
                    "网格" + orderId + "下限价格询不一致，测试：" +lowerLimitPrice + "，预期：" + lowerLimitPriceExpect);
            Assert.assertEquals(gridQuantity, gridQuantityExpect,
                    "网格" + orderId + "网格数量查询不一致，测试：" +gridQuantity + "，预期：" + gridQuantityExpect);
            Assert.assertEquals(gridType, gridTypeExpect,
                    "网格" + orderId + "网格类型查询不一致，测试：" +gridType + "，预期：" + gridTypeExpect);
            Assert.assertTrue(maxProfitRatePerGrid.subtract(maxProfitRatePerGridExpect).abs().compareTo(BigDecimal.valueOf(0.1)) <= 0,
                    "网格" + orderId + "单网格最高收益率查询不一致，测试：" +maxProfitRatePerGrid + "，预期：" + maxProfitRatePerGridExpect);
            Assert.assertTrue(minProfitRatePerGrid.subtract(minProfitRatePerGridExpect).abs().compareTo(BigDecimal.valueOf(0.1)) <= 0,
                    "网格" + orderId + "单网格最低收益率查询不一致，测试：" +minProfitRatePerGrid + "，预期：" + minProfitRatePerGridExpect);
            Assert.assertTrue(amountPerGrid.subtract(amountPerGridExpect).abs().compareTo(BigDecimal.valueOf(0.001)) <= 0,
                    "网格" + orderId + "单网格买入量查询不一致，测试：" +amountPerGrid + "，预期：" + amountPerGridExpect);

            // 断言网格高级参数
            // 1、trailingUpEnabled网格是否开启上移
            Boolean trailingUpEnabled = strategyAdvancedOptions.getBoolean("trailingUpEnabled");
            // 2、trailingUpLimit网格上移上限
            BigDecimal trailingUpLimit = strategyAdvancedOptions.getBigDecimal("trailingUpLimit");
            // 3、runningTriggerPrice启动触发价格
            BigDecimal triggerPrice = strategyAdvancedOptions.getBigDecimal("runningTriggerPrice");
            // 4、stopLossPrice止损终止价格
            BigDecimal stopLossPrice = strategyAdvancedOptions.getBigDecimal("stopLossPrice");
            // 5、takeProfitPrice止盈终止价格
            BigDecimal takeProfitPrice = strategyAdvancedOptions.getBigDecimal("takeProfitPrice");
            // 6、终止时是否卖出基础币
            Boolean sellWhileTerminating = strategyAdvancedOptions.getBoolean("sellWhileTerminating");

            Boolean trailingUpEnabledExpect = strategyEntity.getTrailingUpEnabled() != 0;
            BigDecimal trailingUpLimitExpect = strategyEntity.getTrailingUpLimit();
            BigDecimal triggerPriceExpect = strategyEntity.getRunningTriggerPrice();
            BigDecimal stopLossPriceExpect = strategyEntity.getStopLossPrice();
            BigDecimal takeProfitPriceExpect = strategyEntity.getTakeProfitPrice();
            Boolean sellWhileTerminatingExpect = strategyEntity.getShouldSellWhileTerminating() != 0;

            Assert.assertEquals(trailingUpEnabled, trailingUpEnabledExpect,
                    "网格" + orderId + "是否开启上移查询不一致，测试：" +trailingUpEnabled + "，预期：" + trailingUpEnabledExpect);
            Assert.assertTrue(Objects.equals(trailingUpLimit, trailingUpLimitExpect) || trailingUpLimit.compareTo(trailingUpLimitExpect) == 0,
                    "网格" + orderId + "上移上限查询不一致，测试：" +trailingUpLimit + "，预期：" + trailingUpLimitExpect);
            Assert.assertTrue(Objects.equals(triggerPrice, triggerPriceExpect) || triggerPrice.compareTo(triggerPriceExpect) == 0,
                    "网格" + orderId + "启动触发价格查询不一致，测试：" +triggerPrice + "，预期：" + triggerPriceExpect);
            Assert.assertTrue(Objects.equals(stopLossPrice, stopLossPriceExpect) || stopLossPrice.compareTo(stopLossPriceExpect) == 0,
                    "网格" + orderId + "止损终止价格查询不一致，测试：" +stopLossPrice + "，预期：" + stopLossPriceExpect);
            Assert.assertTrue(Objects.equals(takeProfitPrice, takeProfitPriceExpect) || takeProfitPrice.compareTo(takeProfitPriceExpect) == 0,
                    "网格" + orderId + "止盈终止价格查询不一致，测试：" +takeProfitPrice + "，预期：" + takeProfitPriceExpect);
            Assert.assertEquals(sellWhileTerminating, sellWhileTerminatingExpect,
                    "网格" + orderId + "终止时是否卖出基础币查询不一致，测试：" +sellWhileTerminating + "，预期：" + sellWhileTerminatingExpect);

            // 断言订单状态
            if (attributeName.equals("orderId_triggerUp") || attributeName.equals("orderId_triggerDown")) {
                Assert.assertEquals(resp.getJSONObject("data").getString("status"), "ready", "网格：" + orderId + "状态异常，待触发订单，或由于行情波动较大，提前触发（推动行情前），导致状态检查不通过");
            } else {
                Assert.assertTrue(resp.getJSONObject("data").getString("status").equals("initializing") || resp.getJSONObject("data").getString("status").equals("running"), "网格：" + orderId + "状态异常，自启动订单，或由于初始化建仓、委托未完成，或异常终止，导致状态检查不通过");
            }
        }

        // 断言收益详情
        // 网格收益
        BigDecimal gridProfit = profitDetail.getBigDecimal("gridProfit");
        // 网格收益率
        BigDecimal gridProfitRatio = profitDetail.getBigDecimal("gridProfitRatio");
        // 浮动收益
        BigDecimal floatProfit = profitDetail.getBigDecimal("floatProfit");
        // 浮动收益率
        BigDecimal floatProfitRatio = profitDetail.getBigDecimal("floatProfitRatio");
        // 总收益
        BigDecimal totalPnL = profitDetail.getBigDecimal("totalPnL");
        // 总收益率
        BigDecimal pnlRatio = profitDetail.getBigDecimal("pnlRatio");
        // 年化收益率
        BigDecimal APR = profitDetail.getBigDecimal("APR");

        Long userId = orderEntity.getUserId();
        BigDecimal investment = orderEntity.getInvestment();
        // double runningTime = Double.parseDouble(resp.getJSONObject("data").getString("runningTime")) / 1000 / 60;
        double runningTime = Math.ceil((System.currentTimeMillis() - orderEntity.getCreateTime().getTime()) / ((float) 60 * 1000));
        logger.info("网格" + orderId + "，运行时长，当前系统时间：" + System.currentTimeMillis());
        logger.info("网格" + orderId + "，运行时长，创建时间：" + orderEntity.getCreateTime().getTime());

        BigDecimal gridProfitExpect = GridStatistics.queryGridArbitrageAmount(orderId);
        BigDecimal gridProfitRatioExpect = gridProfitExpect.multiply(BigDecimal.valueOf(100)).divide(investment, 2, RoundingMode.HALF_UP);
        List<GridSpotUserAssetsEntity> gridSpotUserAssetsEntities = UserAssetsManage.queryGridAssets(userId, orderId, null);
        BigDecimal gridAssets = AssetConvert.convert(gridSpotUserAssetsEntities).get("usdt");
        BigDecimal totalPnLExpect = gridAssets.subtract(investment);
        BigDecimal pnlRatioExpect = totalPnLExpect.multiply(BigDecimal.valueOf(100)).divide(investment, 2, RoundingMode.HALF_UP);
        BigDecimal floatProfitExpect = totalPnLExpect.subtract(gridProfitExpect);
        BigDecimal floatProfitRatioExpect = floatProfitExpect.multiply(BigDecimal.valueOf(100)).divide(investment, 2, RoundingMode.HALF_UP);
        // 年化收益率：当前总收益/时长（分）*365*24*60
        BigDecimal APRExpect = pnlRatioExpect.multiply(BigDecimal.valueOf(365 * 24 * 60).setScale(20, RoundingMode.HALF_UP)).divide(BigDecimal.valueOf(runningTime), 20, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        Assert.assertTrue(gridProfit.subtract(gridProfitExpect).abs().compareTo(BigDecimal.valueOf(0.001)) <= 0,
                "网格" + orderId + "套利计算不一致，测试：" + gridProfit + "，预期：" + gridProfitExpect);
        Assert.assertTrue(gridProfitRatio.subtract(gridProfitRatioExpect).abs().compareTo(BigDecimal.valueOf(0.01)) <= 0,
                "网格" + orderId + "收益率计算不一致，测试：" + gridProfitRatio + "，预期：" + gridProfitRatioExpect);
        if (!tcDescp.contains("aisi")) {
            Assert.assertTrue(floatProfit.subtract(floatProfitExpect).abs().compareTo(BigDecimal.valueOf(0.5)) <= 0,
                    "网格" + orderId + "浮动收益计算不一致，测试：" + floatProfit + "，预期：" + floatProfitExpect);
            Assert.assertTrue(floatProfitRatio.subtract(floatProfitRatioExpect).abs().compareTo(BigDecimal.valueOf(0.01)) <= 0,
                    "网格" + orderId + "浮动收益率计算不一致，测试：" + floatProfitRatio + "，预期：" + floatProfitRatioExpect);
            Assert.assertTrue(totalPnL.subtract(totalPnLExpect).abs().compareTo(BigDecimal.valueOf(1)) <= 0,
                    "网格" + orderId + "总收益计算不一致，测试：" + totalPnL + "，预期：" + totalPnLExpect);
            Assert.assertTrue(pnlRatio.subtract(pnlRatioExpect).abs().compareTo(BigDecimal.valueOf(0.01)) <= 0,
                    "网格" + orderId + "总收益率计算不一致，测试：" + pnlRatio + "，预期：" + pnlRatioExpect);
            // Assert.assertTrue(APR.subtract(APRExpect).abs().compareTo(BigDecimal.valueOf(400)) <= 0,
            //         "网格" + orderId + "年化收益率计算不一致，测试：" + APR + "，预期：" + APRExpect);
        }

        logger.info("********************pass********************");
    }

}

