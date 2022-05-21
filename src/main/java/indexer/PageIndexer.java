package indexer;

import database.DBManager;
import org.bson.Document;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;

/** Questions
 * TF-IDF + explain schema *air quotes*
 * indexer input from crawler -> html pages , db
 * time = 500 * 20 mins on main thread
 * no runtime errors found that i know of
 */


public class PageIndexer {
    private final DBManager manager;
    private final String pathName;
    private final HTMLParser htmlParser = new HTMLParser();

    /**
     * Constructor
     * @param pathName
     * @param manager
     */
    public PageIndexer(String pathName, DBManager manager) {
        this.manager = manager;
        this.pathName = pathName;
    }
    /**
     * index a page
     * @param page
     */
    public void indexPage(HTMLPage page) {
        htmlParser.setPage(page);
        page = htmlParser.parse();

        /** parse each word in document and then add in a list to bulkWrite later */
        for(HTMLPage.Word word : page.words) {
            long paragraphHash = page.crcHash;
            paragraphHash += (word.paragraphID << 32);
            manager.insertOccurrence(page.url, word.stemmedWord, word.tag, word.position, page.wordCount, page.title, page.crcHash, word.exactWord, paragraphHash);
        }

        /** paragraph hash upper 32 bit is paragraph count in page lower 32 bit is html page crc -> hash */
        long i = 1;
        for(String paragraph : page.paragraphs) {
            long paragraphHash = page.crcHash;
            paragraphHash += (i << 32);
            ++i;
            manager.insertParagraph(paragraph, paragraphHash);
        }
        /**
         * update that a page is indexed in PageSaver Collection .
         * write the results in SearchIndex collection.
         * save paragraphs in Paragraphs collection.
         * */
        manager.updateIndexStatus(page.crcHash, true);
        manager.bulkWriteIndexer();
        manager.bulkWriteParagraphs();
    }

    public void indexAll() {
        /**
         * keep indexing while no more pages left in PageSaver database
         */
        while (true) {
            /** gets a url with indexed == false in the db*/
            Document doc = manager.getUrlForIndexing();
            /** if no more left exit jop is done */
            if (doc == null) {
                return;
            }
            /** retrieve data correctly */
            long hash = (long) doc.get("hash");
            String url = (String) doc.get("url");
            /** crc -> hash is the name of the file */
            File file = new File(pathName + hash + ".html");
            HTMLPage page;
            try {
                page = new HTMLPage(Jsoup.parse(file, null), url, hash);
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("Error JSOUP parsing webpage");
                continue;
            }
            /** index the page */
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