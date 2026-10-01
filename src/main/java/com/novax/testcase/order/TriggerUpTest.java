package com.novax.testcase.order;

import com.novax.entity.mysql.GridSpotOrderEntity;
import com.novax.entity.mysql.GridSpotStrategyEntity;
import com.novax.mapper.GridSpotOrderEntityMapper;
import com.novax.mapper.GridSpotStrategyEntityMapper;
import com.novax.testcase.AdjustMarketPrice2;
import com.novax.testcase.GridStatistics;
import com.novax.testcase.PublicParams;
import com.novax.util.MyBatisUtil;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import java.math.BigDecimal;

/**
 * 网格触发-向上
 */

public class TriggerUpTest extends PublicParams {
    Logger logger = Logger.getLogger(TriggerUpTest.class);

    @Test(groups = {"smokeTest"}, description = "待触发状态订单，行情上行达到触发价，网格自动启动")
    public void triggerPrice_Up_Test(ITestContext context) throws Exception {
        long orderId = Long.parseLong((String) context.getAttribute("orderId_triggerUp"));
        // 获取订单的触发价格
        GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        BigDecimal runningTriggerPrice = strategyEntity.getRunningTriggerPrice();
        // 获取订单symbol
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        String symbol = orderEntity.getSymbol();
        // 检查触发前订单状态
        byte status = orderEntity.getStatus();
        Assert.assertEquals(status, 0, "网格" + orderId + "状态检查不通过，或行情波动较大，网格已提前触发，测试：" + status + "，预期：0");

        // 调用工具类》向上推动市场行情到触发价，启动网格
        logger.info(symbol + ",向上推动行情到:" + runningTriggerPrice);
        AdjustMarketPrice2.adjustPrice(symbol, runningTriggerPrice, 1);

        // 检查订单状态，断言网格是否启动
        Thread.sleep(30000);
        GridSpotOrderEntity orderEntity_triggerUp = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        byte status1 = orderEntity_triggerUp.getStatus();
        Assert.assertTrue(status1 == 1 || status1 == 2, "网格：" + orderId + "未启动；或未推到触发价；或等待时间不够提前断言了，手动检查下");

        // 断言网格事件 GridSpotAutomatedStartUp
        int result = GridStatistics.searchGridEvent(orderId, "GridSpotAutomatedStartUp");
        Assert.assertTrue(result != 0, "网格" + orderId + "未触发，未查询到GridSpotAutomatedStartUp事件");

        logger.info("********************pass********************");
    }
}
