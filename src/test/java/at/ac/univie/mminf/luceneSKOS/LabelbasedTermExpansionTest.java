package at.ac.univie.mminf.luceneSKOS;

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
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
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.Test;

import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer.ExpansionType;

/**
 * This test-case verifies and demonstrates the "Label-based term expansion" use
 * case as described in https://code.
 * google.com/p/lucene-skos/wiki/UseCases#UC2:_Label-based_term_expansion
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public class LabelbasedTermExpansionTest extends AbstractTermExpansionTest {
  
  /**
   * This test indexes a sample metadata record (=lucene document) having a
   * "title", "description", and "subject" field.
   * 
   * A search for "arms" returns that record as a result because "arms" is
   * defined as an alternative label for "weapons", the term which is contained
   * in the subject field.
   * 
   * @throws Exception
   */
  @Test
  public void labelBasedTermExpansion() throws Exception {
    
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
    
    /* setting up the SKOS analyzer */
    String skosFile = "src/test/resources/skos_samples/ukat_examples.n3";
    
    /* ExpansionType.URI->the field to be analyzed (expanded) contains URIs */
    Analyzer skosAnalyzer = new SKOSAnalyzer(skosFile, ExpansionType.LABEL);
    
    /* Define different analyzers for different fields */
    Map<String,Analyzer> analyzerPerField = new HashMap<String,Analyzer>();
    analyzerPerField.put("subject", skosAnalyzer);
    PerFieldAnalyzerWrapper indexAnalyzer = new PerFieldAnalyzerWrapper(
        new SimpleAnalyzer(Version.LUCENE_40), analyzerPerField);
    
    /* setting up a writer with a default (simple) analyzer */
    writer = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(
        Version.LUCENE_40, indexAnalyzer));
    
    /* adding the document to the index */
    writer.addDocument(doc);
    
    /* defining a query that searches over all fields */
    BooleanQuery query1 = new BooleanQuery();
    query1.add(new TermQuery(new Term("title", "arms")),
        BooleanClause.Occur.SHOULD);
    query1.add(new TermQuery(new Term("description", "arms")),
        BooleanClause.Occur.SHOULD);
    query1.add(new TermQuery(new Term("subject", "arms")),
        BooleanClause.Occur.SHOULD);
    
    /* creating a new searcher */
    searcher = new IndexSearcher(DirectoryReader.open(writer, false));
    
    TopDocs results = searcher.search(query1, 10);
    
    /* the document matches because "arms" is among the expanded terms */
    Assert.assertEquals(1, results.totalHits);
    
    /* defining a query that searches for a broader concept */
    Query query2 = new TermQuery(new Term("subject", "military equipment"));
    
    results = searcher.search(query2, 10);
    
    /* ... also returns the document as result */
    Assert.assertEquals(1, results.totalHits);
    
  }
  
}
