package Indexer;

import java.util.HashMap;

public class HTMLParserUtilities {
    public static final HashMap<String, Integer> TagImportanceMap = new HashMap<>() {
        {
            put("title", 10);
            put("h1", 9);
            put("h2", 8);
            put("h3", 7);
            put("h4", 6);
            put("h5", 5);
            put("h6", 4);
            put("p", 3);
            put("strong", 1);
            put("em", 1);
            put("b", 1);
            put("mark", 1);
            put("i", 1);
            put("blockquote", 1);
            put("body", 1);
            put("div", 1);
            put("li", 1);
            put("ol", 1);
            put("ul", 1);
            put("pre", 1);
            put("span", 1);
            put("a", 0);
            put("article", 1);
            put("code", 1);
        }
    };
}
