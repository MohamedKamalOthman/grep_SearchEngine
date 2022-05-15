package utilities;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import java.util.HashMap;
import java.util.Map;

public class HTMLParserUtilities {
    /**
     * Threshold of importance at which the current tag is treated as a full paragraph.
     */
    public static final int THRESHOLD = 3;
    private static final Map<String, Integer> TAG_IMPORTANCE_MAP = new HashMap<>();
    static
    {
        TAG_IMPORTANCE_MAP.put("title", 10);
        TAG_IMPORTANCE_MAP.put("h1", 9);
        TAG_IMPORTANCE_MAP.put("h2", 8);
        TAG_IMPORTANCE_MAP.put("h3", 7);
        TAG_IMPORTANCE_MAP.put("h4", 6);
        TAG_IMPORTANCE_MAP.put("h5", 5);
        TAG_IMPORTANCE_MAP.put("h6", 4);
        TAG_IMPORTANCE_MAP.put("p", THRESHOLD);
        TAG_IMPORTANCE_MAP.put("strong", 1);
        TAG_IMPORTANCE_MAP.put("em", 1);
        TAG_IMPORTANCE_MAP.put("b", 1);
        TAG_IMPORTANCE_MAP.put("mark", 1);
        TAG_IMPORTANCE_MAP.put("i", 1);
        TAG_IMPORTANCE_MAP.put("blockquote", 1);
        TAG_IMPORTANCE_MAP.put("body", 0);
        TAG_IMPORTANCE_MAP.put("div", 1);
        TAG_IMPORTANCE_MAP.put("li", 1);
        TAG_IMPORTANCE_MAP.put("ol", 2);
        TAG_IMPORTANCE_MAP.put("ul", 2);
        TAG_IMPORTANCE_MAP.put("pre", 1);
        TAG_IMPORTANCE_MAP.put("span", 1);
        TAG_IMPORTANCE_MAP.put("a", 0);
        TAG_IMPORTANCE_MAP.put("article", 1);
        TAG_IMPORTANCE_MAP.put("code", 1);
    }

    public static int getTagImportance(String tag) {
        return TAG_IMPORTANCE_MAP.getOrDefault(tag, -1);
    }

    public static boolean isParagraphTag(String tag) {
        return TAG_IMPORTANCE_MAP.getOrDefault(tag, -1) >= THRESHOLD;
    }

    public static String buildStringFromNode(Node n) {
        StringBuilder sb = new StringBuilder();
        recursiveNodeBuild(n, sb);
        return sb.toString();
    }

    private static void recursiveNodeBuild(Node n, StringBuilder sb) {
        if (n instanceof TextNode tn) {
            sb.append(tn.text());
            return;
        }

        if (n instanceof Element) {
            for (Node node : n.childNodes()) {
                recursiveNodeBuild(node, sb);
            }
        }
    }


    private HTMLParserUtilities() {
        throw new IllegalStateException("Utility class HTMLParserUtilities instantiated");
    }
}
