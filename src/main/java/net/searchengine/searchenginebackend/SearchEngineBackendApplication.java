package net.searchengine.searchenginebackend;

import com.mongodb.client.*;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class SearchEngineBackendApplication {
    protected MongoCollection<Document> Crawler;
    public SearchEngineBackendApplication(){
        MongoClient mongoClient = MongoClients.create("mongodb://admin:pass@mongo-dev.demosfortest.com:27017/");
        MongoDatabase database = mongoClient.getDatabase("SearchEngine");
        Crawler = database.getCollection("Crawler");
    }
    @CrossOrigin("localhost:4200/")
    @GetMapping("/api/find/{s}")
    public Document find(@PathVariable int s){
        Document query = new Document().append("crawled",s);
        return Crawler.find(query).first();
    }
    public static void main(String[] args) {
//        new SearchEngineBackendApplication();
        SpringApplication.run(SearchEngineBackendApplication.class, args);
    }

}
