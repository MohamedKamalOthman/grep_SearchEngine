package queries;


import database.DBManager;
import ranker.PageRanker;
import ranker.RankerResult;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class QueryProcessor {
    private String[] Query;
    private final PageRanker ranker;
    long resultsAmount;
    double time;
    public QueryProcessor(DBManager Manager) {
        ranker = new PageRanker(Manager);
    }
    public List<Document> rankQuery(String query) {
        long start = System.currentTimeMillis();
        Query = query.toLowerCase().split("\\s+");
        ArrayList<ArrayList<RankerResult>> results = new ArrayList<>();
        for(String word : Query) {
            var result = ranker.GetSingleWordResults(word, false);
            if(result != null)
                results.add(result);
        }

        HashMap<RankerResult, Double> AggregatedResults = new HashMap<>();
        for(var ResultsList : results) {
            for(var Result : ResultsList) {
                AggregatedResults.put(Result, AggregatedResults.getOrDefault(Result, 0.0) + Result.rank);
            }
        }

        for(var Result : AggregatedResults.keySet()) {
            Result.rank = AggregatedResults.get(Result);
        }
        ArrayList<RankerResult> RankedPages = new ArrayList<>(AggregatedResults.keySet());
        RankedPages.sort(((o1, o2) -> {
            return Double.compare(o2.rank, o1.rank);
        }));

        // Get Paragraphs From Database
        ranker.setParagraphsMap();

        //return result list of json
        List<Document> searchResult = new ArrayList<>();
        for (var doc:RankedPages) {
            searchResult.add(doc.toJSON());
        }
        time = (System.currentTimeMillis() - start)/1000.0;
        //logs falla7y
        for(var Result : RankedPages) {
            System.out.println(Result);
        }

        resultsAmount = RankedPages.size();
        System.out.println("No of results: " + RankedPages.size());
        return searchResult;
    }
    public double getTime(){
        return time;
    }

    public long getResultsAmount() {
        return resultsAmount;
    }

    public static void main(String[] args) {

        QueryProcessor magic = new QueryProcessor(new DBManager());
        long start = System.currentTimeMillis();
        magic.rankQuery("codeforces contest div 1 2 3 4 5 6");
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to run!");
    }
}
