package at.ac.univie.mminf.luceneSKOS;

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

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Common code used for all Use Case (UC) tests
 */
public abstract class AbstractTermExpansionTest {
  
  protected final Version matchVersion = Version.LUCENE_45;
  
  protected IndexSearcher searcher;
  
  protected IndexWriter writer;
  
  @After
  public void tearDown() throws Exception {
    
    writer.close();
    
    searcher.getIndexReader().close();
    
  }
  
  /**
   * This test indexes a sample metadata record (=lucene document) having a
   * "title", "description", and "subject" field, which contains plain subject
   * terms.
   * 
   * A search for "arms" doesn't return that record because the term "arms" is
   * not explicitly contained in the record (document).
   * 
   * @throws IOException
   * @throws LockObtainFailedException
   * @throws CorruptIndexException
   */
  @Test
  public void noExpansion() throws CorruptIndexException,
      LockObtainFailedException, IOException {
    
    /* defining the document to be indexed */
    Document doc = new Document();
    doc.add(new Field("title", "Spearhead", TextField.TYPE_STORED));
    doc.add(new Field(
        "description",
        "Roman iron spearhead. The spearhead was attached to one end of a wooden shaft..."
            + "The spear was mainly a thrusting weapon, but could also be thrown. "
            + "It was the principal weapon of the auxiliary soldier... "
            + "(second - fourth century, Arbeia Roman Fort).",
        TextField.TYPE_NOT_STORED));
    doc.add(new Field("subject", "weapons", TextField.TYPE_NOT_STORED));
    
    /* setting up a writer with a default (simple) analyzer */
    writer = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(
        Version.LUCENE_45, new SimpleAnalyzer(Version.LUCENE_45)));
    
    /* adding the document to the index */
    writer.addDocument(doc);
    
    /* defining a query that searches over all fields */
    BooleanQuery query = new BooleanQuery();
    query.add(new TermQuery(new Term("title", "arms")),
        BooleanClause.Occur.SHOULD);
    query.add(new TermQuery(new Term("description", "arms")),
        BooleanClause.Occur.SHOULD);
    query.add(new TermQuery(new Term("subject", "arms")),
        BooleanClause.Occur.SHOULD);
    
    /* creating a new searcher */
    searcher = new IndexSearcher(DirectoryReader.open(writer, false));
    
    TopDocs results = searcher.search(query, 10);
    
    /* no results are returned since there is no term match */
    Assert.assertEquals(0, results.totalHits);
    
  }
  
}
