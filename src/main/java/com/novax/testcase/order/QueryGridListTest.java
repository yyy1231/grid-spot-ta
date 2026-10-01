package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.condition.GridSpotOrderSelectEntity;
import com.novax.entity.enums.GridTypeEnum;
import com.novax.entity.mysql.GridSpotOrderEntity;
import com.novax.entity.mysql.GridSpotStrategyEntity;
import com.novax.mapper.GridSpotOrderEntityMapper;
import com.novax.mapper.GridSpotStrategyEntityMapper;
import com.novax.testcase.GridStrategyAnalysis;
import com.novax.testcase.PublicParams;
import com.novax.util.*;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 查询现货网格订单列表
 * GET/v1/gridSpotOrders
 */
public class QueryGridListTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridListTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyQuery();
    }

    @DataProvider(name = "datas_success")
    public Object[][] getProvider1() throws IOException {
        return ExcelUtil.readObjDatas("/order/queryGridList.xls", 0);
    }

    @DataProvider(name = "datas_fail")
    public Object[][] getProvider2() throws IOException {
        return ExcelUtil.readObjDatas("/order/queryGridList.xls", 1);
    }

    @Test(dataProvider = "datas_success", groups = {"smokeTest"}, description = "查询网格列表")
    public void queryGridSpotOeders_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        HashMap<String, String> reqParams = StringToMap.stringToMap(params);
        Long userId = Long.valueOf(reqHeaders.get("userId"));
        JSONObject resp = httpClientUtil.sendGet(url, reqParams, reqHeaders);

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + reqHeaders);
        logger.info(tcNum + "-入参:" + reqParams);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
        Integer total = resp.getJSONObject("data").getInteger("total");

        GridSpotOrderSelectEntity condition = GridSpotOrderSelectEntity.builder()
                .userId(userId)
                .isRunning(Boolean.parseBoolean(reqParams.get("isRunning")))
                .build();
        int totalNum = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.count(condition));
        Assert.assertEquals(total, totalNum, "用户" + userId + "网格列表查询不一致");

        // 断言订单策略形态，运行中的订单，第一条数据
        if (tcDescp.equals("查询网格列表isRunning=true")) {
            JSONObject grid = resp.getJSONObject("data").getJSONArray("items").getJSONObject(0);
            Long orderId = grid.getLong("id");
            // 断言单网格买入量
            BigDecimal amountPerGrid = grid.getBigDecimal("amountPerGrid");
            // 断言单网格最低收益率
            BigDecimal minProfitRatePerGrid = grid.getBigDecimal("minProfitRatePerGrid");
            // 断言单网格最高收益率
            BigDecimal maxProfitRatePerGrid = grid.getBigDecimal("maxProfitRatePerGrid");

            GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
            GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
            String gridType = GridTypeEnum.getDescByType(strategyEntity.getGridType());
            Map<String, BigDecimal> result = GridStrategyAnalysis.calculate(strategyEntity.getUpperLimitPrice(), strategyEntity.getLowerLimitPrice(), strategyEntity.getGridQuantity(), orderEntity.getSymbol(), orderEntity.getInvestment(), gridType);
            BigDecimal amountPerGridExpect = result.get("amountPerGrid");
            BigDecimal minProfitRatePerGridExpect = result.get("minProfitRatePerGrid");
            BigDecimal maxProfitRatePerGridExpect = result.get("maxProfitRatePerGrid");


            Assert.assertEquals(amountPerGrid.subtract(amountPerGridExpect).abs().compareTo(new BigDecimal("0.0005")), -1,
                    "网格" + orderId + "单网格买入数量计算不一致，测试：" + amountPerGrid + "，预期：" + amountPerGridExpect);
            Assert.assertEquals(minProfitRatePerGrid.subtract(minProfitRatePerGridExpect).abs().compareTo(new BigDecimal("0.5")), -1,
                    "网格" + orderId + "单网格最低收益率计算不一致，测试：" + minProfitRatePerGrid + "，预期：" + minProfitRatePerGridExpect);
            Assert.assertEquals(maxProfitRatePerGrid.subtract(maxProfitRatePerGridExpect).abs().compareTo(new BigDecimal("0.5")), -1,
                    "网格" + orderId + "单网格最高收益率计算不一致，测试：" + maxProfitRatePerGrid + "，预期：" + maxProfitRatePerGridExpect);
        }

        logger.info("********************pass********************");

    }

    @Test(dataProvider = "datas_fail", description = "查询网格列表异常场景")
    public void queryGridSpotOeders_Fail_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect) throws Exception {
        JSONObject resp = httpClientUtil.sendGet(url, StringToMap.stringToMap(params), reqHeaders);

        logger.info(tcNum + "-url:" + url);
        logger.info(tcNum + "-headers:" + StringToMap.stringToMap(headers));
        logger.info(tcNum + "-入参:" + StringToMap.stringToMap(params));
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));
    }
}
