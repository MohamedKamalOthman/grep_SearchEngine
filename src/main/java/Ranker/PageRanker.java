package Ranker;

import Database.IdbManager;
import Database.dbManager;
import org.bson.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class PageRanker {
    private final IdbManager Manager;

    public PageRanker(IdbManager manager) {
        this.Manager = manager;
    }

    public ArrayList<RankerResult> GetSingleWordResults(String word){
        // Should Stem The Word First Then Search For it
        String stemmed_word = word;
        Document doc = Manager.getWordDocument(stemmed_word);
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
            double normalized_term_frequency = (double) ((int)occurrence.get("term_frequency")) / (double) ((long)occurrence.get("length"));
            if(normalized_term_frequency > 0.5)
                continue;
            int popularity = 1;
            result.rank = inverse_document_frequency * normalized_term_frequency;
            try {
                var host = new URL(result.url);
                popularity = Manager.getPopularity(host.getHost());
                result.rank *= Math.abs(Math.log((double) popularity / 5000.0));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            result.paragraphs = new ArrayList<>();
            result.topParagraphs = new ArrayList<>();
            for(Document place : (ArrayList<Document>) occurrence.get("places")){
                result.paragraphs.add(((String) place.get("paragraph")).trim());
                if(((String) place.get("exactWord")).equals(word))
                    result.topParagraphs.add(((String) place.get("paragraph")).trim());
            }
            Results.add(result);
        }
        return Results;
    }

    public static void main(String[] args) {
        dbManager db = new dbManager();
        PageRanker pageRanker = new PageRanker(db);
        var result = pageRanker.GetSingleWordResults("latest");
        result.sort(((o1, o2) -> {
            return o1.rank < o2.rank ? +1 : o1.rank > o2.rank ? -1 : 0;
        }));
        System.out.println(result);
    }
}
