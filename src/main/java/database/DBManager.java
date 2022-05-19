package database;

import com.mongodb.*;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import com.mongodb.client.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import crawler.FetchedUrl;
import crawler.HostInformation;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class DBManager {
    protected MongoCollection<Document> PageSaver;
    protected MongoCollection<Document> Crawler;
    protected MongoCollection<Document> SearchIndex;
    protected MongoCollection<Document> Popularity;
    protected MongoCollection<Document> Testing;
    protected MongoCollection<Document> Paragraphs;
    protected static List indexerBulkWrite = new ArrayList();
    protected static List paragraphBulkWrite = new ArrayList();

    public static boolean finishedCrawling = false;

    public DBManager() {
//      MongoClient mongoClient = MongoClients.create("mongodb://admin:pass@mongo-dev.demosfortest.com:27017/");
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("SearchEngine");
        PageSaver = database.getCollection("PageSaver");
        Crawler = database.getCollection("Crawler");
        SearchIndex = database.getCollection("SearchIndex");
        Popularity = database.getCollection("Popularity");
        MongoDatabase database1 = mongoClient.getDatabase("test");
        Testing = database1.getCollection("testing");
        Paragraphs = database.getCollection("Paragraphs");
    }

    //
    // Crawler methods
    //

    //crawler first run
    public void initializeCrawlerDB(){
        //Reset interrupted crawlers
        Document query = new Document().append("crawled",1);
        Crawler.updateMany(query,Updates.set("crawled",0),new UpdateOptions().upsert(false));
        //set seed if not already exist
        long count = Crawler.countDocuments();
        if (count >= 5100) {
            finishedCrawling = true;
        }

        if(count != 0)
            return;

        List<String> seedUrls = new ArrayList<>();
        seedUrls.add("https://en.wikipedia.org/wiki/Main_Page");
        seedUrls.add("https://codeforces.com");
        seedUrls.add("https://stackoverflow.com");
        List<FetchedUrl> seed = new ArrayList<>();
        URL url;
        for(var seedUrl : seedUrls) {
            try {
                 url = new URL(seedUrl);
            } catch (MalformedURLException e) {
                continue;
            }

            seed.add(new FetchedUrl(url.toString(), null, url.getHost()));
        }

        Map<String, HostInformation> fetchedHosts = new HashMap<>();
        FetchedUrl currentFetchedUrl;
        HostInformation currentHostInfo;
        //Update crawler collection
        BulkWriteResult results = saveUrls(seed);
        if(results == null)
            return;

        //update hosts collection
        for(BulkWriteUpsert upsert : results.getUpserts()) {
            currentFetchedUrl = seed.get(upsert.getIndex());
            currentHostInfo = fetchedHosts.getOrDefault(currentFetchedUrl.host, new HostInformation());
            currentHostInfo.fetchedCount++;

            if(currentFetchedUrl.host == null)
                currentFetchedUrl.host = "";

            if(!currentFetchedUrl.host.isBlank() && !currentFetchedUrl.host.equals(currentFetchedUrl.parentHost)) {
                currentHostInfo.refCount++;
            }

            fetchedHosts.putIfAbsent(currentFetchedUrl.host, currentHostInfo);
        }

        incrementHosts(fetchedHosts);
    }
    public boolean savePage(String url, long docHash, boolean indexed) {
        Document document = new Document();
        document.append("url", url);
        PageSaver.updateOne(document,Updates.combine(Updates.set("indexed", indexed), Updates.set("hash", docHash)), new UpdateOptions().upsert(true));
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

    public BulkWriteResult saveUrls(Iterable<FetchedUrl> fetchedUrls) {
        if(finishedCrawling)
            return null;

        List<UpdateOneModel<Document>> crawlerBulkWrite = new ArrayList<>();

        for (FetchedUrl fetchedUrl : fetchedUrls) {
            Document query = new Document().append("url", fetchedUrl.url);
            Bson updates = Updates.combine(
                    Updates.setOnInsert("host", fetchedUrl.host),
                    Updates.setOnInsert("crawled", 0)
            );
            UpdateOptions options = new UpdateOptions().upsert(true);
            crawlerBulkWrite.add(new UpdateOneModel<>(query, updates, options));
        }
        if(!crawlerBulkWrite.isEmpty())
            try {
                var result = Crawler.bulkWrite(crawlerBulkWrite);
                long count = Crawler.countDocuments();
                if (count >= 600) {
                    finishedCrawling = true;
                }
                return result;
            } catch (MongoException me) {
                System.err.println("Unable to update due to an error: " + me);
                return null;
            }
        return null;
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

    public String fetchUrl(String host) {

        Document query = new Document().append("crawled", 0).append("host", host);
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

    public String priorityFetchUrl() {
        Bson query = Filters.where("this.fetchedCount != this.crawledCount");
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(false).sort(Sorts.ascending("crawledCount"));
        Bson updates = Updates.combine(
                Updates.inc("crawledCount", 1)
        );
        synchronized (this) {
            Document out = Popularity.findOneAndUpdate(query, updates, options);
            if (out == null)
                return "";
            else
                return fetchUrl((String) out.get("host"));
        }
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

    /*public boolean incrementHost(String host) {
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
    }*/

    public boolean incrementHosts(Map<String, HostInformation> hosts) {
        List<UpdateOneModel<Document>> popularityBulkWrite = new ArrayList<>();
        for (var host : hosts.entrySet()) {
            Document query = new Document().append("host", host.getKey());
            Bson updates = Updates.combine(
                    Updates.inc("fetchedCount", host.getValue().fetchedCount),
                    Updates.setOnInsert("crawledCount", 0),
                    Updates.inc("refCount", host.getValue().refCount)
            );
            UpdateOptions options = new UpdateOptions().upsert(true);
            popularityBulkWrite.add(new UpdateOneModel<>(query, updates, options));
        }
        if(!popularityBulkWrite.isEmpty())
            try {
                var result = Popularity.bulkWrite(popularityBulkWrite);
                return result.getInsertedCount() != 0;
            } catch (MongoException | IllegalArgumentException me) {
                System.err.println("Unable to update due to an error: " + me);
                return false;
            }
        return false;
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



    public void insertParagraph(String paragraph, long paragraphHash) {
        Document query = new Document().append("paragraph", paragraph).append("hash", paragraphHash);
        paragraphBulkWrite.add(new InsertOneModel(query));
    }

    public void insertOccurrence(String url, String value, String text_type, long location, long length, String title,long hash, String exactWord, long paragraphHash) {
        Document query = new Document().append("value", value);
//        query.append("occurrences.url",url);
//        Updates.
        Document place = new Document().append("location", location).append("text_type", text_type).append("exactWord", exactWord).append("paragraph", paragraphHash);
//        Document.parse("{\"location\":" + location + ",\"text_type\":\"" + text_type + "\",\"exactWord\":\"" + exactWord + "\",\"paragraph\":\"" + paragraph + "\"}")
        Bson updates = Updates.combine(
//              Updates.setOnInsert();
//              Updates.setOnInsert("value",  value),
//              Updates.set("occurrences.$.url", url),
                Updates.set("occurrences." + hash + ".url", url),
                Updates.addToSet("occurrences." + hash + ".places", place),
                Updates.inc("occurrences." + hash + ".total_count." + text_type, 1),
                Updates.inc("occurrences." + hash + ".term_frequency", 1),
                Updates.set("occurrences." + hash + ".length", length),
                Updates.set("occurrences." + hash + ".title", title)
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
        var count = SearchIndex.countDocuments(new Document().append("value", value)
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
            SearchIndex.bulkWrite(indexerBulkWrite, new BulkWriteOptions().ordered(false));
        } catch (Exception me) {
            System.err.println("Unable to update due to an error: " + me);
        }
        indexerBulkWrite.clear();
    }

    public void bulkWriteParagraphs() {
        try {
            Paragraphs.bulkWrite(paragraphBulkWrite, new BulkWriteOptions().ordered(false));
        } catch (Exception me) {
            System.err.println("Unable to insert due to an error: " + me);
        }
        paragraphBulkWrite.clear();
    }
    public  HashMap<Long,String> findParagraphs(List<Long> hashs){
        Bson doc = Filters.in("hash", hashs);
        HashMap<Long,String> map = new HashMap<>();
        FindIterable<Document> resultsIterable = Paragraphs.find(doc);
        MongoCursor<Document> resultsCursor = resultsIterable.cursor();
        while( resultsCursor.hasNext() ) {
            Document result = resultsCursor.next();
            map.put((long)result.get("hash"),(String)result.get("paragraph"));
        }
        return map;
    }
    public Document getWordDocument(String word){
        return SearchIndex.find(new Document().append("value",word)).first();
    }

    public FindIterable<Document> getMultipleWordDocument(List<String> words) {
        Bson doc = Filters.in("value", words);
        return SearchIndex.find(doc);
    }

    public HashMap<String,Number> getPopularity(){
        HashMap<String,Number> map = new HashMap<>();
        FindIterable<Document> resultsIterable = Popularity.find(new Document());
        MongoCursor<Document> resultsCursor = resultsIterable.cursor();
        for (MongoCursor<Document> it = resultsCursor; it.hasNext(); ) {
            Document result = it.next();
            map.put((String)result.get("host"),(Number)result.get("refCount"));
        }
        return map;
    }

    public void Test()
    {
        var result = SearchIndex.find(eq("value","programmings")).first();
        Document occurrences = (Document) result.get("occurrences");
        for (String field : occurrences.keySet())
        {
            System.out.println(field + " " + occurrences.get(field));
        }
    }

    public boolean resetCrawledStatus() {
        Document query = new Document().append("crawled", 1);
        Bson updates = Updates.combine(
                Updates.set("crawled", 0)
        );
        UpdateOptions options = new UpdateOptions().upsert(false);
        try {
            UpdateResult result = Crawler.updateMany(query, updates, options);
            System.out.println("Reset " + result.getModifiedCount() + " crawled status");
            return true;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

    //for testing only!
    public static void main(String[] args) {
        DBManager d = new DBManager();
//        d.insertOccurrence("https://www.stackoverflowss.com","programmings","header",1,-2561, "programming","paragraph");
//        d.insertOccurrence("https://www.stackoverflow.com","programmings","header",5,-236, "programming","paragraph");
//        d.insertOccurrence("https://www.stackoverflowe.com","programmings","header",78,-86, "programming","paragraph");
//        d.bulkWriteIndexer();
        //  d.SetNormalizedTermFrequency();
        d.Test();
    }
}
