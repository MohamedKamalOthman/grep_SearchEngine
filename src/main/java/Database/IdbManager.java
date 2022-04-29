package Database;

import org.bson.Document;

public interface IdbManager {
    public boolean savePage(String url, long docHash, boolean indexed);

    public String fetchUrl();

    public boolean updateUrl(String url, int status);

    public boolean searchUrl(String url);

    public boolean saveUrl(String url, int crawled);

    public Document getUrlForIndexing();

    public boolean docExists(long docHash);

    public boolean incrementHost(String host);

    public void insertOccurrence(String url, String value, String text_type, long location, int hash, String exactWord, String paragraph);

    public void bulkWriteIndexer();
}
