package Pages;

import Crawler.DocumentWrapper;
import org.jsoup.nodes.Document;

public interface IHtmlPageSaver {
    void save(DocumentWrapper Doc, String Url);
}
