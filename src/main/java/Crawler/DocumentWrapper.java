package Crawler;


import org.jsoup.nodes.Document;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class DocumentWrapper {
    public Document doc;
    public String html;
    public long crc;

    DocumentWrapper(Document Doc)
    {
        doc = Doc;
        if(doc == null)
            return;

        html = doc.html();
        byte[] bytes = html.getBytes();
        CRC32 checksum = new CRC32();
        checksum.update(bytes, 0, bytes.length);
        crc = checksum.getValue();
    }
}
