package com.novax.testcase;

import com.novax.entity.mysql.SpotUserAssetsEntity;
import com.novax.util.HttpClientUtil;
import com.novax.util.PropertyUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

/**
 * 不过网关调/v1/market/prices/eth_usdt/adjust?
 * 通过服务ip:port
 */

public class AdjustMarketPrice2 {
    static Logger logger = Logger.getLogger(AdjustMarketPrice.class);

    public static void adjustPrice(String symbol, BigDecimal to, int reachTimes) throws IOException {
        logger.info("推动to:" + to);
        logger.info("推动reachTimes:" + reachTimes);
        String baseUrl = PropertyUtil.getIp() + ":" + PropertyUtil.getPort();
        String url = "http://" + baseUrl + "/v1/market/prices/" + symbol + "/adjust";

        HashMap<String, String> headers = new HashMap<>();
        // 该用户用来推动行情价 保证资金充足
        Long userId = 28672914L;
        String currency = symbol.split("_")[0];

        List<SpotUserAssetsEntity> assetsEntities = UserAssetsManage.queryAssets(userId, "usdt");
        if (assetsEntities.size() == 0) {
            UserAssetsManage.createSpotAccount(userId, "usdt", BigDecimal.valueOf(100000000));
        }

        List<SpotUserAssetsEntity> assetsEntityList = UserAssetsManage.queryAssets(userId, currency);
        if (assetsEntityList.size() == 0) {
            UserAssetsManage.createSpotAccount(userId, currency, BigDecimal.valueOf(100000000));
        }

        if (assetsEntities.size() != 0 && assetsEntities.get(0).getAvailable().compareTo(BigDecimal.valueOf(100000000)) < 0) {
            UserAssetsManage.updateSpotAccount(userId, "usdt", BigDecimal.valueOf(100000000));
        }
        if (assetsEntityList.size() != 0 && assetsEntityList.get(0).getAvailable().compareTo(BigDecimal.valueOf(100000000)) < 0) {
            UserAssetsManage.updateSpotAccount(userId, currency, BigDecimal.valueOf(100000000));
        }

        headers.put("userId", String.valueOf(userId));
        headers.put("language", "en");

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

    public static void main(String[] args) throws IOException {
        adjustPrice("eth_usdt",BigDecimal.valueOf(3000),1);
    }
}
