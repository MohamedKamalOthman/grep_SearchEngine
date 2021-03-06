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
import static com.mongodb.client.model.Projections.*;

public class DBManager {
    protected MongoCollection<Document> pageSaver;
    protected MongoCollection<Document> crawler;
    protected MongoCollection<Document> searchIndex;
    protected MongoCollection<Document> popularity;
    protected MongoCollection<Document> testing;
    protected MongoCollection<Document> paragraphs;
    protected static List indexerBulkWrite = new ArrayList();
    protected static List paragraphBulkWrite = new ArrayList();

    public static boolean finishedCrawling = false;

    public DBManager() {
        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("SearchEngine");
        pageSaver = database.getCollection("PageSaver");
        crawler = database.getCollection("Crawler");
        searchIndex = database.getCollection("SearchIndex");
        popularity = database.getCollection("Popularity");
        MongoDatabase database1 = mongoClient.getDatabase("test");
        testing = database1.getCollection("testing");
        paragraphs = database.getCollection("Paragraphs");
    }

    //
    // Crawler methods
    //

    //crawler first run
    public void initializeCrawlerDB() {
        //Reset interrupted crawlers
        Document query = new Document().append("crawled", 1);
        crawler.updateMany(query, Updates.set("crawled", 0), new UpdateOptions().upsert(false));
        //set seed if not already exist
        long count = crawler.countDocuments();
        if (count >= 5100) {
            finishedCrawling = true;
        }

        if (count != 0)
            return;

        List<String> seedUrls = new ArrayList<>();
        seedUrls.add("https://en.wikipedia.org/wiki/Main_Page");
        seedUrls.add("https://codeforces.com");
        seedUrls.add("https://stackoverflow.com");
        List<FetchedUrl> seed = new ArrayList<>();
        URL url;
        for (var seedUrl : seedUrls) {
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
        if (results == null)
            return;

        //update hosts collection
        for (BulkWriteUpsert upsert : results.getUpserts()) {
            currentFetchedUrl = seed.get(upsert.getIndex());
            currentHostInfo = fetchedHosts.getOrDefault(currentFetchedUrl.host, new HostInformation());
            currentHostInfo.fetchedCount++;

            if (currentFetchedUrl.host == null)
                currentFetchedUrl.host = "";

            if (!currentFetchedUrl.host.isBlank() && !currentFetchedUrl.host.equals(currentFetchedUrl.parentHost)) {
                currentHostInfo.refCount++;
            }

            fetchedHosts.putIfAbsent(currentFetchedUrl.host, currentHostInfo);
        }

        incrementHosts(fetchedHosts);
    }

    public boolean savePage(String url, long docHash, boolean indexed) {
        Document document = new Document();
        document.append("url", url);
        pageSaver.updateOne(document, Updates.combine(Updates.set("indexed", indexed), Updates.set("hash", docHash)), new UpdateOptions().upsert(true));
        return true;
    }

    public boolean saveUrl(String url, int crawled) {
        long count = crawler.countDocuments();
        if (count >= 5100) {
            return false;
        }
        Document query = new Document().append("url", url);
        Bson updates = Updates.combine(
                Updates.setOnInsert("crawled", crawled)
        );
        UpdateOptions options = new UpdateOptions().upsert(true);

        try {
            UpdateResult result = crawler.updateOne(query, updates, options);
            return result.getUpsertedId() != null;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

    public BulkWriteResult saveUrls(Iterable<FetchedUrl> fetchedUrls) {
        if (finishedCrawling)
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
        if (!crawlerBulkWrite.isEmpty())
            try {
                var result = crawler.bulkWrite(crawlerBulkWrite);
                long count = crawler.countDocuments();
                if (count >= 5000) {
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
        long count = crawler.countDocuments(query);
        return count > 0;
    }

    public String fetchUrl() {
        Document query = new Document().append("crawled", 0);
        Bson updates = Updates.combine(
                Updates.set("crawled", 1)
        );
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(false);//if true new document will be inserted
        Bson out = crawler.findOneAndUpdate(query, updates, options);
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
        Bson out = crawler.findOneAndUpdate(query, updates, options);
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
            Document out = popularity.findOneAndUpdate(query, updates, options);
            if (out == null)
                return "";
            else
                return fetchUrl((String) out.get("host"));
        }
    }

    public String reCrawlFetchUrl() {
        //only re-crawl within an hour
        final long oneHour = 3600000L;
        Document query = new Document().append("crawled", 2);
        FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().upsert(false).sort(Sorts.ascending("date"));
        Bson updates = Updates.combine(
                Updates.set("date", new Date())
        );
        synchronized (this) {
            Document out = crawler.findOneAndUpdate(query, updates, options);
            if (out == null || (new Date().getTime()) - ((Date) out.get("date")).getTime() < oneHour)
                return "";
            else
                return (String) out.get("url");
        }
    }

    public boolean updateUrl(String url, int status) {
        Document query = new Document().append("url", url);
        Bson updates = Updates.combine(
                Updates.set("crawled", status)
        );
        UpdateOptions options = new UpdateOptions().upsert(false);
        try {
            UpdateResult result = crawler.updateOne(query, updates, options);
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
                    Updates.set("crawled", status),
                    Updates.set("date", new Date())
            );
            UpdateOptions options = new UpdateOptions().upsert(false);
            UpdateBulkWrite.add(new UpdateOneModel(query, updates, options));
        }
        try {
            var result = crawler.bulkWrite(UpdateBulkWrite);
            System.out.println("Modified document count: " + result.getModifiedCount());
            //System.out.println("Upserted id: " + result.getUpsertedId()); // only contains a value when an upsert is performed
            return true;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

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
        if (!popularityBulkWrite.isEmpty())
            try {
                var result = popularity.bulkWrite(popularityBulkWrite);
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
        return pageSaver.find(query).limit(1).first();
    }

    public boolean updateIndexStatus(long hash, boolean status) {
        Document query = new Document().append("hash", hash);
        Bson updates = Updates.combine(
                Updates.set("indexed", status)
        );
        UpdateOptions options = new UpdateOptions().upsert(false);
        try {
            UpdateResult result = pageSaver.updateOne(query, updates, options);
            return result.getModifiedCount() != 0;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

    public boolean docExists(long docHash) {
        Document where = new Document().append("hash", docHash);
        return pageSaver.countDocuments(where) > 0;
    }


    public void insertParagraph(String paragraph, long paragraphHash) {
        Document query = new Document().append("paragraph", paragraph).append("hash", paragraphHash);
        paragraphBulkWrite.add(new InsertOneModel(query));
    }

    public void insertOccurrence(String url, String value, String text_type, long location, long length, String title, long hash, String exactWord, long paragraphHash) {
        Document query = new Document().append("value", value);
        Document place = new Document().append("location", location).append("text_type", text_type).append("exactWord", exactWord).append("paragraph", paragraphHash);
        Bson updates = Updates.combine(
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
        var count = searchIndex.countDocuments(new Document().append("value", value)
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
            searchIndex.bulkWrite(indexerBulkWrite, new BulkWriteOptions().ordered(false));
        } catch (Exception me) {
            System.err.println("Unable to update due to an error: " + me);
        }
        indexerBulkWrite.clear();
    }

    public void bulkWriteParagraphs() {
        try {
            paragraphs.bulkWrite(paragraphBulkWrite, new BulkWriteOptions().ordered(false));
        } catch (Exception me) {
            System.err.println("Unable to insert due to an error: " + me);
        }
        paragraphBulkWrite.clear();
    }

    public HashMap<Long, String> findParagraphs(List<Long> hashs) {
        Bson doc = Filters.in("hash", hashs);
        HashMap<Long, String> map = new HashMap<>();
        FindIterable<Document> resultsIterable = paragraphs.find(doc);
        MongoCursor<Document> resultsCursor = resultsIterable.cursor();
        while (resultsCursor.hasNext()) {
            Document result = resultsCursor.next();
            map.put((long) result.get("hash"), (String) result.get("paragraph"));
        }
        return map;
    }

    public Document getWordDocument(String word) {
        return searchIndex.find(new Document().append("value", word)).first();
    }

    public FindIterable<Document> getMultipleWordDocument(List<String> words) {
        Bson doc = Filters.in("value", words);
        return searchIndex.find(doc);
    }

    public HashMap<String, Number> getPopularity() {
        HashMap<String, Number> map = new HashMap<>();
        FindIterable<Document> resultsIterable = popularity.find(new Document());
        MongoCursor<Document> resultsCursor = resultsIterable.cursor();
        for (MongoCursor<Document> it = resultsCursor; it.hasNext(); ) {
            Document result = it.next();
            map.put((String) result.get("host"), (Number) result.get("refCount"));
        }
        return map;
    }

    public boolean resetCrawledStatus() {
        Document query = new Document().append("crawled", 1);
        Bson updates = Updates.combine(
                Updates.set("crawled", 0)
        );
        UpdateOptions options = new UpdateOptions().upsert(false);
        try {
            UpdateResult result = crawler.updateMany(query, updates, options);
            System.out.println("Reset " + result.getModifiedCount() + " crawled status");
            return true;
        } catch (MongoException me) {
            System.err.println("Unable to update due to an error: " + me);
            return false;
        }
    }

    //call this method if crc is changed, so you can nuke all previous data related to web page
    //tested and working
    public void cleanWebPageData(String url) {
        Document query = new Document().append("url", url);
        long hash = 0L;
        /** Fetch old hash and Reset indexed to false */
        try {
            hash = (long) (pageSaver.findOneAndUpdate(query, Updates.set("indexed", false)).get("hash"));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        /** Delete occurrences */
        query = new Document();
        searchIndex.updateMany(query, Updates.unset("occurrences." + hash), new UpdateOptions().bypassDocumentValidation(true));
        /** Delete paragraph */
        paragraphs.deleteMany(Filters.where("(" + hash + " ^ (this.hash & 0xffffffff)) == 0"));
    }

    public Set<Long> getExactPhraseParagraphs(String phrase) {
        /** Search For Exact Phrase In All Paragraphs */
        Bson textFilter = Filters.text('\"' + phrase + '\"', new TextSearchOptions().caseSensitive(false));
        FindIterable<Document> result = paragraphs.find(textFilter).projection(fields(include("hash"), exclude("_id")));
        Set<Long> hashes = new HashSet<>();
        Long hash;
        for(Document doc : result) {
            try {
                hash = (Long)doc.get("hash");
            }
            catch(Exception ex) {
                continue;
            }
            hashes.add(hash);
        }
        /** Return Hashes Of Found Paragraphs */
        return hashes;
    }
}
