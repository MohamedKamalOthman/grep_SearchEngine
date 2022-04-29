package Database;

import org.bson.Document;

import java.util.ArrayList;

public interface IdbManager {
    public boolean savePage(String url, long docHash, boolean indexed);

    public String fetchUrl();

    public boolean updateUrl(String url, int status);

    public boolean searchUrl(String url);

    public boolean saveUrl(String url, int crawled);

    public boolean saveUrls(ArrayList<String> urls);

    public Document getUrlForIndexing();

    public boolean docExists(long docHash);

    public boolean incrementHost(String host);

    public boolean isFinishedCrawling();

    public boolean incrementHosts(ArrayList<String> hosts);

    public void insertOccurrence(String url, String value, String text_type, long location, long hash, String exactWord, String paragraph);

    public void bulkWriteIndexer();
}
