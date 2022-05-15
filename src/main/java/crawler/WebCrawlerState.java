package crawler;

import database.DBManager;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.bulk.BulkWriteUpsert;
import pages.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebCrawlerState {
    private final int id;
    private final IUrlListHandler urlListHandler;
    private final IHtmlPageSaver htmlPageSaver;

    private final DBManager manager;

    private final ArrayList<String> finishedCrawlingUrls = new ArrayList<>();
    private final HashMap<String, Integer> hosts = new HashMap<>();
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
        synchronized (manager){
            finished = manager.isFinishedCrawling();
            return finished;
        }
    }

    public void saveFetchedUrls() {
        List<String> fetchedUrlStrings = fetchedUrls.stream().map(FetchedUrl::getUrl).toList();
        Map<String, HostInformation> fetchedHosts = new HashMap<>();
        FetchedUrl currentFetchedUrl;
        HostInformation currentHostInfo;
        synchronized (manager) {
            BulkWriteResult results = manager.saveUrls(fetchedUrlStrings);
            if(results == null)
                return;

            for(BulkWriteUpsert upsert : results.getUpserts()) {
                currentFetchedUrl = fetchedUrls.get(upsert.getIndex());
                currentHostInfo = fetchedHosts.getOrDefault(currentFetchedUrl.host, new HostInformation());
                currentHostInfo.fetchedCount++;

                if(currentFetchedUrl.host == null)
                    currentFetchedUrl.host = "null";

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

    public String getNextUrl() {
        //if(isFinished())
            //return "";
        return manager.fetchUrl();
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

    public void incrementHost(String host) {
        manager.incrementHost(host);
    }


    public static void main(String[] args) {
        DBManager manager = new DBManager();
        // Must Start With Adding Our Start Pages For The First Run Only
        String pathName = "." + File.separator + "Files" + File.separator;
        WebCrawlerState state = new WebCrawlerState(1, new FileUrlListHandler("urls.txt"), new FileHtmlPageSaver(pathName, manager), manager);
        state.addFetchedUrl(new FetchedUrl("lalalal", null, null));
        state.addFetchedUrl(new FetchedUrl("https://codeforces.com", null, null));
        state.addFetchedUrl(new FetchedUrl("forkfork", null, null));
        state.addFetchedUrl(new FetchedUrl("https://stackoverflow.com", null, null));
        state.addFetchedUrl(new FetchedUrl("lalalal", null, null));
        state.addFetchedUrl(new FetchedUrl("lele", null, null));

        state.saveFetchedUrls();
    }
}
