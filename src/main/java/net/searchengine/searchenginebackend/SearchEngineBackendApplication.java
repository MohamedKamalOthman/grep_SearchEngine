package net.searchengine.searchenginebackend;


import database.DBManager;
import queries.QueryProcessor;
import com.mongodb.client.*;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SearchEngineBackendApplication {
    protected MongoCollection<Document> crawler;
    protected MongoCollection<Document> queries;
    private final DBManager manager;
    private final QueryProcessor processor;
    public SearchEngineBackendApplication() {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017/");
        MongoDatabase database = mongoClient.getDatabase("SearchEngine");
        crawler = database.getCollection("crawler");
        queries = database.getCollection("queries");
        manager = new DBManager();
        processor = new QueryProcessor(manager);
    }

    @CrossOrigin("http://localhost:4200")
    @GetMapping("/api/grep/{s}")
    public List<Document> grep(@PathVariable String s) {
        queries.updateOne(new Document(),Updates.addToSet("q",s.toLowerCase()),new UpdateOptions().upsert(true));
        return processor.rankQuery(s);
    }

    @CrossOrigin("http://localhost:4200")
    @GetMapping("/api/stats")
    public Document timeElapsed() {
        return new Document().append("time",processor.getTime()).append("results",processor.getResultsAmount());
    }

    //hopefully no one finds this code
    @CrossOrigin("http://localhost:4200")
    @GetMapping("/api/prevQueries")
    public List<String> prevQueries(){
        var l = (List<String>) queries.find(new Document()).first().get("q");
        Collections.reverse(l);
        return  l;
    }

    public static void main(String[] args) {
        SpringApplication.run(SearchEngineBackendApplication.class, args);
    }
}
