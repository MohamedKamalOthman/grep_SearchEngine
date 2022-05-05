package Ranker;

import Database.IdbManager;
import Database.dbManager;
import Indexer.PageIndexer;
import org.bson.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class PageRanker {
    private final IdbManager Manager;
    private final HashMap<String,Number> popularityMap;
    public PageRanker(IdbManager manager) {
        this.Manager = manager;
        popularityMap = Manager.getPopularity();
    }

    public ArrayList<RankerResult> GetSingleWordResults(String word){
        String stemmed_word = PageIndexer.stemWord(word);
        if(stemmed_word == null)
            return null;
        Document doc = Manager.getWordDocument(stemmed_word);
        if(doc == null)
            return new ArrayList<>();
        //System.out.println(doc.toJson());
        Document occurrences = (Document) doc.get("occurrences");
        long count_occurrences = occurrences.keySet().size();
        double inverse_document_frequency = Math.log(5000.0 / (double) count_occurrences);
        System.out.println("IDF For Word = " + inverse_document_frequency);
        ArrayList<RankerResult> Results = new ArrayList<>();
        for(String key : occurrences.keySet())
        {
            RankerResult result = new RankerResult();
            Document occurrence = (Document) occurrences.get(key);
            result.url = (String) occurrence.get("url");
            long totalLength = (long)occurrence.get("length");
            double normalized_term_frequency = (double) ((int)occurrence.get("term_frequency")) / (double) totalLength;
            if(normalized_term_frequency > 0.5)
                continue;
            result.rank = inverse_document_frequency * normalized_term_frequency;
            try {
                var host = new URL(result.url);
                Number popularity = popularityMap.getOrDefault(host.getHost(),1);
                result.rank *= Math.abs(Math.log( (double)((int)popularity) / 5000.0));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            result.paragraphs = new ArrayList<>();
            result.topParagraphs = new ArrayList<>();
            int count = 1;
            for(Document place : (ArrayList<Document>) occurrence.get("places")){
                ParagraphData p = new ParagraphData();
                p.exactWord = (String) place.get("exactWord");
                p.location = (long) place.get("location");
                p.paragraph = ((String) place.get("paragraph")).trim();
                result.paragraphs.add(p);
                if(((String) place.get("exactWord")).equals(word)){
                    result.topParagraphs.add(p);
                    count++;
                }
            result.rank *= 2 * (count / (double) totalLength);
            }
            result.title = (String)occurrence.get("title");
            if(result.topParagraphs.isEmpty())
                result.topParagraphs.add(result.paragraphs.get(0));
            Results.add(result);
        }
        return Results;
    }

    public static void main(String[] args) {
        dbManager db = new dbManager();
        PageRanker pageRanker = new PageRanker(db);
        var result = pageRanker.GetSingleWordResults("fox");
        result.sort(((o1, o2) -> {
            return o1.rank < o2.rank ? +1 : o1.rank > o2.rank ? -1 : 0;
        }));
        System.out.println(result);
    }
}
