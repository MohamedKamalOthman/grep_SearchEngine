package utilities;

import com.panforge.robotstxt.RobotsTxt;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.ConcurrentHashMap;

public class RobotsTxtUtilities {
    private static final ConcurrentHashMap<String, RobotsTxt> RobotsTextsMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> noRobotsTextsSet = new ConcurrentHashMap<>();

    public static boolean isAllowedByRobotsTxt(String hostUrl, String url) {
        //return true if host doesn't have robots.txt
        if(noRobotsTextsSet.get(hostUrl) != null)
            return true;

        RobotsTxt robotsTxtFile = RobotsTextsMap.get(hostUrl);
        //return the file if I already downloaded it
        if (robotsTxtFile != null)
            return robotsTxtFile.query(null, url);

        try {
            URL connUrl = new URL(hostUrl + "/robots.txt");
            URLConnection urlConnection = connUrl.openConnection();
            urlConnection.setConnectTimeout(1500);
            InputStream inputStream = urlConnection.getInputStream();
            robotsTxtFile = RobotsTxt.read(inputStream);
        } catch (Exception e) {
            noRobotsTextsSet.put(hostUrl,"");
            return true;
        }

        RobotsTextsMap.put(hostUrl, robotsTxtFile);
        return robotsTxtFile.query(null, url);
    }

    private RobotsTxtUtilities() {
        throw new IllegalStateException("Utility class RobotsTxtUtilities instantiated");
    }
}
