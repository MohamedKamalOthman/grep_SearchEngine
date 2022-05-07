package Indexer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import static Indexer.PageIndexer.stemWord;

public class HTMLParser {
    private HTMLPage page;

    HTMLParser(Document JSOUPDocument, String Url) {
        page = new HTMLPage(JSOUPDocument, Url);
    }

    public void setPage(Document JSOUPDocument, String Url) {
        page = new HTMLPage(JSOUPDocument, Url);
    }

    public HTMLPage parse() {
        parseTitle();
        parseBody(page.doc.body(), page.doc.body().tagName(), page.doc.body().tagName());
        return page;
    }

    private void parseTitle() {
        var titles = page.doc.head().select("title");
        page.title = "";
        if(titles.size() > 0) {
            try {
                page.title = titles.first().ownText().strip();
            }
            catch (NullPointerException ex) {
                page.title = "";
            }
        }
    }

    private void parseBody(Node n, String ParentTag, String CurrentTag) {
        if (n instanceof TextNode tn) {
            parseText(tn, ParentTag, CurrentTag);
            return;
        }

        if (n instanceof Element e) {
            String ElementTag = e.tagName();

            //if (HTMLParserUtilities.TagImportanceMap.get(ElementTag) == null) {
            //    return;
            //}

            for (Node node : n.childNodes()) {
                parseBody(node, upgradeTag(ElementTag, ParentTag), ElementTag);
            }
        }
    }

    private void parseText(TextNode tn, String ParentTag, String CurrentTag) {
        String text = tn.text().strip().replaceAll("\\p{Punct}", " ");
        if(text.isBlank())
            return;

        String[] split = text.split("\\s+");
        for (String exactWord : split) {
            exactWord = exactWord.toLowerCase();
            String stemmed = stemWord(exactWord);
            if (stemmed == null || stemmed.isBlank()) {
                page.wordCount++;
                continue;
            }

            var word = new HTMLPage.Word(exactWord, stemmed, page.wordCount++, ParentTag);
            //System.out.println(word);
            page.words.add(word);
        }
    }
    private static String upgradeTag(String Tag, String ParentTag) {
        int CurrentTagImportance = HTMLParserUtilities.TagImportanceMap.getOrDefault(Tag, -1);
        int ParentTagImportance = HTMLParserUtilities.TagImportanceMap.getOrDefault(ParentTag, -1);
        if(ParentTagImportance > CurrentTagImportance)
            return ParentTag;
        else
            return Tag;
    }
}
