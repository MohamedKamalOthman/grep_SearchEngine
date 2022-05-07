package Indexer;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.HashMap;

public class HTMLParserUtilities {
    /**
     * Threshold of importance at which the current tag is treated as a full paragraph.
     */
    public static final int THRESHOLD = 3;
    public static final HashMap<String, Integer> TagImportanceMap = new HashMap<>() {
        {
            put("title", 10);
            put("h1", 9);
            put("h2", 8);
            put("h3", 7);
            put("h4", 6);
            put("h5", 5);
            put("h6", 4);
            put("p", THRESHOLD);
            put("strong", 1);
            put("em", 1);
            put("b", 1);
            put("mark", 1);
            put("i", 1);
            put("blockquote", 1);
            put("body", 1);
            put("div", 1);
            put("li", 1);
            put("ol", 2);
            put("ul", 2);
            put("pre", 1);
            put("span", 1);
            put("a", 0);
            put("article", 1);
            put("code", 1);
        }
    };

    public static String BuildStringFromNode(Node n) {
        StringBuilder SB = new StringBuilder();
        recursiveNodeBuild(n, SB);
        return SB.toString();
    }

    private static void recursiveNodeBuild(Node n, StringBuilder SB) {
        if (n instanceof TextNode tn) {
            SB.append(tn.text());
            return;
        }

        if (n instanceof Element) {
            for (Node node : n.childNodes()) {
                recursiveNodeBuild(node, SB);
            }
        }
    }
}
