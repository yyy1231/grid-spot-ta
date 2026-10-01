package com.novax.testcase;

import com.novax.util.ExcelUtil;
import com.novax.util.HttpClientUtil;
import com.novax.util.PropertyUtil;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * 过网关调/v1/market/prices/eth_usdt/adjust?
 * 服务：grid-spot-strategy-calculator
 */

public class AdjustMarketPrice {
    static Logger logger = Logger.getLogger(AdjustMarketPrice.class);

    public static void adjustPrice(String symbol, BigDecimal to, int reachTimes) {
        logger.info("推动to:" + to);
        logger.info("推动reachTimes:" + reachTimes);
        String baseUrl = PropertyUtil.getBaseUrl();
        String url = "https://" + baseUrl + "/grid-spot-strategy-calculator/v1/market/prices/" +
                symbol + "/adjust";
        HashMap<String, String> headers = new HashMap<>();
        HSSFCell token = ExcelUtil.getCell("/order/adjustMarketPrice.xls", 0, 1, 0);
        headers.put("token", token.getStringCellValue());

        HashMap<String, String> params = new HashMap<>();
        params.put("to", String.valueOf(to));
        params.put("reachTimes", String.valueOf(reachTimes));

        HttpClientUtil httpClientUtil = new HttpClientUtil();
        try {
            httpClientUtil.sendPostByForm(url, params, headers);
        } catch (Exception e) {
            logger.info("工具api，无响应数据，忽略该异常" + e);
        }
    }

    public static void main(String[] args) {
        adjustPrice("btc_usdt", new BigDecimal("60000"), 1);
    }
}
