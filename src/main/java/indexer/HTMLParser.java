package indexer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import utilities.HTMLParserUtilities;
import utilities.Stemmer;

import java.net.MalformedURLException;
import java.net.URL;

public class HTMLParser {
    private HTMLPage page;

    HTMLParser() {
        page = null;
    }

    HTMLParser(Document jsoupDocument, String url, long hash) {
        page = new HTMLPage(jsoupDocument, url, hash);
    }

    public void setPage(Document jsoupDocument, String url, long hash) {
        page = new HTMLPage(jsoupDocument, url, hash);
    }

    public void setPage(HTMLPage htmlPage) {
        page = htmlPage;
    }

    /**
     * parses current html webpage into indexed stemmed words.
     * @return parsed webpage.
     */
    public HTMLPage parse() {
        parseTitle();
        parseBody(page.doc.body(), page.doc.body().tagName());
        return page;
    }

    /**
     * Parse webpage title.
     * If no title is available in html document it will default to host name.
     */
    private void parseTitle() {
        var titles = page.doc.head().select("title");
        if(!titles.isEmpty()) {
            try {
                page.title = titles.first().ownText().strip();
            }
            catch (NullPointerException ex) {
                try {
                    page.title = new URL(page.url).getHost();
                } catch (MalformedURLException e) {
                    page.title = "";
                }
            }
        }
        else {
            try {
                page.title = new URL(page.url).getHost();
            } catch (MalformedURLException e) {
                page.title = "";
            }
        }
    }

    /**
     * Parse html webpage body.
     * Recursively parses each element in webpage.
     * @param n Current node to parse
     * @param parentTag Best parent tag based on importance
     */
    private void parseBody(Node n, String parentTag) {
        if (n instanceof TextNode tn) {
            parseTextNode(tn, parentTag);
            return;
        }

        if (n instanceof Element e) {
            String tag = upgradeTag(e.tagName(), parentTag);
            if(HTMLParserUtilities.isParagraphTag(tag)) {
                parseTextElement(n, tag);
                return;
            }

            for (Node node : n.childNodes()) {
                parseBody(node, tag);
            }
        }
    }

    private void parseTextElement(Node e, String parentTag) {
        String paragraph = HTMLParserUtilities.buildStringFromNode(e);
        extractText(parentTag, paragraph);
    }

    private void parseTextNode(TextNode tn, String parentTag) {
        String paragraph = tn.text().strip();
        extractText(parentTag, paragraph);
    }

    /**
     * Extracts all words from paragraph and adds to them to the word list of the webpage.
     * @param parentTag Parent tag of words in this paragraph.
     * @param paragraph Paragraph text.
     */
    private void extractText(String parentTag, String paragraph) {
        String text = paragraph.replaceAll("\\p{Punct}", " ");
        if(text.isBlank())
            return;

        String[] split = text.split("\\s+");
        if(HTMLParserUtilities.getTagImportance(parentTag) < 4 && split.length <= 9) {
            page.wordCount += split.length;
            return;
        }

        for (String exactWord : split) {
            exactWord = exactWord.toLowerCase();
            String stemmed = Stemmer.stemWord(exactWord);
            if (stemmed == null || stemmed.isBlank()) {
                page.wordCount++;
                continue;
            }

            var word = new HTMLPage.Word(exactWord, stemmed, page.wordCount++, parentTag, paragraph);
            page.words.add(word);
        }
    }

    /**
     * Compares and chooses which tag is more important. Favors current tag in case of a tie.
     * @param tag Current tag.
     * @param parentTag Parent Tag.
     * @return Best tag.
     */
    private static String upgradeTag(String tag, String parentTag) {
        if(HTMLParserUtilities.getTagImportance(parentTag) > HTMLParserUtilities.getTagImportance(tag))
            return parentTag;

        return tag;
    }
}
