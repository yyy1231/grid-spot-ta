package com.novax.util;

import com.mongodb.BasicDBObject;
import com.mongodb.client.*;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MongoUtils {
    static Logger logger = Logger.getLogger(MongoUtils.class);


    public static MongoCollection<Document> getMongoConnection(String database, String collection) {
        String host = PropertyUtil.getMongoHost();
        String port = PropertyUtil.getMongoPort();
        String username = PropertyUtil.getMongoUser();
        String password = PropertyUtil.getMongoPassword();
        String connectionString = "mongodb://" + username + ":" + password + "@" + host + ":" + port;
        MongoClient mongoClient = MongoClients.create(connectionString);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
        logger.info("Connected to the database successfully");
        return mongoCollection;
    }

    public static List<String> getRows(MongoCollection<Document> collection,BasicDBObject query){
        FindIterable<Document> result = collection.find(query);
        ArrayList<String> rows = new ArrayList<>();
        for (Document document:result) {
            rows.add(document.toJson());
        }
        return rows;
    }

}
