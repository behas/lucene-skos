package at.ac.univie.mminf.luceneSKOS.analysis;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer.ExpansionType;
import at.ac.univie.mminf.luceneSKOS.util.AnalyzerUtils;

/**
 * Testing the SKOS URI Filter
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public class SKOSURIFilterTest extends AbstractFilterTest {
  
  @Before
  @Override
  public void setUp() throws Exception {
    
    super.setUp();
    
    skosAnalyzer = new SKOSAnalyzer(matchVersion, skosEngine, ExpansionType.URI);
    
    writer = new IndexWriter(directory, new IndexWriterConfig(matchVersion,
        skosAnalyzer));
    
  }
  
  @Test
  public void singleUriExpansionWithStoredField() throws Exception {
    
    Document doc = new Document();
    doc.add(new Field("subject", "http://example.com/concept/1",
        TextField.TYPE_STORED));
    
    writer.addDocument(doc);
    
    searcher = new IndexSearcher(DirectoryReader.open(writer, false));
    
    Query query = new TermQuery(new Term("subject", "leaps"));
    
    TopDocs results = searcher.search(query, 10);
    Assert.assertEquals(1, results.totalHits);
    
    Document indexDoc = searcher.doc(results.scoreDocs[0].doc);
    
    String[] fieldValues = indexDoc.getValues("subject");
    
    Assert.assertEquals(1, fieldValues.length);
    
    Assert.assertEquals(fieldValues[0], "http://example.com/concept/1");
    
  }
  
  @Test
  public void singleUriExpansionWithUnstoredField() throws Exception {
    
    Document doc = new Document();
    doc.add(new Field("subject", "http://example.com/concept/1",
        TextField.TYPE_NOT_STORED));
    
    writer.addDocument(doc);
    
    searcher = new IndexSearcher(DirectoryReader.open(writer, false));
    
    Query query = new TermQuery(new Term("subject", "jumps"));
    
    TopDocs results = searcher.search(query, 10);
    Assert.assertEquals(1, results.totalHits);
    
    Document indexDoc = searcher.doc(results.scoreDocs[0].doc);
    
    String[] fieldValues = indexDoc.getValues("subject");
    
    Assert.assertEquals(0, fieldValues.length);
    
  }
  
  @Test
  public void multipleURIExpansion() throws Exception {
    
    Document doc = new Document();
    doc.add(new Field("subject", "http://example.com/concept/1",
        TextField.TYPE_STORED));
    doc.add(new Field("subject", "http://example.com/concept/2",
        TextField.TYPE_STORED));
    
    writer.addDocument(doc);
    
    searcher = new IndexSearcher(DirectoryReader.open(writer, false));
    
    // querying for alternative term of concept 1
    Query query = new TermQuery(new Term("subject", "hops"));
    
    TopDocs results = searcher.search(query, 10);
    Assert.assertEquals(1, results.totalHits);
    
    Document indexDoc = searcher.doc(results.scoreDocs[0].doc);
    
    String[] fieldValues = indexDoc.getValues("subject");
    
    Assert.assertEquals(2, fieldValues.length);
    
    // querying for alternative term of concept 2
    query = new TermQuery(new Term("subject", "speedy"));
    
    results = searcher.search(query, 10);
    Assert.assertEquals(1, results.totalHits);
    
    indexDoc = searcher.doc(results.scoreDocs[0].doc);
    
    fieldValues = indexDoc.getValues("subject");
    
    Assert.assertEquals(2, fieldValues.length);
    
  }
  
  // @Test
  public void displayTokensWithURIExpansion() throws Exception {
    
    String text = "http://example.com/concept/1";
    
    skosAnalyzer = new SKOSAnalyzer(matchVersion, skosEngine, ExpansionType.URI);
    
    AnalyzerUtils.displayTokensWithFullDetails(skosAnalyzer, text);
    // AnalyzerUtils.displayTokensWithPositions(synonymAnalyzer, text);
    
  }
  
}
