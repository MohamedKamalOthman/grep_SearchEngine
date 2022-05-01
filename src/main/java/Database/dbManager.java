package Database;

import Indexer.PageIndexer;
import com.mongodb.*;
import com.mongodb.BasicDBObject;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import javax.print.Doc;
import java.util.*;

import static com.mongodb.client.model.Filters.empty;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;

public class dbManager implements IdbManager {
    protected MongoCollection<Document> PageSaver;
    protected MongoCollection<Document> Crawler;
    protected MongoCollection<Document> indexerTest;
    protected MongoCollection<Document> Popularity;
    protected MongoCollection<Document> Testing;
    protected static List indexerBulkWrite = new ArrayList();

    public static boolean finishedCrawling = false;

    public dbManager() {
        MongoClient mongoClient = MongoClients.create("mongodb://admin:pass@mongo-dev.demosfortest.com:27017/");
        MongoDatabase database = mongoClient.getDatabase("SearchEngine");
        PageSaver = database.getCollection("PageSaver");
        Crawler = database.getCollection("Crawler");
        indexerTest = database.getCollection("indexer test");
        Popularity = database.getCollection("Popularity");
        MongoDatabase database1 = mongoClient.getDatabase("test");
        Testing = database1.getCollection("testing");
    }

    //region Crawler Methods
    public boolean savePage(String url, long docHash, boolean indexed) {
        Document document = new Document();
        document.append("url", url);
        document.append("hash", docHash);
        document.append("indexed", indexed);
        PageSaver.insertOne(document);
        return true;
    }

