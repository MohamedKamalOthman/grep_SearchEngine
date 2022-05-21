package ranker;

import database.DBManager;
import org.bson.Document;
import utilities.HTMLParserUtilities;
import utilities.Stemmer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class PageRanker {
    private final DBManager manager;
    private final HashMap<String,Number> popularityMap;
    private static final HashMap<Long,String> paragraphsMap = new HashMap<>();
    protected static HashMap<Long,String> fetchedParagraphsMap = new HashMap<>();
    public PageRanker(DBManager manager) {
        this.manager = manager;
        popularityMap = this.manager.getPopularity();
    }

    public List<RankerResult> getSingleWordResults(String word, boolean strictSearch){
        String stemmedWord = Stemmer.stemWord(word);
        if(stemmedWord == null)
            return null;
        Document doc = manager.getWordDocument(stemmedWord);
        if(doc == null)
            return new ArrayList<>();
        Document occurrences = (Document) doc.get("occurrences");
        long countOccurrences = occurrences.keySet().size();
        /** Inverse Document Frequency */
        double inverseDocumentFrequency = Math.log(5000.0 / (countOccurrences));

        ArrayList<RankerResult> results = new ArrayList<>();
        for(String key : occurrences.keySet())
        {
            RankerResult result = new RankerResult();
            Document occurrence = (Document) occurrences.get(key);
            result.url = (String) occurrence.get("url");
            long totalLength = (long)occurrence.get("length");
            int term_frequency = (int)occurrence.get("term_frequency");
            /** Normalized Term Frequency */
            double normalizedTermFrequency = (double) (term_frequency) / (double) totalLength;
            if(normalizedTermFrequency > 0.5)
                continue;

            /** Initial Rank Of Page ( RANK = IDF * NTF) */
            result.rank = inverseDocumentFrequency  * normalizedTermFrequency ;

            try {
                var host = new URL(result.url);
                Number popularity = popularityMap.getOrDefault(host.getHost(),1);
                /** Add Popularity To Rank */
                result.rank *= Math.abs(Math.log( 1.01 + ((int)popularity) / 5000.0));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            Document totalCount = (Document) occurrence.get("total_count");
            for(String k : totalCount.keySet())
            {
                int importance = HTMLParserUtilities.getTagImportance(k);
                int count = (int) totalCount.get(k);
                /** HTML Tag Importance Factor */
                result.rank *= ((double) count / (double) term_frequency) * importance;
            }
            result.paragraphs = new ArrayList<>();
            result.topParagraphs = new ArrayList<>();
            int count_exact = 1;

            for(Document place : (ArrayList<Document>) occurrence.get("places")){
                String tag = (String)place.get("text_type");
                //Ignore header tags
                if(!tag.isBlank() && tag.charAt(0) == 'h' && !result.paragraphs.isEmpty())
                    continue;
                ParagraphData p = new ParagraphData();
                p.exactWord = (String) place.get("exactWord");
                p.location = (long) place.get("location");
                p.hash = (long) place.get("paragraph");
                result.paragraphs.add(p);
                if((place.get("exactWord")).equals(word)){
                    result.topParagraphs.add(p);
                    count_exact++;
                }
            }

            /** Increase Importance Of Pages With Exact Word */
            result.rank *= (count_exact / (double) totalLength);
            result.title = (String)occurrence.get("title");
            if(result.topParagraphs.isEmpty())
            {
                if(strictSearch)
                    continue;
                else
                    result.topParagraphs.add(result.paragraphs.get(0));
            }

            //fetch the top paragraph only from the database
            paragraphsMap.put(result.topParagraphs.get(0).hash,"");

            //finally, add the result
            results.add(result);
        }
        return results;
    }

    public Set<Long> getPhraseMatchHashes(String phrase)
    {
        return manager.getExactPhraseParagraphs(phrase);
    }

    public void setParagraphsMap(){
        //after getting all results fetch all data from database only the paragraphs we need which in paragraphMap
        fetchedParagraphsMap = manager.findParagraphs(paragraphsMap.keySet().stream().toList());
    }

    public static void main(String[] args) {
        DBManager db = new DBManager();
        PageRanker pageRanker = new PageRanker(db);
        var result = pageRanker.getSingleWordResults("page", false);
        result.sort(((o1, o2) -> {
            return Double.compare(o2.rank, o1.rank);
        }));
        System.out.println(result);
    }

    public void prefetchParagraph(long hash) {
        paragraphsMap.put(hash, "");
    }
}
