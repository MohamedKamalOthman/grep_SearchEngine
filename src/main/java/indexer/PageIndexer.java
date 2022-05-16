package indexer;

import database.DBManager;
import org.bson.Document;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;

public class PageIndexer {
    private final DBManager manager;
    private final String pathName;
    private final HTMLParser htmlParser = new HTMLParser();

    public PageIndexer(String pathName, DBManager manager) {
        this.manager = manager;
        this.pathName = pathName;
    }


    public void indexPage(HTMLPage page) {
        htmlParser.setPage(page);
        page = htmlParser.parse();

        for(HTMLPage.Word word : page.words) {
            long paragraphHash = page.crcHash;
            paragraphHash += (word.paragraphID << 32);
            manager.insertOccurrence(page.url, word.stemmedWord, word.tag, word.position, page.wordCount, page.title, page.crcHash, word.exactWord, paragraphHash);
        }

        long i = 1;
        for(String paragraph : page.paragraphs) {
            long paragraphHash = page.crcHash;
            paragraphHash += (i << 32);
            ++i;
            manager.insertParagraph(paragraph, paragraphHash);
        }

        manager.updateIndexStatus(page.crcHash, true);
        manager.bulkWriteIndexer();
        manager.bulkWriteParagraphs();
    }

    public void indexAll() {
        while (true) {
            Document doc = manager.getUrlForIndexing();
            if (doc == null) {
                return;
            }

            long hash = (long) doc.get("hash");
            String url = (String) doc.get("url");
            File file = new File(pathName + hash + ".html");
            HTMLPage page;

            try {
                page = new HTMLPage(Jsoup.parse(file, null), url, hash);
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("Error JSOUP parsing webpage");
                return;
            }

            indexPage(page);
        }
    }

    public static void main(String[] args) {
        DBManager db = new DBManager();
        PageIndexer pageIndexer = new PageIndexer("." + File.separator + "Files" + File.separator, db);
        pageIndexer.indexAll();

        System.out.println("Finished Indexing");
    }
}