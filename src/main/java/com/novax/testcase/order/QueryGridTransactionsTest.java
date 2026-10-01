package com.novax.testcase.order;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.entity.model.ArbitrageGroupModel;
import com.novax.entity.model.GridArbitrageModel;
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
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询现货网格订单下的成交记录列表
 * GET/v1/gridSpotOrders/{orderId}/transactions
 */
public class QueryGridTransactionsTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridTransactionsTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyTransactions();
    }

    @DataProvider(name = "datas")
    public Object[][] getProvider() throws IOException {
        return ExcelUtil.readObjDatas("/order/queryGridTransactions.xls", 0);
    }

    @Test(dataProvider = "datas", groups = {"smokeTest"}, description = "查询订单下的成交记录列表")
    public void queryGridSpotOrderTransaction_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        // int index = Integer.parseInt(tcNum.substring(tcNum.length() - 1));
        int index = Integer.parseInt(tcNum.replaceAll(".*[^\\d](?=(\\d+))", ""));
        String attributeName = allAttributeNameList.get(index - 1);
        Long orderId = Long.parseLong((String) context.getAttribute(attributeName));
        // Long orderId = 523478888824827904L;

        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
        JSONObject resp = httpClientUtil.sendGet(requestUrl, StringToMap.stringToMap(params), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        int arbitrageNum = 0;
        JSONArray items = resp.getJSONObject("data").getJSONArray("items");
        JSONArray arbitrageGroups = new JSONArray();
        for (Object item : items) {
            JSONObject group = (JSONObject) JSON.toJSON(item);
            JSONArray transactions = group.getJSONArray("transactions");
            if (transactions.size() == 2) {
                arbitrageNum++;
                arbitrageGroups.add(item);
            }
        }

        // 预期
        GridArbitrageModel gridArbitrageModel = GridStatistics.queryGridArbitrageGroup(orderId);
        int arbitrageTimes = gridArbitrageModel.getArbitrageTimes();
        List<ArbitrageGroupModel> list = gridArbitrageModel.getList();
        List<ArbitrageGroupModel> groupModelList = list.stream().sorted(Comparator.comparing(ArbitrageGroupModel::getArbitrageTime).reversed())
                .collect(Collectors.toList());

        Assert.assertEquals(arbitrageNum, arbitrageTimes, "网格：" + orderId + "套利次数统计不一致，测试：" + arbitrageNum + "，预期：" + arbitrageTimes);

        // 套利公式检查  套利=网格卖出成交额-网格买入成交额-卖出手续费+粉尘币以当前最新价折U
        for (Object arbitrageGroup : arbitrageGroups) {
            JSONObject group = (JSONObject) JSON.toJSON(arbitrageGroup);
            long groupId = group.getLongValue("groupId");
            BigDecimal totalProfit = group.getBigDecimal("totalProfit");
            JSONArray transactions = group.getJSONArray("transactions");
            JSONObject sellTransaction = transactions.getJSONObject(0);
            JSONObject buyTransaction = transactions.getJSONObject(1);
            BigDecimal tradeTotalOfSell = sellTransaction.getBigDecimal("tradeTotal");
            BigDecimal feeOfSell = sellTransaction.getBigDecimal("fee"); // 单位U
            BigDecimal orderAmountOfSell = sellTransaction.getBigDecimal("orderAmount");
            BigDecimal price = sellTransaction.getBigDecimal("price");
            BigDecimal tradeTotalOfBuy = buyTransaction.getBigDecimal("tradeTotal");
            BigDecimal feeOfBuy = buyTransaction.getBigDecimal("fee");// 单位交易货币
            BigDecimal orderAmountOfBuy = buyTransaction.getBigDecimal("orderAmount");

            BigDecimal dustCoin = orderAmountOfBuy.subtract(orderAmountOfSell).subtract(feeOfBuy);
            BigDecimal dustCoinAmount = dustCoin.multiply(price);
            BigDecimal gridProfitExpect = tradeTotalOfSell.subtract(tradeTotalOfBuy).subtract(feeOfSell).add(dustCoinAmount);

            Assert.assertTrue(totalProfit.subtract(gridProfitExpect).abs().compareTo(BigDecimal.valueOf(0.001)) <= 0,
                    "网格" + orderId + "套利组" + groupId + "套利金额计算不一致，测试：" + totalProfit + "，预期：" + gridProfitExpect);
        }

        // 与mongo中数据比对
        for (int i = 0; i < arbitrageNum; i++) {
            JSONObject group = arbitrageGroups.getJSONObject(i);
            long id = group.getLongValue("groupId");
            BigDecimal totalProfit = group.getBigDecimal("totalProfit");
            long tradeTime = group.getLongValue("tradeTime");

            // 预期
            ArbitrageGroupModel arbitrageGroupModel = groupModelList.get(i);
            Long groupId = arbitrageGroupModel.getGroupId();
            BigDecimal arbitrageAmount = arbitrageGroupModel.getArbitrageAmount();
            Long arbitrageTime = arbitrageGroupModel.getArbitrageTime();

            Assert.assertEquals(id, groupId, "网格" + orderId + "套利组不一致");
            Assert.assertEquals(tradeTime, arbitrageTime, "网格" + orderId + "套利组" + id + "套利时间不一致，测试：" + tradeTime + "，预期：" + arbitrageTime);
            Assert.assertTrue(totalProfit.subtract(arbitrageAmount).abs().compareTo(BigDecimal.valueOf(0.01)) <= 0, "网格" + orderId + "套利组" + id + "套利金额统计不一致，测试：" + totalProfit + "，预期：" + arbitrageAmount);
        }

        logger.info("********************pass********************");
    }

}
