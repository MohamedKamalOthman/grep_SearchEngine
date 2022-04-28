package com.panforge.robotstxt;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.panforge.robotstxt.RobotsTxt;
public class testmain {
    public static void main(String[] args) throws IOException {
        InputStream robotsTxtStream = new URL("https://github.com/robots.txt").openStream();
        RobotsTxt robotsTxt = RobotsTxt.read(robotsTxtStream);
//        boolean hasAccess = robotsTxt.query(null,"/*/pulse");
        Grant grant = robotsTxt.ask(null,"/*/pulse/asdasjdhjkashdkjhaskjdhjkashdjkhasjkdjka");
        if (grant == null || grant.hasAccess()) {
            System.out.println("Allowed");
        }else System.out.println("disallowed");
    }
}
