package at.ac.univie.mminf.luceneSKOS;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Common code used for all Use Case (UC) tests
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public abstract class AbstractTermExpansionTest {
  
  protected final Version matchVersion = Version.LUCENE_40;
  
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
   * @throws Exception
   */
  @Test
  public void noExpansion() throws Exception {
    
    /* defining the document to be indexed */
    Document doc = new Document();
    doc.add(new Field("title", "Spearhead", TextField.TYPE_STORED));
    doc.add(new Field(
        "description",
        "Roman iron spearhead. The spearhead was attached to one end of a wooden shaft..."
            + "The spear was mainly a thrusting weapon, but could also be thrown. "
            + "It was the principal weapon of the auxiliary soldier... "
            + "(second - fourth century, Arbeia Roman Fort).",
        TextField.TYPE_UNSTORED));
    doc.add(new Field("subject", "weapons", TextField.TYPE_UNSTORED));
    
    /* setting up a writer with a default (simple) analyzer */
    writer = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(
        Version.LUCENE_40, new SimpleAnalyzer(Version.LUCENE_40)));
    
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
