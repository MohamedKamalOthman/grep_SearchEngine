package Queries;

import Database.IdbManager;
import Database.dbManager;
import Ranker.PageRanker;
import Ranker.RankerResult;
import com.sun.jdi.PathSearchingVirtualMachine;
import org.bson.Document;
import org.springframework.data.domain.Page;

import javax.xml.transform.Result;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class QueryProcessor {
    private String[] Query;
    private final PageRanker ranker;
    public QueryProcessor(IdbManager Manager) {
        ranker = new PageRanker(Manager);
    }

    public List<Document> rankQuery(String query) {
        Query = query.toLowerCase().split("\\s+");
        ArrayList<ArrayList<RankerResult>> results = new ArrayList<>();
        for(String word : Query) {
            var result = ranker.GetSingleWordResults(word);
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


        //logs falla7y
        for(var Result : RankedPages) {
            System.out.println(Result);
        }
        //return result list of json
        List<Document> searchResult = new ArrayList<>();
        for (var doc:RankedPages) {
            searchResult.add(doc.toJSON());
        }
        return searchResult;
    }

    public static void main(String[] args) {
//        QueryProcessor magic = new QueryProcessor("local drug addicts", new dbManager());
//        magic.rankQuery();
    }
}
