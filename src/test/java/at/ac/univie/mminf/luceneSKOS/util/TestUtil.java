package at.ac.univie.mminf.luceneSKOS.util;

import java.io.IOException;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

/**
 * Common utilities for testing.
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public class TestUtil {
  
  public static int hitCount(IndexSearcher searcher, Query query)
      throws IOException {
    
    TopDocs results = searcher.search(query, 1);
    
    return results.totalHits;
    
  }
  
  public static void explainQuery(IndexSearcher searcher, Query query)
      throws IOException {
    
    TopDocs topDocs = searcher.search(query, 10);
    
    for (ScoreDoc match : topDocs.scoreDocs) {
      
      Explanation explanation = searcher.explain(query, match.doc);
      
      System.out.println("---------------");
      
      System.out.println(explanation.toString());
      
    }
    
  }
  
}
