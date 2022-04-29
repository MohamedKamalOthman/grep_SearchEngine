package Database;

import com.mongodb.*;
import com.mongodb.BasicDBObject;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import javax.print.Doc;
import java.util.Iterator;

import static com.mongodb.client.model.Filters.eq;

public class dbManager implements IdbManager {
    protected MongoCollection<Document> PageSaver;
    protected MongoCollection<Document> Crawler;
    protected MongoCollection<Document> indexerTest;
    protected MongoCollection<Document> Popularity;


    public dbManager(){
        MongoClient mongoClient = MongoClients.create("mongodb://admin:pass@mongo-dev.demosfortest.com:27017/");
        MongoDatabase database = mongoClient.getDatabase("SearchEngine");
        PageSaver = database.getCollection("PageSaver");
        Crawler = database.getCollection("Crawler");
        indexerTest = database.getCollection("indexer test");
        Popularity = database.getCollection("Popularity");
    }

    //region Crawler Methods
    public boolean savePage(String url,long docHash,boolean indexed){
        Document document = new Document();
        document.append("url", url);
        document.append("hash", docHash);
        document.append("indexed", indexed);
        PageSaver.insertOne(document);
        return true;
    }
    public boolean saveUrl(String url,int crawled){
        /*
        Document document = new Document();
        document.append("url", url);
        document.append("crawled", crawled);
        Crawler.insertOne(document);
        return true;
         */
        Document query = new Document().append("url",  url);
        Bson updates = Updates.combine(
                Updates.setOnInsert("crawled", crawled)
        );
        UpdateOptions options = new UpdateOptions().upsert(true);
        try {
            UpdateResult result = Crawler.updateOne(query, updates, options);
            return result.getUpsertedId() != null;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }
    public boolean searchUrl(String url){
        Document query = new Document().append("url", url);
        long count = Crawler.countDocuments(query);
        if(count > 0)
            return true;
        else
            return false;
    }
    public String fetchUrl(){
        Document query = new Document().append("crawled",0);
        Bson updates = Updates.combine(
                Updates.set("crawled",1)
        );
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(false);//if true new document will be inserted
        Bson out = Crawler.findOneAndUpdate(query,updates,options);
        if(out==null)
            return "";
        else
            return (String)((Document)out).get("url");
    }
    public boolean updateUrl(String url,int status){
        Document query = new Document().append("url",  url);
        Bson updates = Updates.combine(
                Updates.set("crawled", status)
                );
        UpdateOptions options = new UpdateOptions().upsert(false);
        try {
            UpdateResult result = Crawler.updateOne(query, updates, options);
            System.out.println("Modified document count: " + result.getModifiedCount());
            System.out.println("Upserted id: " + result.getUpsertedId()); // only contains a value when an upsert is performed
            return true;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }
    //endregion

    public Document getUrlForIndexing()
    {
        String json = """
                {
                    "indexed" : false
                }
                """;
        Document query = Document.parse(json);
        return PageSaver.find(query).limit(1).first();
    }

    public boolean docExists(long docHash) {
        Document where = new Document().append("hash", docHash);
        return PageSaver.countDocuments(where) > 0;
    }

    public boolean incrementHost(String host) {
        Document query = new Document().append("host",  host);
        Bson updates = Updates.combine(
                Updates.inc("refCount", 1)
                );
        UpdateOptions options = new UpdateOptions().upsert(true);
        try {
            UpdateResult result = Popularity.updateOne(query, updates, options);
            System.out.println("Modified document count: " + result.getModifiedCount());
            System.out.println("Upserted id: " + result.getUpsertedId()); // only contains a value when an upsert is performed
            return true;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

    public void insertOccurrence(String url,String value,String text_type,long location,int hash){
        Document query = new Document().append("value",  value);
//        query.append("occurrences.url",url);
//        Updates.
        Bson updates = Updates.combine(
//                Updates.setOnInsert();
//                Updates.setOnInsert("value",  value),
//                Updates.set("occurrences.$.url", url),
                Updates.set("occurrences."+hash+".url", url),
                Updates.addToSet("occurrences."+hash+".places", Document.parse( "{\"location\":"+location+",\"text_type\":\""+text_type+"\"}")),
                Updates.inc("occurrences."+hash+".total_count."+text_type,1)
        );
        UpdateOptions options = new UpdateOptions().upsert(true);
        try {
            UpdateResult result = indexerTest.updateOne(query,updates, options);
//            return true;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
//            return false;
        }
    }
//for testing only!
    public static void main(String[] args) {
        dbManager d = new dbManager();
        d.insertOccurrence("https://www.stackoverflows.com","programmings","header",2576,-2561);
    }
}
