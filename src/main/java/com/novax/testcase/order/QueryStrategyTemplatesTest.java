package com.novax.testcase.order;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.GridSpotStrategyTemplateEntity;
import com.novax.mapper.GridSpotStrategyTemplateEntityMapper;
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
import java.util.HashMap;
import java.util.List;

/**
 * 查询现货网格策略模板
 * GET/v1/gridSpotStrategyTemplates
 */
public class QueryStrategyTemplatesTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryStrategyTemplatesTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyTemplates();
    }

    @DataProvider(name = "datas_success")
    public Object[][] getProvider1() throws IOException {
        return ExcelUtil.readObjDatas("/order/queryStrategyTemplates.xls", 0);
    }

    @DataProvider(name = "datas_fail")
    public Object[][] getProvider2() throws IOException {
        return ExcelUtil.readObjDatas("/order/queryStrategyTemplates.xls", 1);
    }

    @Test(dataProvider = "datas_success", groups = {"smokeTest"}, description = "查询网格策略模板")
    public void queryStrategyTemplates_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        HashMap<String, String> reqParams = StringToMap.stringToMap(params);
        JSONObject resp = httpClientUtil.sendGet(url, reqParams, reqHeaders);

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        // 数据库断言-条数
        String symbol = StringToMap.stringToMap(params).get("symbol");
        List<GridSpotStrategyTemplateEntity> templateEntityList = MyBatisUtil.execute(GridSpotStrategyTemplateEntityMapper.class, m -> m.selectBySymbol(symbol));
        int count = templateEntityList.size();
        JSONArray data = resp.getJSONArray("data");
        int templateNum = data.size();
        Assert.assertEquals(templateNum, count, "交易市场" + symbol + "，策略模板查询结果不一致，测试：" + templateNum + "，预期：" + count);

        // 断言策略模板基本逻辑
        for (int i = 0; i < templateNum; i++) {
            // 回测周期/天、策略模板名称、策略模板id
            int backtesting_period = (int) data.getJSONObject(i).get("backtestingPeriod");
            String name = (String) data.getJSONObject(i).get("name");
            String strategyTemplateId = (String) data.getJSONObject(i).get("id");
            // 断言交易市场、网格类型
            Assert.assertEquals(data.getJSONObject(i).get("symbol"), symbol);
            Assert.assertEquals(data.getJSONObject(i).get("gridType"), "arithmetical");
            // 上限价格>下限价格
            BigDecimal lowerLimitPrice = data.getJSONObject(i).getBigDecimal("lowerLimitPrice");
            BigDecimal upperLimitPrice = data.getJSONObject(i).getBigDecimal("upperLimitPrice");
            Assert.assertEquals(upperLimitPrice.compareTo(lowerLimitPrice), 1);
            // 每网格最高利润率>每网格最低利润率
            BigDecimal minProfitRatePerGrid = data.getJSONObject(i).getBigDecimal("minProfitRatePerGrid");
            BigDecimal maxProfitRatePerGrid = data.getJSONObject(i).getBigDecimal("maxProfitRatePerGrid");
            Assert.assertEquals(maxProfitRatePerGrid.compareTo(minProfitRatePerGrid), 1);
            // 回测周期
            BigDecimal backtestingPeriod = data.getJSONObject(i).getBigDecimal("backtestingPeriod");
            // 实际回测收益率 (接口没返回该字段)
            // BigDecimal actualBacktestingProfitRate = data.getJSONObject(i).getBigDecimal("actualBacktestingProfitRate");
            // 回测收益率
            BigDecimal backtestingProfitRate = data.getJSONObject(i).getBigDecimal("backtestingProfitRate");
            // 回测年化收益率
            BigDecimal backtestingAPR = data.getJSONObject(i).getBigDecimal("backtestingAPR");
            logger.info("策略模板名称:" + name);
            logger.info("回测周期/天:" + backtesting_period);
            logger.info("回测年化收益率:" + backtestingAPR);
            // 回测年化收益率=回测收益率*365/回测周期
            BigDecimal actualBacktestingProfitRateDB = templateEntityList.get(i).getActualBacktestingProfitRate();
            BigDecimal backtestingAPRcheck;
            if (actualBacktestingProfitRateDB.compareTo(BigDecimal.ZERO) <= 0) {
                backtestingAPRcheck = backtestingProfitRate;
            } else {
                backtestingAPRcheck = backtestingProfitRate.multiply(new BigDecimal(365)).divide(backtestingPeriod, 2, RoundingMode.HALF_UP);
            }
            logger.info("回测年化收益率-验算:" + backtestingAPRcheck);
            Assert.assertTrue(backtestingAPR.subtract(backtestingAPRcheck).abs().compareTo(new BigDecimal("0.5")) <= 0,
                    "回测年化收益率=回测收益率*365/回测周期,验算不通过");

            // 模板id写入ITestContext，供关联接口使用，数据共享
            context.setAttribute(name, strategyTemplateId);
        }

        logger.info("********************pass********************");

        for (String attributeName : context.getAttributeNames()) {
            System.out.println(attributeName + ":" + context.getAttribute(attributeName));
        }
    }

    @Test(dataProvider = "datas_fail", description = "查询网格策略模板异常场景")
    public void queryStrategyTemplates_Fail_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        JSONObject resp = httpClientUtil.sendGet(url, StringToMap.stringToMap(params), StringToMap.stringToMap(headers));

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"));
        Assert.assertEquals(resp.get("message"), JSONObject.parseObject(expect).get("message"));

    }
}
