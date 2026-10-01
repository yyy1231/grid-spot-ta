package com.novax.testcase.order;


import com.novax.entity.mysql.GridSpotOrderEntity;
import com.novax.entity.mysql.GridSpotStrategyEntity;
import com.novax.mapper.GridSpotOrderEntityMapper;
import com.novax.mapper.GridSpotStrategyEntityMapper;
import com.novax.testcase.AdjustMarketPrice2;
import com.novax.testcase.GridStatistics;
import com.novax.util.MyBatisUtil;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 网格上移
 */
public class TrailingUpGridTest {
    Logger logger = Logger.getLogger(TrailingUpGridTest.class);

    @Test(groups = {"smokeTest"}, description = "网格上移")
    public void trailingUp_GridSpotPrice_Test(ITestContext context) throws Exception {
        BigDecimal targetPrice;
        BigDecimal expectUpPrice;
        BigDecimal expectLowPrice;

        long orderId = Long.parseLong((String) context.getAttribute("orderId_trailingUp"));
        logger.info("订单号，orderId_trailingUp：" + orderId);

        // 获取订单的上下限价格、上移上限、价差、symbol
        GridSpotStrategyEntity strategyEntity = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        BigDecimal lowerLimitPrice = strategyEntity.getLowerLimitPrice();
        BigDecimal upperLimitPrice = strategyEntity.getUpperLimitPrice();
        BigDecimal trailingUpLimit = strategyEntity.getTrailingUpLimit();
        short gridQuantity = strategyEntity.getGridQuantity();
        BigDecimal priceDif = upperLimitPrice.subtract(lowerLimitPrice).divide(BigDecimal.valueOf(gridQuantity), 5, RoundingMode.HALF_UP);
        GridSpotOrderEntity orderEntity = MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(orderId));
        String symbol = orderEntity.getSymbol();
        logger.info("市场:" + symbol);
        logger.info("订单号，orderId_trailingUp：" + orderId + "，网格下限：" + lowerLimitPrice);
        logger.info("订单号，orderId_trailingUp：" + orderId + "，网格上限：" + upperLimitPrice);
        logger.info("订单号，orderId_trailingUp：" + orderId + "，上移上限：" + trailingUpLimit);
        logger.info("订单号，orderId_trailingUp：" + orderId + "，网格价差：" + priceDif);

        // 1、调用工具类>第一次推动行情到【上限+价差*0.5】>查数据库>断言网格上下限：不变
        targetPrice = upperLimitPrice.add(priceDif.multiply(new BigDecimal("0.5")));
        logger.info("第一次推动市场到【上限+价差*0.5】：" + targetPrice);
        AdjustMarketPrice2.adjustPrice(symbol, targetPrice, 1);
        expectUpPrice = new BigDecimal(upperLimitPrice.stripTrailingZeros().toPlainString());
        expectLowPrice = new BigDecimal(lowerLimitPrice.stripTrailingZeros().toPlainString());
        logger.info("第一次推动行情，预期上限不变：" + expectUpPrice);
        logger.info("第一次推动行情，预期下限不变：" + expectLowPrice);
        Thread.sleep(10000);
        GridSpotStrategyEntity strategyEntity1 = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        Assert.assertEquals(new BigDecimal(strategyEntity1.getUpperLimitPrice().stripTrailingZeros().toPlainString()), expectUpPrice,
                "第一次推动行情，预期上下限不变，不符预期");
        Assert.assertEquals(new BigDecimal(strategyEntity1.getLowerLimitPrice().stripTrailingZeros().toPlainString()), expectLowPrice);

