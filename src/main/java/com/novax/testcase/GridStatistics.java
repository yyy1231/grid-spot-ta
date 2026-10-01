package com.novax.testcase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.novax.entity.model.ArbitrageGroupModel;
import com.novax.entity.model.GridArbitrageModel;
import com.novax.entity.mysql.GridSpotOrderEntity;
import com.novax.entity.mysql.GridSpotStrategyEntity;
import com.novax.entity.mysql.GridSpotUserAssetsSnapshotEntity;
import com.novax.entity.mysql.SpotOrderEntity;
import com.novax.mapper.*;
import com.novax.util.MongoUtils;
import com.novax.util.MyBatisUtil;
import com.novax.util.PropertyUtil;
import org.bson.Document;
import org.testng.ITestContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GridStatistics {
    // 查网格
    public static GridSpotOrderEntity queryGrid(Long id) {
        return MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectByPrimaryKey(id));
    }

    // 查网格策略
    public static GridSpotStrategyEntity queryGridStrategy(Long gridId) {
        return MyBatisUtil.execute(GridSpotStrategyEntityMapper.class, m -> m.selectByGridSpotOrderId(gridId));
    }

    // 运行中网格
    public static List<GridSpotOrderEntity> queryGridsOnRunning(Long userId) {
        return MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectGridsOnRunning(userId));
    }

    // 网格
    public static List<GridSpotOrderEntity> queryAllGrids(Long userId, Integer status) {
        return MyBatisUtil.execute(GridSpotOrderEntityMapper.class, m -> m.selectAllGrids(userId, status));
    }

    // 查网格交易中委托买单
    public static List<SpotOrderEntity> queryInTradingGridOrders(String symbol, Long gridId, Integer tradeType) {
        return MyBatisUtil.execute(SpotOrderEntityMapper.class, m -> m.selectInTradingGridOrders(symbol, gridId, tradeType));
    }

    // 查某市场交易中委托 现货
    public static List<SpotOrderEntity> queryInTradingOrders(String symbol) {
        return MyBatisUtil.execute(SpotOrderEntityMapper.class, m -> m.selectInTradingOrders(symbol));
    }

    // 网格初始建仓数量
    public static List<SpotOrderEntity> queryInitialPositionsNum(Long userId, Long gridId) {
        return MyBatisUtil.execute(SpotOrderEntityMapper.class, m -> m.selectInitialOrders(userId, gridId));
    }

    // 网格初始委托
    public static List<SpotOrderEntity> queryInitialEntrust(Long userId, Long gridId, int tradeType) {
        return MyBatisUtil.execute(SpotOrderEntityMapper.class, m -> m.selectInitialEntrust(userId, gridId, tradeType));
    }

    // 委托单手续费
    public static BigDecimal queryOrderFee(Long orderId) {
        return MyBatisUtil.execute(GridSpotOrderTransactionRecordEntityMapper.class, m -> m.selectOrderFee(orderId));
    }

    // 网格套利总金额
    public static BigDecimal queryGridArbitrageAmount(Long gridId) {
        String database = PropertyUtil.getMongoDataBase();
        MongoCollection<Document> collection = MongoUtils.getMongoConnection(database, "grid_spot_order_arbitrage");

        BasicDBObject query = new BasicDBObject();
        query.put("grid_spot_order_id", gridId);
        List<String> rows = MongoUtils.getRows(collection, query);
        int arbitrageTimes = JSONObject.parseObject(rows.get(0)).getIntValue("number_of_times_of_arbitrage");
        BigDecimal profit = BigDecimal.ZERO;
        if (arbitrageTimes != 0) {
            profit = JSONObject.parseObject(rows.get(0)).getJSONObject("cumulative_profit_of_order").getBigDecimal("$numberDecimal");
        }
        return profit.setScale(3, RoundingMode.FLOOR);
    }

    // 网格套利组
    public static GridArbitrageModel queryGridArbitrageGroup(Long gridId) {
        String database = PropertyUtil.getMongoDataBase();
        MongoCollection<Document> collection = MongoUtils.getMongoConnection(database, "grid_spot_order_arbitrage");

        BasicDBObject query = new BasicDBObject();
        query.put("grid_spot_order_id", gridId);
        List<String> rows = MongoUtils.getRows(collection, query);
        JSONArray arbitrageGroups = JSONObject.parseObject(rows.get(0)).getJSONArray("arbitrage_groups");

        GridArbitrageModel gridArbitrageModel = new GridArbitrageModel();
        List<ArbitrageGroupModel> arbitrageGroupModelList = new ArrayList<>();
        int arbitrageTimes = 0;
        for (Object arbitrageGroup : arbitrageGroups) {
            JSONObject group = (JSONObject) JSONObject.toJSON(arbitrageGroup);
            Boolean hasBeenArbitraged = group.getBoolean("has_been_arbitraged");
            if (hasBeenArbitraged) {
                arbitrageTimes++;
                ArbitrageGroupModel arbitrageGroupModel = new ArbitrageGroupModel();
                long groupId = group.getJSONObject("group_id").getLongValue("$numberLong");
                long arbitragedTime = group.getJSONObject("arbitraged_time").getLongValue("$date");
                BigDecimal estimatedGridProfit = group.getJSONObject("estimated_grid_profit").getBigDecimal("$numberDecimal");
                arbitrageGroupModel.setGroupId(groupId);
                arbitrageGroupModel.setArbitrageTime(arbitragedTime);
                arbitrageGroupModel.setArbitrageAmount(estimatedGridProfit);
                arbitrageGroupModelList.add(arbitrageGroupModel);
            }
        }
        gridArbitrageModel.setList(arbitrageGroupModelList);
        gridArbitrageModel.setArbitrageTimes(arbitrageTimes);
        return gridArbitrageModel;
    }

    // 网格资产快照
    public static List<GridSpotUserAssetsSnapshotEntity> queryAssetSnapshot(List<Long> gridIds) {
        return MyBatisUtil.execute(GridSpotUserAssetsSnapshotEntityMapper.class, m -> m.selectAssetSnapshot(gridIds));
    }

    // 网格事件
    public static int searchGridEvent(Long orderId, String eventType) {
        String database = PropertyUtil.getMongoDataBase();
        MongoCollection<Document> collection = MongoUtils.getMongoConnection(database, "grid_spot_event");

        BasicDBObject query = new BasicDBObject();
        query.put("grid_spot_order_id", orderId);
        List<String> rows = MongoUtils.getRows(collection, query);
        int times = 0;
        for (String jsonString : rows) {
            JSONObject jsonObject = JSONObject.parseObject(jsonString);
            String event = jsonObject.getString("event_type");
            if (event.equals(eventType)) {
                times++;
            }
        }
        return times;
    }

    // 创建的网格数量
    public static int getOrderCount(ITestContext context) {
        Set<String> attributeNames = context.getAttributeNames();
        int num = 0;
        for (String attributeName : attributeNames) {
            if (attributeName.contains("orderId")) {
                num++;
            }
        }
        return num;
    }

    public static void main(String[] args) {
        // System.out.println(searchGridEvent(527086614577086464L, "GridSpotCreated"));
        System.out.println(queryGrid(527086614577086464L));
    }

}
