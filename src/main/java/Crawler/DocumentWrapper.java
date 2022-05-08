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

        html = doc.html();
        String body = doc.body().text();
        byte[] bytes = body.getBytes();
        CRC32 checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        crc = checksum.getValue();
    }
}