        // 2、调用工具类>第二次推动行情到【上限+价差*1】>查数据库>断言网格上下限：上移一格
        targetPrice = upperLimitPrice.add(priceDif.multiply(BigDecimal.ONE));
        logger.info("第二次推动市场到【上限+价差*1】：" + targetPrice);
        AdjustMarketPrice2.adjustPrice(symbol, targetPrice, 1);
        expectUpPrice = new BigDecimal(targetPrice.stripTrailingZeros().toPlainString());
        expectLowPrice = new BigDecimal(lowerLimitPrice.add(priceDif).stripTrailingZeros().toPlainString());
        logger.info("第二次推动行情，预期上移一格，上限+【价差*1】：" + expectUpPrice);
        logger.info("第二次推动行情，预期上移一格，下限+【价差*1】：" + expectLowPrice);
        Thread.sleep(30000);
        GridSpotStrategyEntity strategyEntity2 = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        Assert.assertEquals(new BigDecimal(strategyEntity2.getUpperLimitPrice().stripTrailingZeros().toPlainString()), expectUpPrice,
                "第二次推动行情，预期上移1格，不符预期，或未推到目标价:" + targetPrice);
        Assert.assertEquals(new BigDecimal(strategyEntity2.getLowerLimitPrice().stripTrailingZeros().toPlainString()), expectLowPrice);

        // 3、调用工具类>第三次推动行情到【上限+价差*2】>查数据库>断言网格上下限：上移2格
        targetPrice = upperLimitPrice.add(priceDif.multiply(new BigDecimal("2")));
        logger.info("第三次推动市场到【上限+价差*2】：" + targetPrice);
        AdjustMarketPrice2.adjustPrice(symbol, targetPrice, 1);
        expectUpPrice = new BigDecimal(targetPrice.stripTrailingZeros().toPlainString());
        expectLowPrice = new BigDecimal(lowerLimitPrice.add(priceDif.multiply(new BigDecimal("2"))).stripTrailingZeros().toPlainString());
        logger.info("第三次推动行情，预期上移2格，上限+【价差*2】：" + expectUpPrice);
        logger.info("第三次推动行情，预期上移2格，下限+【价差*2】：" + expectLowPrice);
        Thread.sleep(20000);
        GridSpotStrategyEntity strategyEntity3 = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        Assert.assertEquals(new BigDecimal(strategyEntity3.getUpperLimitPrice().stripTrailingZeros().toPlainString()), expectUpPrice,
                "第三次推动行情，预期上移2格，不符预期，或未推到目标价：" + targetPrice);
        Assert.assertEquals(new BigDecimal(strategyEntity3.getLowerLimitPrice().stripTrailingZeros().toPlainString()), expectLowPrice);

        // 4、调用工具类>第四次推动行情到【大于网格移动上限】 >查数据库>断言网格上下限：不再上移
        targetPrice = trailingUpLimit.add(BigDecimal.TEN);
        logger.info("第四次推动行情，大于网格移动上限：" + targetPrice);
        AdjustMarketPrice2.adjustPrice(symbol, targetPrice, 1);
        logger.info("第四次推动行情大于网格移动上限，预期网格不再移动：" + expectUpPrice);
        logger.info("第四次推动行情大于网格移动上限，预期网格不再移动：" + expectLowPrice);
        Thread.sleep(20000);
        GridSpotStrategyEntity strategyEntity4 = MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(orderId));
        Assert.assertEquals(new BigDecimal(strategyEntity4.getUpperLimitPrice().stripTrailingZeros().toPlainString()), expectUpPrice,
                "第四次推动行情大于网格移动上限，预期网格不再移动，不符预期");
        Assert.assertEquals(new BigDecimal(strategyEntity4.getLowerLimitPrice().stripTrailingZeros().toPlainString()), expectLowPrice);

        // 断言网格事件 GridSpotStrategyTrailingUp 预期2次
        int times = GridStatistics.searchGridEvent(orderId, "GridSpotStrategyTrailingUp");
        Assert.assertEquals(times, 2, "多次推动行情，预期网格上移2次，存在2次上移事件，不符预期，或行情未达到目标价");

        logger.info("********************pass********************");
    }
}



