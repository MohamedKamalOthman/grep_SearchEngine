package Crawler;

import Database.IdbManager;
import Pages.IHtmlPageSaver;
import Pages.IUrlListHandler;
import com.panforge.robotstxt.RobotsTxt;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class WebCrawlerState {
    private final int ID;
    private final IUrlListHandler UrlListHandler;
    private final IHtmlPageSaver HtmlPageSaver;
    private static final ConcurrentHashMap<String, RobotsTxt> RobotsTxtsMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String,String> noRobotsTxtsSet = new ConcurrentHashMap<>();

    private final IdbManager Manager;

    private final ArrayList<String> FinishedCrawlingUrls = new ArrayList<>();

    public static boolean finished = false;


    WebCrawlerState(int id, IUrlListHandler urlListHandler, IHtmlPageSaver htmlPageSaver, IdbManager manager) {
        ID = id;
        UrlListHandler = urlListHandler;
        HtmlPageSaver = htmlPageSaver;
        Manager = manager;
    }
    public boolean isFinished(){
        return finished;
    }

    public boolean isUrlVisited(String URL) {
        return UrlListHandler.contains(URL);
    }

    public int getID() {
        return ID;
    }

    public void saveDocument(DocumentWrapper Doc, String Url) {
        HtmlPageSaver.save(Doc, Url);
        UrlListHandler.add(Url);
    }

    public boolean isAllowedByRobotsTxt(String HostUrl, String Url) {
        //return true if host doesn't have robots.txt
        if(noRobotsTxtsSet.contains(HostUrl))
            return true;
        RobotsTxt RobotsTxtFile = RobotsTxtsMap.get(HostUrl);
        //return the file if I already downloaded it
        if (RobotsTxtFile != null)
            return RobotsTxtFile.query(null, Url);
        try {
            URL url = new URL(HostUrl + "/robots.txt");
            URLConnection urlConnection =  url.openConnection();
            urlConnection.setConnectTimeout(1500);
            InputStream inputStream = urlConnection.getInputStream();;
            RobotsTxtFile = RobotsTxt.read(inputStream);
        } catch (IOException e) {
            noRobotsTxtsSet.put(HostUrl,"");
            e.printStackTrace();
            return true;
        }
        RobotsTxtsMap.put(HostUrl, RobotsTxtFile);
        return RobotsTxtFile.query(null, Url);
    }

    public String getNextUrl() {
        return Manager.fetchUrl();
    }

    public void urlCrawled(String url) {
        FinishedCrawlingUrls.add(url);
        if (FinishedCrawlingUrls.size() >= 100)
        {
            Manager.updateUrls(FinishedCrawlingUrls, 2);
        }
    }

    public boolean saveUrl(String url) {
        return Manager.saveUrl(url, 0);
    }

    public boolean saveUrls(ArrayList<String> urls) {
        boolean result =  Manager.saveUrls(urls);
        finished = Manager.isFinishedCrawling();
        if (!FinishedCrawlingUrls.isEmpty() && finished)
        {
            Manager.updateUrls(FinishedCrawlingUrls, 2);
        }
        return result;
    }

    public boolean urlExists(String url) {
        return Manager.searchUrl(url);
    }

    public boolean docExists(long docHash) {
        return Manager.docExists(docHash);
    }

    public void incrementHost(String host) {
        Manager.incrementHost(host);
    }

    public void incrementHosts(HashMap<String, Integer> hosts) {
        Manager.incrementHosts(hosts);
    }
}
