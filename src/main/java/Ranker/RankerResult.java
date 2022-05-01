package Ranker;

import java.util.ArrayList;

public class RankerResult {
    public String url;
    public ArrayList<ParagraphData> paragraphs;
    public ArrayList<ParagraphData> topParagraphs;
    public double rank;


    @Override
    public String toString(){
        String result =  "URL = " + url + "\n" +
                "With Rank = " + rank + "\n" +
                "Top Paragraphs : \n";
        for(var p : paragraphs)
            result += p.location + "\n" + p.paragraph + "\n-------------------------------------- \n";

        result +="All Paragraphs : \n";

        for(var p : paragraphs)
            result += p.location + "\n" + p.paragraph + "\n-------------------------------------- \n";

        return result;
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }

}
