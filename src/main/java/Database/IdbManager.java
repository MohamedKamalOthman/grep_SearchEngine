package Database;

import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;

public interface IdbManager {
    public boolean savePage(String url, long docHash, boolean indexed);

    public String fetchUrl();

    public boolean updateUrl(String url, int status);

    public boolean updateUrls(ArrayList<String> urls,int status);

    public boolean searchUrl(String url);

    public boolean saveUrl(String url, int crawled);

    public boolean saveUrls(ArrayList<String> urls);

    public Document getUrlForIndexing();
    public boolean updateIndexStatus(long hash, boolean status);

    public boolean docExists(long docHash);

    public boolean incrementHost(String host);

    public boolean isFinishedCrawling();

    public boolean incrementHosts(HashMap<String, Integer> hosts);

    public void insertOccurrence(String url, String value, String text_type, long location, long hash, String exactWord, String paragraph);

    public void bulkWriteIndexer();
}
