package Ranker;

import java.util.ArrayList;

public class RankerResult {
    public String url;
    public ArrayList<String> paragraphs;
    public ArrayList<String> topParagraphs;
    public double rank;


    @Override
    public String toString(){
        String result =  "URL = " + url + "\n" +
                "With Rank = " + rank + "\n" +
                "Top Paragraphs : \n";
        for(String p : paragraphs)
            result += p + "\n-------------------------------------- \n";

        result +="All Paragraphs : \n";

        for(String p : paragraphs)
            result += p + "\n-------------------------------------- \n";

        return result;
    }


}
