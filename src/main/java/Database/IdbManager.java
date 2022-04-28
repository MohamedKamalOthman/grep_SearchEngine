package Database;

import org.bson.Document;

public interface IdbManager {
    public boolean savePage(String url,int hash,boolean indexed);
    public String fetchUrl();
    public boolean updateUrl(String url,int status);
    public boolean searchUrl(String url);
    public boolean saveUrl(String url,int crawled);
    public Document getUrlForIndexing();
}
