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
            strictQueries.add(m.group(1).strip().toLowerCase());
        }
        boolean strictSearch = !strictQueries.isEmpty();

        String[] query = rawQuery.toLowerCase().strip().replaceAll("\"", " ").split("\\s+");

        List<List<RankerResult>> results = new ArrayList<>();
        for(String word : query) {
            var result = ranker.getSingleWordResults(word, strictSearch);
            if(result != null)
                results.add(result);
        }


        HashMap<String, RankerResult> aggregate = new HashMap<>();
        for(var ResultsList : results) {
            for(var Result : ResultsList) {
                if(aggregate.containsKey(Result.url)) {
                    RankerResult prev = aggregate.get(Result.url);
                    Map<Long, ParagraphData> pMap = new HashMap<>();
                    for(ParagraphData pData : prev.paragraphs) {
                        pMap.put(pData.hash, pData);
                    }
                    for(ParagraphData pData : Result.paragraphs) {
                        if(pMap.containsKey(pData.hash)) {
                            pMap.get(pData.hash).refCount++;
                        }
                        else {
                            prev.paragraphs.add(pData);
                        }
                    }

                    prev.rank += Result.rank;
                }

                aggregate.putIfAbsent(Result.url, Result);
            }
        }

        if(strictSearch) {
            List<Set<Long>> pHashSets = new ArrayList<>();
            for(String phrase : strictQueries)
                pHashSets.add(ranker.getPhraseMatchHashes(phrase));

            for(Iterator<Map.Entry<String, RankerResult>> it = aggregate.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, RankerResult> resultAndRank = it.next();
                RankerResult result = resultAndRank.getValue();
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

        ArrayList<RankerResult> rankedPages = new ArrayList<>(aggregate.values());
        rankedPages.sort(((o1, o2) -> Double.compare(o2.rank, o1.rank)));
        if(!strictSearch) {
            for(RankerResult result : rankedPages) {
                ParagraphData topParagraph = Collections.max(result.paragraphs, Comparator.comparing(c -> c.refCount));
                if(result.topParagraphs.isEmpty()) {
                    result.topParagraphs.add(topParagraph);
                }
                else {
                    result.topParagraphs.set(0, topParagraph);
                }

                ranker.prefetchParagraph(topParagraph.hash);
            }
        }

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
        magic.rankQuery("\"codeforces\"");
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms to run!");
    }
}
