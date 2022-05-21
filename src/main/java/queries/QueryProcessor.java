package queries;


import database.DBManager;
import ranker.PageRanker;
import ranker.ParagraphData;
import ranker.RankerResult;
import org.bson.Document;
import utilities.NormalizeURL;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryProcessor {
    private final PageRanker ranker;
    long resultsAmount;
    double time;
    public QueryProcessor(DBManager Manager) {
        ranker = new PageRanker(Manager);
    }
    public List<Document> rankQuery(String rawQuery) {
        long start = System.currentTimeMillis();
        if(rawQuery.isBlank())
            return new ArrayList<>();


        rawQuery = rawQuery.strip();
        List<String> strictQueries = new ArrayList<>();

        Pattern p = Pattern.compile("\"([^\"]*)\"");
        Matcher m = p.matcher(rawQuery);
        while (m.find()) {
            strictQueries.add(m.group(1).strip().toLowerCase().replaceAll("\\s+", " "));
        }
        boolean strictSearch = !strictQueries.isEmpty();

//        if(rawQuery.charAt(0) == '"' && rawQuery.charAt(rawQuery.length() - 1) == '"') {
//            rawQuery = rawQuery.substring(1, rawQuery.length() - 1);
//            strictSearch = true;
//        }

        String[] query = rawQuery.toLowerCase().split("\\s+");

        List<List<RankerResult>> results = new ArrayList<>();
        for(String word : query) {
            var result = ranker.GetSingleWordResults(word, strictSearch);
            if(result != null)
                results.add(result);
        }



        HashMap<RankerResult, Double> aggregatedResults = new HashMap<>();
        for(var ResultsList : results) {
            for(var Result : ResultsList) {
                aggregatedResults.put(Result, aggregatedResults.getOrDefault(Result, 0.0) + Result.rank);
            }
        }

        if(strictSearch) {
            List<Set<Long>> pHashSets = new ArrayList<>();
            for(String phrase : strictQueries)
                pHashSets.add(ranker.getPhraseMatchHashes(phrase));

            for(Iterator<Map.Entry<RankerResult, Double>> it = aggregatedResults.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<RankerResult, Double> resultAndRank = it.next();
                RankerResult result = resultAndRank.getKey();
                result.rank = resultAndRank.getValue();
                boolean found = false;
                for (ParagraphData paragraph : result.topParagraphs) {
                    boolean matched = true;
                    for(var pHashSet : pHashSets) {
                        matched = matched && pHashSet.contains(paragraph.hash);
                    }
                    if (matched) {
                        found = true;
                        result.topParagraphs.set(0, paragraph);
                    }
                }
                if(found) {
                    ranker.prefetchParagraph(result.topParagraphs.get(0).hash);
                }
                else {
                    it.remove();
                }
            }
        }

        ArrayList<RankerResult> rankedPages = new ArrayList<>(aggregatedResults.keySet());
        rankedPages.sort(((o1, o2) -> Double.compare(o2.rank, o1.rank)));


//        for(var Result : RankedPages) {
//            System.out.println(Result);
//        }

        // Get Paragraphs From Database
        ranker.setParagraphsMap();

        //return result list of json
        List<Document> searchResult = new ArrayList<>();
        for (var doc:rankedPages) {
            searchResult.add(doc.toJSON());
        }
        time = (System.currentTimeMillis() - start)/1000.0;
        resultsAmount = rankedPages.size();
        System.out.println("No of results: " + rankedPages.size());
        return searchResult;
    }

    public double getTime(){
        return time;
    }

    public long getResultsAmount() {
        return resultsAmount;
    }

    public static void main(String[] args) {
        String t1 = "https://codeforCes.com";
        String t2 = "https://coDEforces.com/pAthName/";
        String t3 = "https://codeforces.com#comments";
        try {
            System.out.println(NormalizeURL.normalize(t1.toLowerCase()));
            System.out.println(NormalizeURL.normalize(t2.toLowerCase()));
            System.out.println(NormalizeURL.normalize(t3.toLowerCase()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        QueryProcessor magic = new QueryProcessor(new DBManager());
        long start = System.currentTimeMillis();
        magic.rankQuery("\"codeforces systems\"");
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to run!");
    }
}
