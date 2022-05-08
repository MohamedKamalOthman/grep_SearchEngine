package Indexer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.net.MalformedURLException;
import java.net.URL;

import static Indexer.PageIndexer.stemWord;

public class HTMLParser {
    private HTMLPage page;

    HTMLParser() {
        page = null;
    }

    HTMLParser(Document JSOUPDocument, String Url, long hash) {
        page = new HTMLPage(JSOUPDocument, Url, hash);
    }

    public void setPage(Document JSOUPDocument, String Url, long hash) {
        page = new HTMLPage(JSOUPDocument, Url, hash);
    }
    public void setPage(HTMLPage Page) {
        page = Page;
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
        if(titles.size() > 0) {
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
     * @param ParentTag Best parent tag based on importance
     */
    private void parseBody(Node n, String ParentTag) {
        if (n instanceof TextNode tn) {
            parseTextNode(tn, ParentTag);
            return;
        }

        if (n instanceof Element e) {
            String tag = upgradeTag(e.tagName(), ParentTag);
            if(HTMLParserUtilities.TagImportanceMap.getOrDefault(tag, 0) >= HTMLParserUtilities.THRESHOLD) {
                parseTextElement(n, tag);
                return;
            }

            for (Node node : n.childNodes()) {
                parseBody(node, tag);
            }
        }
    }

    private void parseTextElement(Node e, String ParentTag) {
        String paragraph = HTMLParserUtilities.BuildStringFromNode(e);
        extractText(ParentTag, paragraph);
    }

    private void parseTextNode(TextNode tn, String ParentTag) {
        String paragraph = tn.text().strip();
        extractText(ParentTag, paragraph);
    }

    /**
     * Extracts all words from paragraph and adds to them to the word list of the webpage.
     * @param ParentTag Parent tag of words in this paragraph.
     * @param paragraph Paragraph text.
     */
    private void extractText(String ParentTag, String paragraph) {
        String text = paragraph.replaceAll("\\p{Punct}", " ");
        if(text.isBlank())
            return;

        String[] split = text.split("\\s+");
        if(HTMLParserUtilities.TagImportanceMap.getOrDefault(ParentTag, 0) < 4 && split.length <= 9) {
            page.wordCount += split.length;
            return;
        }

        for (String exactWord : split) {
            exactWord = exactWord.toLowerCase();
            String stemmed = stemWord(exactWord);
            if (stemmed == null || stemmed.isBlank()) {
                page.wordCount++;
                continue;
            }

            var word = new HTMLPage.Word(exactWord, stemmed, page.wordCount++, ParentTag, paragraph);
            page.words.add(word);
        }
    }

    /**
     * Compares and chooses which tag is more important. Favors current tag in case of a tie.
     * @param Tag Current tag.
     * @param ParentTag Parent Tag.
     * @return Best tag.
     */
    private static String upgradeTag(String Tag, String ParentTag) {
        int CurrentTagImportance = HTMLParserUtilities.TagImportanceMap.getOrDefault(Tag, -1);
        int ParentTagImportance = HTMLParserUtilities.TagImportanceMap.getOrDefault(ParentTag, -1);
        if(ParentTagImportance > CurrentTagImportance)
            return ParentTag;
        else
            return Tag;
    }
}
