package crawler;

public class FetchedUrl {
    public String url;
    public String parentHost;
    public String host;

    public FetchedUrl(String url, String parentHost, String host) {
        this.url = url;
        this.parentHost = parentHost;
        this.host = host;
    }

    public String getUrl() {
        return url;
    }
}
