package pages;

import crawler.DocumentWrapper;

public interface IHtmlPageSaver {
    void save(DocumentWrapper doc, String url);
}
