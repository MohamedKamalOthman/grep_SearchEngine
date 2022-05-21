package ranker;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class RankerResult {
    public String url;
    public long hash;
    public ArrayList<ParagraphData> paragraphs;
    public ArrayList<ParagraphData> topParagraphs;
    public double rank;
    public String title;


    // if you want this to work properly get the paragraph with hash like we do in JSON
    @Override
    public String toString(){
        String result =  "URL = " + url + " HASH = " + hash + "\n" +
                "With Rank = " + rank + "\n" +
                "Top Paragraphs : \n";
        for(var p : topParagraphs)
            result += p.location + "\n" + p.paragraph + "\n-------------------------------------- \n";

        result +="All Paragraphs : \n";

        for(var p : paragraphs)
            result += p.location + "\n" + p.paragraph + "\n-------------------------------------- \n";

        return result;
    }
    public Document toJSON(){
        return new Document()
                .append("title",title)
                .append("p", PageRanker.fetchedParagraphsMap.get(topParagraphs.get(0).hash))
                .append("url",url);
    }
    @Override
    public int hashCode() {
        return url.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        return this.hashCode() == o.hashCode();
    }
}
