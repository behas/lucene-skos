package at.ac.univie.mminf.luceneSKOS.analysis;

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

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer.ExpansionType;
import at.ac.univie.mminf.luceneSKOS.util.AnalyzerUtils;
import at.ac.univie.mminf.luceneSKOS.util.TestUtil;

/**
 * Testing the SKOS Label Filter
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * @author Martin Kysel <martin.kysel@univie.ac.at>
 * 
 */
public class SKOSLabelFilterTest extends AbstractFilterTest {
  
  @Before
  @Override
  public void setUp() throws Exception {
    
    super.setUp();
    
    skosAnalyzer = new SKOSAnalyzer(matchVersion, skosEngine,
        ExpansionType.LABEL);
    
    writer = new IndexWriter(directory, new IndexWriterConfig(matchVersion,
        skosAnalyzer));
    
  }
  
  @Test
  public void termQuerySearch() throws CorruptIndexException, IOException {
    
    Document doc = new Document();
    doc.add(new Field("content", "The quick brown fox jumps over the lazy dog",
        Field.Store.YES, Field.Index.ANALYZED));
    
    writer.addDocument(doc);
    
    searcher = new IndexSearcher(IndexReader.open(writer, false));
    
    TermQuery tq = new TermQuery(new Term("content", "hops"));
    
    Assert.assertEquals(1, TestUtil.hitCount(searcher, tq));
    
  }
  
  @Test
  public void phraseQuerySearch() throws CorruptIndexException, IOException {
    
    Document doc = new Document();
    doc.add(new Field("content", "The quick brown fox jumps over the lazy dog",
        Field.Store.YES, Field.Index.ANALYZED));
    
    writer.addDocument(doc);
    
    searcher = new IndexSearcher(IndexReader.open(writer, false));
    
    PhraseQuery pq = new PhraseQuery();
    pq.add(new Term("content", "fox"));
    pq.add(new Term("content", "hops"));
    
    Assert.assertEquals(1, TestUtil.hitCount(searcher, pq));
    
  }
  
  @Test
  public void queryParserSearch() throws IOException, ParseException {
    
    Document doc = new Document();
    doc.add(new Field("content", "The quick brown fox jumps over the lazy dog",
        Field.Store.YES, Field.Index.ANALYZED));
    
    writer.addDocument(doc);
    
    searcher = new IndexSearcher(IndexReader.open(writer, false));
    
    Query query = new QueryParser(Version.LUCENE_36, "content", skosAnalyzer)
        .parse("\"fox jumps\"");
    
    Assert.assertEquals(1, TestUtil.hitCount(searcher, query));
    
    Assert.assertEquals("content:\"fox (jumps hops leaps)\"", query.toString());
    Assert.assertEquals("org.apache.lucene.search.MultiPhraseQuery", query
        .getClass().getName());
    
    query = new QueryParser(Version.LUCENE_36, "content", new StandardAnalyzer(
        Version.LUCENE_36)).parse("\"fox jumps\"");
    Assert.assertEquals(1, TestUtil.hitCount(searcher, query));
    
    Assert.assertEquals("content:\"fox jumps\"", query.toString());
    Assert.assertEquals("org.apache.lucene.search.PhraseQuery", query
        .getClass().getName());
    
  }
  
  @Test
  public void testTermQuery() throws CorruptIndexException, IOException,
      ParseException {
    
    Document doc = new Document();
    doc.add(new Field("content", "I work for the united nations",
        Field.Store.YES, Field.Index.ANALYZED));
    
    writer.addDocument(doc);
    
    searcher = new IndexSearcher(IndexReader.open(writer, false));
    
    QueryParser parser = new QueryParser(Version.LUCENE_36, "content",
        new SimpleAnalyzer(Version.LUCENE_36));
    
    Query query = parser.parse("united nations");
    
    Assert.assertEquals(1, TestUtil.hitCount(searcher, query));
    
  }
  
  // @Test
  public void displayTokensWithLabelExpansion() throws IOException {
    
    String text = "The quick brown fox jumps over the lazy dog";
    
    AnalyzerUtils.displayTokensWithFullDetails(skosAnalyzer, text);
    // AnalyzerUtils.displayTokensWithPositions(synonymAnalyzer, text);
    
  }
  
}
