package indexer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class HTMLParserTest {

    @Test
    void parse() throws IOException {
        HTMLPage page = null;
        long start = System.currentTimeMillis();
        Document doc = Jsoup.parse(new File("." + File.separator + "TestFiles" + File.separator + "317951010.html"), null);
        HTMLParser parser = new HTMLParser(doc, "https://en.wikipedia.org/wiki/Unit_testing", 0);
        page = parser.parse();
        System.out.println(System.currentTimeMillis() - start);

        System.out.println(page.title);

        for(var word : page.words) {
            System.out.println(word);
        }

        System.out.println(page.words.size());
    }


}