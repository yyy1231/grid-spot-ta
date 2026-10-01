package com.novax.testcase.order;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.GridSpotStrategyEntity;
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
import java.util.HashMap;

/**
 * 查询现货网格订单下的现货委托
 * GET/v1/gridSpotOrders/{orderId}/spotOrders
 */
public class QueryGridEntrustTest extends PublicParams {
    Logger logger = Logger.getLogger(QueryGridEntrustTest.class);
    HttpClientUtil httpClientUtil;
    String url;

    @BeforeClass(groups = {"smokeTest"})
    public void setUp() {
        httpClientUtil = new HttpClientUtil();
        url = baseUrl + UrlProperty.getStrategyEntrust();
    }

    @DataProvider(name = "datas_success")
    public Object[][] getProvider() throws IOException {
        return ExcelUtil.readObjDatas("/order/queryGridEntrust.xls", 0);
    }

    @Test(dataProvider = "datas_success", groups = {"smokeTest"}, description = "检查订单委托单")
    public void queryGridSpotOrderEntrust_Success_Test(String tcNum, String tcName, String tcDescp, String reqAdr, String headers, String params, String expect, ITestContext context) throws Exception {
        // int index = Integer.parseInt(tcNum.substring(tcNum.length() - 1));
        int index = Integer.parseInt(tcNum.replaceAll(".*[^\\d](?=(\\d+))", ""));
        String attributeName = allAttributeNameList.get(index-1);
        Long orderId = Long.parseLong((String) context.getAttribute(attributeName));

        String requestUrl = VariableReplace.replace(url, String.valueOf(orderId));
        JSONObject resp = httpClientUtil.sendGet(requestUrl, new HashMap<>(), reqHeaders);

        logger.info(tcNum + "-url:" + requestUrl);
        logger.info(tcNum + "-headers:" + headers);
        logger.info(tcNum + "-入参:" + params);
        logger.info(tcNum + "-响应数据:" + resp);
        logger.info(tcNum + "-预期结果:" + expect);

        Assert.assertEquals(resp.get("code"), JSONObject.parseObject(expect).get("code"), resp.getString("msg"));
        Assert.assertEquals(resp.get("msg"), JSONObject.parseObject(expect).get("msg"));

        Thread.sleep(2000);
        GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class,
                m -> m.selectByGridSpotOrderId(Long.parseLong((String) context.getAttribute(attributeName))));
        int gridQuantity = strategyEntity.getGridQuantity();

        // 运行中订单：委托买数量+委托卖数量=网格数；待触发订单：委托买数量+委托卖数量=0
        int sellOrdersNum = resp.getJSONObject("data").getJSONArray("sellOrders").size();
        int purchaseOrdersNum = resp.getJSONObject("data").getJSONArray("purchaseOrders").size();
        int count = sellOrdersNum + purchaseOrdersNum;
        if (attributeName.equals("orderId_triggerUp") || attributeName.equals("orderId_triggerDown")) {
            Assert.assertEquals(count, 0,
                    "网格" + orderId + "待触发状态订单，委托单断言不通过，测试：" + count + "，预期：0");
        } else {
            Assert.assertEquals(count, gridQuantity,
                    "网格" + orderId + "运行中订单，委托单断言不通过，测试：" + count + "，预期：" + gridQuantity);
        }

        logger.info("********************pass********************");
    }
}
