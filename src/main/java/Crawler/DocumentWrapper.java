package Crawler;


import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.zip.CRC32;

public class DocumentWrapper {
    public Document doc;
    public String html;
    public long crc;

    DocumentWrapper(Document Doc) {
        doc = Doc;
        if (doc == null)
            return;
        //TODO think of a more robust way
        html = doc.text();
        byte[] bytes = html.getBytes();
        CRC32 checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        crc = checksum.getValue();
    }

    //TODO remove once test finished
    public static void main(String[] args) {
        while(true)
        {
            DocumentWrapper d1 = new DocumentWrapper(request("https://leetcode.com/tag/divide-and-conquer/"));
            System.out.println(d1.crc);
        }

    }

    private static Document request(String Url) {
        try {
            Connection con = Jsoup.connect(Url);
            Document doc = con.get();
            if (con.response().statusCode() == 200) {
                String title = doc.title();
                System.out.println("Title: " + title);
                return doc;
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}
