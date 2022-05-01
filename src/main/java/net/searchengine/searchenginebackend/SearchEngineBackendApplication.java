package net.searchengine.searchenginebackend;

import Database.IdbManager;
import Database.dbManager;
import Queries.QueryProcessor;
import com.mongodb.client.*;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.apache.catalina.Manager;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.swing.text.html.HTML;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SearchEngineBackendApplication {
    protected MongoCollection<Document> Crawler;
    protected MongoCollection<Document> Queries;
    private IdbManager Manager;
    private QueryProcessor processor;
    public SearchEngineBackendApplication() {
        MongoClient mongoClient = MongoClients.create("mongodb://admin:pass@mongo-dev.demosfortest.com:27017/");
        MongoDatabase database = mongoClient.getDatabase("SearchEngine");
        Crawler = database.getCollection("Crawler");
        Queries = database.getCollection("Queries");
        Manager = new dbManager();
        processor = new QueryProcessor(Manager);
    }

    @CrossOrigin("http://localhost:4200")
    @GetMapping("/api/find/{s}")
    public List<Document> find(@PathVariable int s) {
        Document query = new Document().append("crawled", s);
        MongoCursor<Document> cursor = Crawler.find(query).cursor();
        List<Document> docs = new ArrayList<>();
        try {
            while (cursor.hasNext()) {
                docs.add(cursor.next());
                System.out.println(docs);
            }
        } finally {
            cursor.close();
            return docs;
        }
    }

    @CrossOrigin("http://localhost:4200")
    @GetMapping("/api/GoFind/{s}")
    public List<Document> test(@PathVariable String s) {
        //TODO refactor this to dbManager
        Queries.updateOne(new Document(),Updates.addToSet("q",s.toLowerCase()),new UpdateOptions().upsert(true));
        return processor.rankQuery(s);
    }
    //hopefully no one finds this code
    @CrossOrigin("http://localhost:4200")
    @GetMapping("/api/prevQueries")
    public List<String> prevQueries(){
        //TODO refactor this to dbManager
        var l = (List<String>)Queries.find(new Document()).first().get("q");
        Collections.reverse(l);
        return  l;
    }

    public static void main(String[] args) {
        SpringApplication.run(SearchEngineBackendApplication.class, args);
    }
}
