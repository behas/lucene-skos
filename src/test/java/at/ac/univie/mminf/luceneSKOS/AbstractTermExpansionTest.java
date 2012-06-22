package at.ac.univie.mminf.luceneSKOS;

import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
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
  
  protected IndexSearcher searcher;
  
  protected IndexWriter writer;
  
  @After
  public void tearDown() throws Exception {
    
    writer.close();
    
    searcher.close();
    
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
    doc.add(new Field("title", "Spearhead", Field.Store.YES,
        Field.Index.ANALYZED));
    doc.add(new Field(
        "description",
        "Roman iron spearhead. The spearhead was attached to one end of a wooden shaft..."
            + "The spear was mainly a thrusting weapon, but could also be thrown. "
            + "It was the principal weapon of the auxiliary soldier... "
            + "(second - fourth century, Arbeia Roman Fort).", Field.Store.NO,
        Field.Index.ANALYZED));
    doc.add(new Field("subject", "weapons", Field.Store.NO,
        Field.Index.ANALYZED));
    
    /* setting up a writer with a default (simple) analyzer */
    writer = new IndexWriter(new RAMDirectory(), new SimpleAnalyzer(),
        IndexWriter.MaxFieldLength.UNLIMITED);
    
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
    searcher = new IndexSearcher(writer.getReader());
    
    TopDocs results = searcher.search(query, 10);
    
    /* no results are returned since there is no term match */
    Assert.assertEquals(0, results.totalHits);
    
  }
  
}
