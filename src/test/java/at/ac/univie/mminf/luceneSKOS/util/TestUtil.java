package at.ac.univie.mminf.luceneSKOS.util;

/**
 * Copyright 2010 Bernhard Haslhofer 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
