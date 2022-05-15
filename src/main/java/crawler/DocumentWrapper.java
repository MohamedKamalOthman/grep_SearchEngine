package crawler;


import org.jsoup.nodes.Document;

import java.util.zip.CRC32;

/**
 * Wraps a jsoup document.
 * On construction, extracts the html document into a string and calculates its crc hash.
 */
public class DocumentWrapper {
    public final Document doc;
    public final String html;
    public final long crc;

    DocumentWrapper(Document document) {
        doc = document;
        if (doc == null) {
            html = null;
            crc = 0;
            return;
        }

        html = doc.html();
        String body = doc.body().text();
        byte[] bytes = body.getBytes();
        CRC32 checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        crc = checksum.getValue();
    }
}
