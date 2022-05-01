package Queries;

import Database.IdbManager;
import Database.dbManager;
import Ranker.PageRanker;
import Ranker.RankerResult;
import com.sun.jdi.PathSearchingVirtualMachine;
import org.springframework.data.domain.Page;

import javax.xml.transform.Result;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class QueryProcessor {
    private String[] Query;
    private final PageRanker ranker;
    QueryProcessor(String Query, IdbManager Manager) {
        ranker = new PageRanker(Manager);
        this.Query = Query.toLowerCase().split("\\s+");
    }

    public void rankQuery() {
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

        for(var Result : RankedPages) {
            System.out.println(Result);
        }
    }

    public static void main(String[] args) {
        QueryProcessor magic = new QueryProcessor("local", new dbManager());
        magic.rankQuery();
    }
}
