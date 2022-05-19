package ranker;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import database.DBManager;
import org.bson.Document;
import utilities.HTMLParserUtilities;
import utilities.Stemmer;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class PageRanker {
    private final DBManager Manager;
    private final HashMap<String,Number> popularityMap;
    protected static HashMap<Long,String> paragraphsMap = new HashMap<>();
    public PageRanker(DBManager manager) {
        this.Manager = manager;
        popularityMap = Manager.getPopularity();
    }

    public ArrayList<RankerResult> GetSingleWordResults(String word){
        String stemmed_word = Stemmer.stemWord(word);
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
            int term_frequency = (int)occurrence.get("term_frequency");
            double normalized_term_frequency = (double) (term_frequency) / (double) totalLength;
            if(normalized_term_frequency > 0.5)
                continue;
            // Initial Rank Of Page
            result.rank = inverse_document_frequency * normalized_term_frequency;
            try {
                var host = new URL(result.url);
                Number popularity = popularityMap.getOrDefault(host.getHost(),1);
                // Update Rank With Popularity Of The Page Host
                result.rank *= Math.abs(Math.log( (double)((int)popularity) / 5000.0));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            Document total_counts = (Document) occurrence.get("total_count");
            for(String k : total_counts.keySet())
            {
                int importance = HTMLParserUtilities.getTagImportance(k);
                int count = (int) total_counts.get(k);
                // Update Rank According To Tag In Which The Word is Present
                result.rank *= (double)importance * ((double) count / (double) term_frequency);
            }
            result.paragraphs = new ArrayList<>();
            result.topParagraphs = new ArrayList<>();
            int count = 1;

            for(Document place : (ArrayList<Document>) occurrence.get("places")){
                ParagraphData p = new ParagraphData();
                p.exactWord = (String) place.get("exactWord");
                p.location = (long) place.get("location");
                p.hash = (long)place.get("paragraph");
                result.paragraphs.add(p);
                if(((String) place.get("exactWord")).equals(word)){
                    result.topParagraphs.add(p);
                    count++;
                }
            }
            // Update Rank Of Pages With The Exact Word
            result.rank *= (count / (double) totalLength);
            result.title = (String)occurrence.get("title");
            if(result.topParagraphs.isEmpty())
                result.topParagraphs.add(result.paragraphs.get(0));
            //fetch the top paragraph only from the database
            paragraphsMap.put(result.topParagraphs.get(0).hash,"");
            //finally, add the result
            Results.add(result);
        }
        return Results;
    }
    public void setParagraphsMap(){
        //after getting all results fetch all data from database only the paragraphs we need which in paragraphMap
        paragraphsMap = Manager.findParagraphs(paragraphsMap.keySet().stream().toList());
    }
    public static void main(String[] args) {
        DBManager db = new DBManager();
        PageRanker pageRanker = new PageRanker(db);
        var result = pageRanker.GetSingleWordResults("page");
        result.sort(((o1, o2) -> {
            return Double.compare(o2.rank, o1.rank);
        }));
        System.out.println(result);
    }
}
