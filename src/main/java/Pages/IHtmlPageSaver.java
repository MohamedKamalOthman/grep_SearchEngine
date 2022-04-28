package Pages;

import org.jsoup.nodes.Document;

public interface IHtmlPageSaver {
    void save (Document Doc, String Url);
}
