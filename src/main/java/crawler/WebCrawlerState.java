package crawler;

import database.DBManager;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import pages.*;

import java.io.File;
import java.util.*;

public class WebCrawlerState {
    private final int id;
    private final IUrlListHandler urlListHandler;
    private final IHtmlPageSaver htmlPageSaver;

    private final DBManager manager;

    private final ArrayList<String> finishedCrawlingUrls = new ArrayList<>();
    private final List<FetchedUrl> fetchedUrls = new ArrayList<>();

    public static boolean finished = false;

    public WebCrawlerState(int id, IUrlListHandler urlListHandler, IHtmlPageSaver htmlPageSaver, DBManager manager) {
        this.id = id;
        this.urlListHandler = urlListHandler;
        this.htmlPageSaver = htmlPageSaver;
        this.manager = manager;
    }

    public int getID() {
        return id;
    }

    public boolean isFinished(){
        if(finished)
            return true;
        synchronized (manager){
            finished = manager.isFinishedCrawling();
            return finished;
        }
    }

    public void saveFetchedUrls() {
        Map<String, HostInformation> fetchedHosts = new HashMap<>();
        FetchedUrl currentFetchedUrl;
        HostInformation currentHostInfo;

        //Serialized code
        synchronized (manager) {
            //Update crawler collection
            BulkWriteResult results = manager.saveUrls(fetchedUrls);
            if(results == null)
                return;

            //update hosts collection
            for(BulkWriteUpsert upsert : results.getUpserts()) {
                currentFetchedUrl = fetchedUrls.get(upsert.getIndex());
                currentHostInfo = fetchedHosts.getOrDefault(currentFetchedUrl.host, new HostInformation());
                currentHostInfo.fetchedCount++;

                if(currentFetchedUrl.host == null)
                    currentFetchedUrl.host = "";

                if(!currentFetchedUrl.host.isBlank() && !currentFetchedUrl.host.equals(currentFetchedUrl.parentHost)) {
                    currentHostInfo.refCount++;
                }

                fetchedHosts.putIfAbsent(currentFetchedUrl.host, currentHostInfo);
            }

            manager.incrementHosts(fetchedHosts);
        }
    }


    public void addFetchedUrl(FetchedUrl fetchedUrl) {
        fetchedUrls.add(fetchedUrl);
    }

    public void saveDocument(DocumentWrapper doc, String url) {
        htmlPageSaver.save(doc, url);
        urlListHandler.add(url);
    }

    public void refreshDocument(DocumentWrapper doc, String url){
        htmlPageSaver.save(doc, url);
    }

    public String getNextUrl() {
        return manager.priorityFetchUrl();
    }

    public void urlCrawled(String url) {
        finishedCrawlingUrls.add(url);
        if (finishedCrawlingUrls.size() >= 100)
        {
            manager.updateUrls(finishedCrawlingUrls, 2);
        }
    }

    public boolean saveUrl(String url) {
        return manager.saveUrl(url, 0);
    }

    public boolean saveUrls() {
        finished = isFinished();
        if (!finishedCrawlingUrls.isEmpty() && finished)
        {
            manager.updateUrls(finishedCrawlingUrls, 2);
        }

        return true;
    }

    public boolean urlExists(String url) {
        return manager.searchUrl(url);
    }

    public boolean docExists(long docHash) {
        return manager.docExists(docHash);
    }

    public String getUrlReCrawl(){
        String url = manager.reCrawlFetchUrl();
        if (Objects.equals(url,""))
            return "";

        //delete occurrence
        //delete paragraph
        //set indexed to false
        manager.cleanWebPageData(url);
        return url;
    }

    public void cleanWebPageData(String url){
        manager.cleanWebPageData(url);
    }

    protected void finalizeCrawling() {
        manager.updateUrls(finishedCrawlingUrls, 2);
    }
}
