package com.novax.testcase;

import com.alibaba.fastjson.JSONObject;
import com.novax.entity.mysql.SpotConfigEntity;
import com.novax.entity.mysql.VipLevelConfigEntity;
import com.novax.mapper.SpotConfigEntityMapper;
import com.novax.mapper.VipLevelConfigEntityMapper;
import com.novax.util.MyBatisUtil;
import com.novax.util.RedisUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridStrategyAnalysis {
    static Logger logger = Logger.getLogger(GridStrategyAnalysis.class);

    public static Map<String, BigDecimal> calculate(String params) {
        Map<String, BigDecimal> result = new HashMap<>();
        JSONObject object = JSONObject.parseObject(params);
        BigDecimal upperLimitPrice = object.getBigDecimal("upperLimitPrice");
        BigDecimal lowerLimitPrice = object.getBigDecimal("lowerLimitPrice");
        int gridNum = object.getInteger("gridQuantity");
        String symbol = object.getString("symbol");
        BigDecimal investmentAmount = object.getBigDecimal("investmentAmount");
        String gridType = object.getString("gridType");

        // 查询spot_config配置信息
        SpotConfigEntity entity = MyBatisUtil.execute(SpotConfigEntityMapper.class, m -> m.selectBySymbol(symbol));
        BigDecimal buyMin = entity.getBuyMin();
        BigDecimal rate = tradeRate();
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            rate = new BigDecimal("0.002");
        }
        logger.info("rate:" + rate);
        logger.info("buyMin" + buyMin);

        // 等差网格模式
        if (gridType.equals("arithmetical")) {
            // 网格价差
            BigDecimal priceDif = upperLimitPrice.subtract(lowerLimitPrice).divide(BigDecimal.valueOf(gridNum), 5, RoundingMode.HALF_UP);
            logger.info("网格价差：" + priceDif);
            // 每个网格的价格
            ArrayList<BigDecimal> pricePerGrids = new ArrayList<>();
            for (int i = 0; i < gridNum + 1; i++) {
                pricePerGrids.add(lowerLimitPrice.add(priceDif.multiply(BigDecimal.valueOf(i))));
            }

            // 单网格买入量
            BigDecimal sumPrice = new BigDecimal("0.0000");
            for (int i = 0; i < gridNum; i++) {
                sumPrice = sumPrice.add(pricePerGrids.get(i));
            }
            BigDecimal amountPerGrid = investmentAmount.divide(sumPrice, 4, RoundingMode.HALF_UP);
            result.put("amountPerGrid", amountPerGrid);
            logger.info("等差单网格买入量：" + amountPerGrid);

            // 单网格最低收益率minProfitRatePerGrid = ((U/(U-gap))*(1-rateOfFee)²)-1
            BigDecimal minProfitRatePerGrid = ((upperLimitPrice.divide(upperLimitPrice.subtract(priceDif), 4, RoundingMode.HALF_UP))
                    .multiply(new BigDecimal(1).subtract(rate))
                    .multiply(new BigDecimal(1).subtract(rate))).subtract(new BigDecimal(1));
            logger.info("等差单网格最低收益率：" +minProfitRatePerGrid);
            result.put("minProfitRatePerGrid", minProfitRatePerGrid);

            // 单网格最高收益率maxProfitRatePerGrid = (((L+gap)/L)*(1-rateOfFee)²)-1
            BigDecimal maxProfitRatePerGrid = (((lowerLimitPrice.add(priceDif)).divide(lowerLimitPrice, 4, RoundingMode.HALF_UP))
                    .multiply(new BigDecimal(1).subtract(rate))
                    .multiply(new BigDecimal(1).subtract(rate))).subtract(new BigDecimal(1));
            logger.info("等差单网格最高收益率：" +maxProfitRatePerGrid);
            result.put("maxProfitRatePerGrid", maxProfitRatePerGrid);
        } else if (gridType.equals("geometrical")) {
            // 等比网格模式,公比q
            BigDecimal pricePercent = upperLimitPrice.divide(lowerLimitPrice, 5, RoundingMode.HALF_UP);
            BigDecimal q = BigDecimalRoot.bigRoot(pricePercent, gridNum, 5, BigDecimal.ROUND_HALF_UP);
            logger.info("公比：" + q);
            // 每个网格的价格
            ArrayList<BigDecimal> pricePerGrids = new ArrayList<>();
            for (int i = 0; i < gridNum + 1; i++) {
                pricePerGrids.add(lowerLimitPrice.multiply(q.pow(i)));
            }
            // 单网格买入量
            BigDecimal sumPrice = new BigDecimal("0.0000");
            for (int i = 0; i < gridNum; i++) {
                sumPrice = sumPrice.add(pricePerGrids.get(i));
            }
            BigDecimal amountPerGrid = investmentAmount.divide(sumPrice, 4, RoundingMode.HALF_UP);
            result.put("amountPerGrid", amountPerGrid);
            logger.info("等比单网格买入量：" + amountPerGrid);

            // 单网格最高、最低收益率相等minProfitRatePerGrid = maxProfitRatePerGrid =(proportion*(1 - rateOfFee)²)-1
            BigDecimal profitRatePerGrid = (q.multiply(BigDecimal.ONE.subtract(rate))
                    .multiply(BigDecimal.ONE.subtract(rate)))
                    .subtract(BigDecimal.ONE);
            logger.info("等比单网格最低收益率：" +profitRatePerGrid);
            result.put("minProfitRatePerGrid", profitRatePerGrid);
            logger.info("等比网格，单网格最高、最低收益率相等");
            result.put("maxProfitRatePerGrid", profitRatePerGrid);
        } else {
            logger.info("网格类型不合法");
        }

        return result;
    }

    public static Map<String, BigDecimal> calculate(BigDecimal upperLimitPrice, BigDecimal lowerLimitPrice, int gridQuantity, String symbol, BigDecimal investmentAmount, String gridType) {
        Map<String, BigDecimal> result = new HashMap<>();
        // 查询spot_config配置信息
        SpotConfigEntity entity = MyBatisUtil.execute(SpotConfigEntityMapper.class, m -> m.selectBySymbol(symbol));

        BigDecimal rate = tradeRate();
        BigDecimal buyMin = entity.getBuyMin();
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            rate = new BigDecimal("0.002");
        }
        logger.info("rate:" + rate);
        logger.info("buyMin" + buyMin);
        // 等差网格模式
        if (gridType.equals("arithmetical")) {
            // 网格价差
            BigDecimal priceDif = upperLimitPrice.subtract(lowerLimitPrice).divide(BigDecimal.valueOf(gridQuantity), 5, RoundingMode.HALF_UP);
            logger.info("网格价差：" + priceDif);
            // 每个网格的价格
            ArrayList<BigDecimal> pricePerGrid = new ArrayList<>();
            for (int i = 0; i < gridQuantity + 1; i++) {
                pricePerGrid.add(lowerLimitPrice.add(priceDif.multiply(BigDecimal.valueOf(i))));
            }
            // 单网格买入量
            BigDecimal sumPrice = new BigDecimal("0.00000");
            for (int i = 0; i < gridQuantity; i++) {
                sumPrice = sumPrice.add(pricePerGrid.get(i));
            }
            BigDecimal amountPerGrid = investmentAmount.divide(sumPrice, 5, RoundingMode.HALF_UP);
            result.put("amountPerGrid", amountPerGrid);
            logger.info("单网格买入量：" + amountPerGrid);

            // 单网格最低收益率minProfitRatePerGrid = ((U/(U-gap))*(1-rateOfFee)²)-1
            BigDecimal minProfitRatePerGrid = (((upperLimitPrice.divide(upperLimitPrice.subtract(priceDif), 5, RoundingMode.HALF_UP))
                    .multiply(new BigDecimal(1).subtract(rate))
                    .multiply(new BigDecimal(1).subtract(rate))).subtract(new BigDecimal(1)))
                    .multiply(new BigDecimal(100));
            logger.info("单网格最低收益率：" +minProfitRatePerGrid);
            result.put("minProfitRatePerGrid", minProfitRatePerGrid);

            // 单网格最高收益率maxProfitRatePerGrid = (((L+gap)/L)*(1-rateOfFee)²)-1
            BigDecimal maxProfitRatePerGrid = ((((lowerLimitPrice.add(priceDif)).divide(lowerLimitPrice, 5, RoundingMode.HALF_UP))
                    .multiply(new BigDecimal(1).subtract(rate))
                    .multiply(new BigDecimal(1).subtract(rate))).subtract(new BigDecimal(1)))
                    .multiply(new BigDecimal(100));
            logger.info("单网格最高收益率：" +maxProfitRatePerGrid);
            result.put("maxProfitRatePerGrid", maxProfitRatePerGrid);
        } else if (gridType.equals("geometrical")) {
            // 等比网格模式,公比q
            BigDecimal pricePercent = upperLimitPrice.divide(lowerLimitPrice, 5, RoundingMode.HALF_UP);
            BigDecimal q = BigDecimalRoot.bigRoot(pricePercent, gridQuantity, 5, BigDecimal.ROUND_HALF_UP);
            logger.info("公比：" + q);
            // 每个网格的价格
            ArrayList<BigDecimal> pricePerGrid = new ArrayList<>();
            for (int i = 0; i < gridQuantity + 1; i++) {
                pricePerGrid.add(lowerLimitPrice.multiply(q.pow(i)));
            }
            // 单网格买入量
            BigDecimal sumPrice = new BigDecimal("0.0000");
            for (int i = 0; i < gridQuantity; i++) {
                sumPrice = sumPrice.add(pricePerGrid.get(i));
            }
            BigDecimal amountPerGrid = investmentAmount.divide(sumPrice, 4, RoundingMode.HALF_UP);
            result.put("amountPerGrid", amountPerGrid);
            logger.info("单网格买入量：" + amountPerGrid);

            // 单网格最高、最低收益率相等minProfitRatePerGrid = maxProfitRatePerGrid =(proportion*(1 - rateOfFee)²)-1
            BigDecimal profitRatePerGrid = ((q.multiply(BigDecimal.ONE.subtract(rate))
                    .multiply(BigDecimal.ONE.subtract(rate)))
                    .subtract(BigDecimal.ONE))
                    .multiply(new BigDecimal(100));
            logger.info("等比单网格最低收益率：" +profitRatePerGrid);
            result.put("minProfitRatePerGrid", profitRatePerGrid);
            logger.info("等比网格，单网格最高、最低收益率相等");
            result.put("maxProfitRatePerGrid", profitRatePerGrid);
        } else {
            logger.info("网格类型不合法");
        }
        return result;
    }


    public static int validateGridQuantity(String params) {
        JSONObject object = JSONObject.parseObject(params);
        BigDecimal upperLimitPrice = object.getBigDecimal("upperLimitPrice");
        BigDecimal lowerLimitPrice = object.getBigDecimal("lowerLimitPrice");
        // int gridNum = object.getInteger("gridQuantity");
        String symbol = object.getString("symbol");
        // BigDecimal investmentAmount = object.getBigDecimal("investmentAmount");
        String gridType = object.getString("gridType");

        int maxGridQuantity = 280;
        // 网格交易的所有币对手续费取0级的现货maker手续费,查vip等级配置superex.trade.vip_level_config
        List<VipLevelConfigEntity> entities = MyBatisUtil.execute(VipLevelConfigEntityMapper.class, m -> m.selectConfigs());
        BigDecimal feeRate = entities.get(0).getSpotMaker();
        if (null == feeRate || feeRate.equals(BigDecimal.ZERO)) {
            feeRate = new BigDecimal("0.002");
        }
        logger.info("手续费：" + feeRate);

        // 获取现货配置信息
        SpotConfigEntity entity = MyBatisUtil.execute(SpotConfigEntityMapper.class, m -> m.selectBySymbol(symbol));
        BigDecimal buyMinPrice = entity.getBuyMinPrice();
        int precisionOfBuyin = buyMinPrice.stripTrailingZeros().scale();
        // 计算等差网格最大网格数
        if (gridType.equals("arithmetical")) {
            // 根据手续费率计算最大网格数（价格上限-价格下限）/(价格上限*手续费率*2)
            BigDecimal maxGridQuantityFromFeeRateOriginal = (upperLimitPrice.subtract(lowerLimitPrice))
                    .divide(upperLimitPrice.multiply(feeRate).multiply(new BigDecimal("2")), 5, RoundingMode.DOWN);
            int maxGridQuantityfromFeeRate = maxGridQuantityFromFeeRateOriginal.setScale(0, RoundingMode.DOWN).intValueExact();
            if (maxGridQuantityFromFeeRateOriginal.stripTrailingZeros().scale() <= 0) {
                maxGridQuantityfromFeeRate -= 1;
            }
            // 根据价格精度buy_min_price计算最大网格数（价格上限-价格下限）/(价格精度)
            BigDecimal maxGridQuantityFromBuyMinPriceOriginal = (upperLimitPrice.subtract(lowerLimitPrice))
                    .divide(BigDecimal.valueOf(precisionOfBuyin), 5, RoundingMode.DOWN);
            int maxGridQuantityFromBuyMinPrice = maxGridQuantityFromBuyMinPriceOriginal.setScale(0, RoundingMode.DOWN).intValueExact();
            maxGridQuantity = Math.min(maxGridQuantityfromFeeRate, maxGridQuantityFromBuyMinPrice);
        } else if (gridType.equals("geometrical")) {
            // 根据手续费率计算最大网格数N <=log(MaxPrice /MinPrice)/ log(1 +2 * FeePercent)
            BigDecimal maxGridQuantityFromFeeRateOriginal = BigDecimal.valueOf(Math.log(upperLimitPrice.divide(lowerLimitPrice, 5, RoundingMode.DOWN).doubleValue()))
                    .divide(BigDecimal.valueOf(Math.log(feeRate.multiply(new BigDecimal(2)).add(BigDecimal.ONE).doubleValue())), 5, RoundingMode.DOWN);

            int maxGridQuantityfromFeeRate = maxGridQuantityFromFeeRateOriginal.setScale(0, RoundingMode.DOWN).intValueExact();
            if (maxGridQuantityFromFeeRateOriginal.stripTrailingZeros().scale() <= 0) {
                maxGridQuantityfromFeeRate -= 1;
            }
            // 根据价格精度buy_min_price计算最大网格数 maxQ = log(minPriceBuyin/L+1,U/L)
            BigDecimal maxGridQuantityFromBuyMinPriceOriginal = BigDecimal.valueOf(Math.log(upperLimitPrice.divide(lowerLimitPrice, 5, RoundingMode.DOWN).doubleValue()))
                    .divide(BigDecimal.valueOf(Math.log((buyMinPrice.divide(lowerLimitPrice, 10, RoundingMode.DOWN)).add(BigDecimal.ONE).doubleValue())), 5, RoundingMode.DOWN);
            int maxGridQuantityFromBuyMinPrice = maxGridQuantityFromBuyMinPriceOriginal.setScale(0, RoundingMode.DOWN).intValueExact();
            maxGridQuantity = Math.min(maxGridQuantityfromFeeRate, maxGridQuantityFromBuyMinPrice);

        }
        maxGridQuantity = Math.min(maxGridQuantity, 280);
        logger.info("最大网格数：" + maxGridQuantity);
        return maxGridQuantity;
    }

    public static BigDecimal validateInvestmentAmount(String params) {
        JSONObject object = JSONObject.parseObject(params);
        BigDecimal upperLimitPrice = object.getBigDecimal("upperLimitPrice");
        BigDecimal lowerLimitPrice = object.getBigDecimal("lowerLimitPrice");
        int gridNum = object.getInteger("gridQuantity");
        String symbol = object.getString("symbol");
        BigDecimal investmentAmount = object.getBigDecimal("investmentAmount");
        String gridType = object.getString("gridType");
        BigDecimal runningTriggerPrice = object.getBigDecimal("runningTriggerPrice");

        // 获取现货配置信息 superex_trade.spot_config
        SpotConfigEntity entity = MyBatisUtil.execute(SpotConfigEntityMapper.class, m -> m.selectBySymbol(symbol));
        // 价格小数位长度
        int priceTick = entity.getPriceTick().scale();
        // 数量小数点长度
        int sizeTick = entity.getSizeTick().scale();
        // 单笔最小交易额
        BigDecimal tradeMin = entity.getTradeMin();
        // 最小买入量
        BigDecimal buyMin = entity.getBuyMin();
        // 最新价
        String value = RedisUtil.getJedis().hget("spot:new:price", symbol);
        BigDecimal newestPrice = JSONObject.parseObject(value).getBigDecimal("price");
        logger.info("priceTick，价格小数位长度：" + priceTick);
        logger.info("sizeTick，数量小数点长度：" + sizeTick);
        logger.info("tradeMin，单笔最小交易额：" + tradeMin);
        logger.info("buyMin，最小买入量：" + buyMin);
        logger.info("最新价：" + newestPrice);

        // 每个网格的价格(不包括最高价)
        ArrayList<BigDecimal> pricePerGrids = new ArrayList<>();
        if (gridType.equals("arithmetical")) {
            // 网格价差
            BigDecimal priceDif = upperLimitPrice.subtract(lowerLimitPrice).divide(BigDecimal.valueOf(gridNum), 5, RoundingMode.HALF_UP);
            logger.info("网格价差：" + priceDif);
            for (int i = 0; i < gridNum; i++) {
                pricePerGrids.add(lowerLimitPrice.add(priceDif.multiply(BigDecimal.valueOf(i))));
            }
        } else if (gridType.equals("geometrical")) {
            BigDecimal pricePercent = upperLimitPrice.divide(lowerLimitPrice, 5, RoundingMode.HALF_UP);
            BigDecimal q = BigDecimalRoot.bigRoot(pricePercent, gridNum, 5, BigDecimal.ROUND_HALF_UP);
            logger.info("公比：" + q);
            for (int i = 0; i < gridNum; i++) {
                pricePerGrids.add(lowerLimitPrice.multiply(q.pow(i)));
            }
        } else {
            logger.info("网格类型不合法");
        }

        // 网格最小投入额∑(L,U-1)*max(buyMin,ceil(TradeMin/min(newprice*0.9,L,startupprice*0.9)/buyMin)*buyMin)*1.01
        BigDecimal sum = BigDecimal.ZERO;
        double minValue;
        double minValue1 = Math.min(newestPrice.multiply(new BigDecimal(0.9)).doubleValue(), lowerLimitPrice.doubleValue());
        // 触发价
        if (runningTriggerPrice != null) {
            double minValue2 = Math.min(minValue1, runningTriggerPrice.multiply(new BigDecimal(0.9)).doubleValue());
            minValue = Math.min(minValue1, minValue2);
        } else {
            minValue = minValue1;
        }

        double ceilValue = Math.ceil(tradeMin.divide(BigDecimal.valueOf(minValue), 5, RoundingMode.HALF_UP).divide(buyMin, 5, RoundingMode.HALF_UP).doubleValue());
        double maxValue = Math.max(buyMin.doubleValue(), BigDecimal.valueOf(ceilValue).multiply(buyMin).doubleValue());

        for (BigDecimal price : pricePerGrids) {
            sum = sum.add(price.multiply(BigDecimal.valueOf(maxValue)).multiply(BigDecimal.valueOf(1.01)));
        }

        logger.info("最小投入额：" + sum);
        return sum;
    }

    public static BigDecimal tradeRate() {
        return MyBatisUtil.execute(VipLevelConfigEntityMapper.class, m -> m.selectConfigs().get(0).getSpotMaker());
    }

    public static void main(String[] args) throws SQLException, IOException {
        System.out.println(tradeRate());
        // System.out.println(BigDecimalRoot.bigRoot(BigDecimal.valueOf(100), 2, 5, 1));
        calculate(new BigDecimal(3336.34), new BigDecimal(2336.34), 10, "eth_usdt", new BigDecimal(4000), "arithmetical");
    }
}