    public boolean saveUrl(String url, int crawled) {
        /*
        Document document = new Document();
        document.append("url", url);
        document.append("crawled", crawled);
        Crawler.insertOne(document);
        return true;
         */

        long count = Crawler.countDocuments();
        if (count >= 5100) {
            return false;
        }
        Document query = new Document().append("url", url);
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

    public boolean saveUrls(ArrayList<String> urls) {
        long count = Crawler.countDocuments();
        if (count >= 5100) {
            finishedCrawling = true;
            return false;
        }
        List crawlerBulkWrite = new ArrayList();

        for (String url : urls) {
            Document query = new Document().append("url", url);
            Bson updates = Updates.combine(
                    Updates.setOnInsert("crawled", 0)
            );
            UpdateOptions options = new UpdateOptions().upsert(true);
            crawlerBulkWrite.add(new UpdateOneModel(query, updates, options));
        }

        try {
            var result = Crawler.bulkWrite(crawlerBulkWrite);
            count = Crawler.countDocuments();
            if (count >= 5100) {
                finishedCrawling = true;
            }
            return result.getInsertedCount() != 0;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

    public boolean isFinishedCrawling() {
        return finishedCrawling;
    }

    public boolean searchUrl(String url) {
        Document query = new Document().append("url", url);
        long count = Crawler.countDocuments(query);
        if (count > 0)
            return true;
        else
            return false;
    }

    public String fetchUrl() {
        Document query = new Document().append("crawled", 0);
        Bson updates = Updates.combine(
                Updates.set("crawled", 1)
        );
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(false);//if true new document will be inserted
        Bson out = Crawler.findOneAndUpdate(query, updates, options);
        if (out == null)
            return "";
        else
            return (String) ((Document) out).get("url");
    }

    public boolean updateUrl(String url, int status) {
        Document query = new Document().append("url", url);
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

    public boolean updateUrls(ArrayList<String> urls, int status) {
        List UpdateBulkWrite = new ArrayList();
        for (String url : urls) {
            Document query = new Document().append("url", url);
            Bson updates = Updates.combine(
                    Updates.set("crawled", status)
            );
            UpdateOptions options = new UpdateOptions().upsert(false);
            UpdateBulkWrite.add(new UpdateOneModel(query, updates, options));
        }
        try {
            var result = Crawler.bulkWrite(UpdateBulkWrite);
            System.out.println("Modified document count: " + result.getModifiedCount());
            //System.out.println("Upserted id: " + result.getUpsertedId()); // only contains a value when an upsert is performed
            return true;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

    //endregion

    public Document getUrlForIndexing() {
        String json = """
                {
                    "indexed" : false
                }
                """;
        Document query = Document.parse(json);
        return PageSaver.find(query).limit(1).first();
    }

    public boolean updateIndexStatus(long hash, boolean status) {
        Document query = new Document().append("hash", hash);
        Bson updates = Updates.combine(
                Updates.set("indexed", status)
        );
        UpdateOptions options = new UpdateOptions().upsert(false);
        try {
            UpdateResult result = PageSaver.updateOne(query, updates, options);
            //System.out.println("Modified document count: " + result.getModifiedCount());
            //System.out.println("Upserted id: " + result.getUpsertedId()); // only contains a value when an upsert is performed
            return result.getModifiedCount() != 0;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

    public boolean docExists(long docHash) {
        Document where = new Document().append("hash", docHash);
        return PageSaver.countDocuments(where) > 0;
    }

    public boolean incrementHost(String host) {
        Document query = new Document().append("host", host);
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

    public boolean incrementHosts(HashMap<String, Integer> hosts) {
        List popularityBulkWrtie = new ArrayList();
        for (String host : hosts.keySet()) {
            Document query = new Document().append("host", host);
            Bson updates = Updates.combine(
                    Updates.inc("refCount", hosts.get(host))
            );
            UpdateOptions options = new UpdateOptions().upsert(true);
            popularityBulkWrtie.add(new UpdateOneModel(query, updates, options));
        }

        try {
            var result = Popularity.bulkWrite(popularityBulkWrtie);
            return result.getInsertedCount() != 0;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

    public void insertOccurrence(String url, String value, String text_type, long location, long length, long hash, String exactWord, String paragraph) {
        Document query = new Document().append("value", value);
//        query.append("occurrences.url",url);
//        Updates.
        Document place = new Document().append("location", location).append("text_type", text_type).append("exactWord", exactWord).append("paragraph", paragraph);
//        Document.parse("{\"location\":" + location + ",\"text_type\":\"" + text_type + "\",\"exactWord\":\"" + exactWord + "\",\"paragraph\":\"" + paragraph + "\"}")
        Bson updates = Updates.combine(
//              Updates.setOnInsert();
//              Updates.setOnInsert("value",  value),
//              Updates.set("occurrences.$.url", url),
                Updates.set("occurrences." + hash + ".url", url),
                Updates.addToSet("occurrences." + hash + ".places", place),
                Updates.inc("occurrences." + hash + ".total_count." + text_type, 1),
                Updates.inc("occurrences." + hash + ".term_frequency", 1),
                Updates.set("occurrences." + hash + ".length", length)
        );
        UpdateOptions options = new UpdateOptions().upsert(true);
        indexerBulkWrite.add(new UpdateOneModel(query, updates, options));
    }

    public void insertOccurrenceTest(String url, String value, String text_type, long location, long hash, String exactWord, String paragraph) {

        // First Stage
        Document queryForWord = new Document().append("value", value);

        Bson updateForNewWord = Updates.combine(
                Updates.setOnInsert("occurrences", Arrays.asList())
        );
        UpdateOptions NewWordOptions = new UpdateOptions().upsert(true);

        indexerBulkWrite.add(new UpdateOneModel(queryForWord, updateForNewWord, NewWordOptions));

        // Second Stage
        var count = indexerTest.countDocuments(new Document().append("value", value)
                .append("occurrences.url", url));
        System.out.println(count);
        if (count == 0) {
            Document queryForOccurrence = new Document().append("value", value);

            Document emptyOccurrence = new Document().append("url", url);

            Bson updateForEmptyOccurrence = Updates.combine(
                    Updates.addToSet("occurrences", emptyOccurrence)
            );

            UpdateOptions EmptyOccurrenceOptions = new UpdateOptions().upsert(false);

            indexerBulkWrite.add(new UpdateOneModel(queryForOccurrence, updateForEmptyOccurrence, EmptyOccurrenceOptions));
        }

        // Third Stage
        Document queryForPlace = new Document().append("value", value).append("occurrences.url", url);

        Document place = new Document().append("location", location).append("text_type", text_type).append("exactWord", exactWord).append("paragraph", paragraph);
        Bson updates = Updates.combine(
                Updates.addToSet("occurrences.$.places", place)
        );
        UpdateOptions options = new UpdateOptions().upsert(false);

        indexerBulkWrite.add(new UpdateOneModel(queryForPlace, updates, options));

    }

    public void bulkWriteIndexer() {
        try {
            indexerTest.bulkWrite(indexerBulkWrite);
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
        }
        indexerBulkWrite.clear();
    }

    public Document getWordDocument(String word){
        var result =  indexerTest.find(new Document().append("value",word)).first();
        return result;
    }

    public int getPopularity(String host){
        var result = Popularity.find(new Document().append("host",host)).first();
        if(result == null)
            return 1;
        return (int)result.get("refCount");
    }

    public void Test()
    {
        var result = indexerTest.find(eq("value","programmings")).first();
        Document occurrences = (Document) result.get("occurrences");
        for (String field : occurrences.keySet())
        {
            System.out.println(field + " " + occurrences.get(field));
        }
    }


    //for testing only!
    public static void main(String[] args) {
        dbManager d = new dbManager();
//        d.insertOccurrence("https://www.stackoverflowss.com","programmings","header",1,-2561, "programming","paragraph");
//        d.insertOccurrence("https://www.stackoverflow.com","programmings","header",5,-236, "programming","paragraph");
//        d.insertOccurrence("https://www.stackoverflowe.com","programmings","header",78,-86, "programming","paragraph");
//        d.bulkWriteIndexer();
        //  d.SetNormalizedTermFrequency();
        d.Test();
    }
}
