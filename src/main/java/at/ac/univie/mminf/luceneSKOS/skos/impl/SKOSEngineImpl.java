package at.ac.univie.mminf.luceneSKOS.skos.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import at.ac.univie.mminf.luceneSKOS.skos.SKOS;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * A Lucene-backed SKOSEngine Implementation.
 * 
 * Each SKOS concept is stored/indexed as a Lucene document.
 * 
 * All labels are converted to lowercase
 * 
 * @author haslhofer
 * 
 */
public class SKOSEngineImpl implements SKOSEngine {
  
  /*
   * Static fields used in the Lucene Index
   */
  private static final String FIELD_URI = "uri";
  private static final String FIELD_PREF_LABEL = "pref";
  private static final String FIELD_ALT_LABEL = "alt";
  private static final String FIELD_BROADER = "broader";
  private static final String FIELD_NARROWER = "narrower";
  
  /**
   * The input SKOS model
   */
  private Model skosModel;
  
  /**
   * The location of the concept index
   */
  private Directory indexDir;
  
  /**
   * Provides access to the index
   */
  private IndexSearcher searcher;
  
  /**
   * The languages to be considered when returning labels.
   * 
   * If NULL, all languages are supported
   */
  private List<String> languages;
  
  /**
   * The analyzer used during indexing of / querying for concepts
   * 
   * SimpleAnalyzer = LetterTokenizer + LowerCaseFilter
   * 
   */
  private Analyzer analyzer = new SimpleAnalyzer();
  
  /**
   * Stores the maximum number of terms contained in a prefLabel
   */
  private int maxPrefLabelTerms = -1;
  
  /**
   * Constructor for all label-languages
   * 
   * @param filenameOrURI
   *          the name of the skos file to be loaded
   * @throws IOException
   */
  public SKOSEngineImpl(String filenameOrURI) throws IOException {
    this(filenameOrURI, (String[]) null);
  }
  
  /**
   * This constructor loads the SKOS model from a given InputStream using the
   * given serialization language parameter, which must be either N3, RDF/XML,
   * or TURTLE.
   * 
   * @param inputStream
   *          the input stream
   * @param lang
   *          the serialization language
   * @throws IOException
   *           if the model cannot be loaded
   */
  public SKOSEngineImpl(InputStream inputStream, String lang)
      throws IOException {
    
    if (!(lang.equals("N3") || lang.equals("RDF/XML") || lang.equals("TURTLE"))) {
      throw new IOException("Invalid RDF serialization format");
    }
    
    skosModel = ModelFactory.createDefaultModel();
    
    skosModel.read(inputStream, null, lang);
    
    initializeEngine();
    
  }
  
  /**
   * This constructor loads the SKOS model from a given filename or URI, starts
   * the indexing process and sets up the index searcher.
   * 
   * @param languages
   *          the languages to be considered
   * @param filenameOrURI
   * @throws IOException
   */
  public SKOSEngineImpl(String filenameOrURI, String... languages)
      throws IOException {
    
    // load the skos model from the given file
    
    FileManager fileManager = new FileManager();
    fileManager.addLocatorFile();
    fileManager.addLocatorURL();
    fileManager.addLocatorClassLoader(SKOSEngineImpl.class.getClassLoader());
    
    skosModel = fileManager.loadModel(filenameOrURI);
    
    if (languages != null) {
      this.languages = Arrays.asList(languages);
    }
    
    initializeEngine();
    
  }
  
  /**
   * Sets up the Lucene index and starts the indexing process
   * 
   * @throws IOException
   */
  private void initializeEngine() throws IOException {
    
    this.indexDir = new RAMDirectory();
    
    indexSKOSModel();
    
    this.searcher = new IndexSearcher(indexDir);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getPrefLabel(java.lang.
   * String)
   */
  @Override
  public String[] getPrefLabels(String conceptURI) throws IOException {
    
    return readConceptFieldValues(conceptURI, FIELD_PREF_LABEL);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getAltLabels(java.lang.
   * String)
   */
  @Override
  public String[] getAltLabels(String conceptURI) throws IOException {
    
    return readConceptFieldValues(conceptURI, FIELD_ALT_LABEL);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getBroaderConcepts(java
   * .lang.String)
   */
  @Override
  public String[] getBroaderConcepts(String conceptURI) throws IOException {
    
    return readConceptFieldValues(conceptURI, FIELD_BROADER);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getNarrowerConcepts(java
   * .lang.String)
   */
  @Override
  public String[] getNarrowerConcepts(String conceptURI) throws IOException {
    
    return readConceptFieldValues(conceptURI, FIELD_NARROWER);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getBroaderLabels(java.lang
   * .String)
   */
  @Override
  public String[] getBroaderLabels(String conceptURI) throws IOException {
    
    List<String> broaderLabels = new ArrayList<String>();
    
    String[] broaderConcepts = getBroaderConcepts(conceptURI);
    
    for (String brConceptURI : broaderConcepts) {
      String[] brPrefLabels = getPrefLabels(brConceptURI);
      broaderLabels.addAll(Arrays.asList(brPrefLabels));
      
      String[] brAltLabels = getAltLabels(brConceptURI);
      broaderLabels.addAll(Arrays.asList(brAltLabels));
      
    }
    
    return broaderLabels.toArray(new String[0]);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getNarrowerLabels(java.
   * lang.String)
   */
  @Override
  public String[] getNarrowerLabels(String conceptURI) throws IOException {
    
    List<String> narrowerLabels = new ArrayList<String>();
    
    String[] narrowerConcepts = getNarrowerConcepts(conceptURI);
    
    for (String nrConceptURI : narrowerConcepts) {
      String[] nrPrefLabels = getPrefLabels(nrConceptURI);
      narrowerLabels.addAll(Arrays.asList(nrPrefLabels));
      
      String[] nrAltLabels = getAltLabels(nrConceptURI);
      narrowerLabels.addAll(Arrays.asList(nrAltLabels));
    }
    
    return narrowerLabels.toArray(new String[0]);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getConcepts(java.lang.String
   * )
   */
  @Override
  public String[] getConcepts(String prefLabel) throws IOException {
    
    // convert the query to lower-case
    String queryString = prefLabel.toLowerCase();
    
    List<String> concepts = new ArrayList<String>();
    
    AllDocCollector collector = new AllDocCollector();
    
    searcher.search(new TermQuery(new Term(SKOSEngineImpl.FIELD_PREF_LABEL,
        queryString)), collector);
    
    for (ScoreDoc hit : collector.getHits()) {
      
      Document doc = searcher.doc(hit.doc);
      
      String conceptURI = doc.getValues(SKOSEngineImpl.FIELD_URI)[0];
      
      concepts.add(conceptURI);
      
    }
    
    return concepts.toArray(new String[0]);
    
  }
  
  @Override
  public String[] getAltTerms(String prefLabel) throws IOException {
    
    // convert the query to lower-case
    String queryString = prefLabel.toLowerCase();
    
    List<String> synList = new ArrayList<String>();
    
    AllDocCollector collector = new AllDocCollector();
    
    searcher.search(new TermQuery(new Term(SKOSEngineImpl.FIELD_PREF_LABEL,
        queryString)), collector);
    
    for (ScoreDoc hit : collector.getHits()) {
      
      Document doc = searcher.doc(hit.doc);
      
      String[] values = doc.getValues(SKOSEngineImpl.FIELD_ALT_LABEL);
      
      for (String syn : values) {
        synList.add(syn);
      }
      
    }
    
    return synList.toArray(new String[0]);
    
  }
  
  @Override
  public int getMaxPrefLabelTerms() {
    
    return this.maxPrefLabelTerms;
    
  }
  
  /**
   * Returns the values of a given field for a given concept
   * 
   * @param conceptURI
   * @param field
   * @return
   * @throws IOException
   */
  private String[] readConceptFieldValues(String conceptURI, String field)
      throws IOException {
    
    Query query = new TermQuery(new Term(SKOSEngineImpl.FIELD_URI, conceptURI));
    
    TopDocs docs = searcher.search(query, 1);
    
    ScoreDoc[] results = docs.scoreDocs;
    
    if (results.length != 1) {
      System.out.println("Unknonwn concept " + conceptURI);
      return null;
    }
    
    Document conceptDoc = searcher.doc(results[0].doc);
    
    String[] fieldValues = conceptDoc.getValues(field);
    
    return fieldValues;
    
  }
  
  /**
   * Creates the synonym index
   * 
   * @throws IOException
   */
  private void indexSKOSModel() throws IOException {
    
    IndexWriter writer = new IndexWriter(indexDir, analyzer,
        IndexWriter.MaxFieldLength.UNLIMITED);
    
    /* iterate SKOS concepts, create Lucene docs and add them to the index */
    
    ResIterator concept_iter = skosModel.listResourcesWithProperty(RDF.type,
        SKOS.Concept);
    while (concept_iter.hasNext()) {
      
      Resource skos_concept = concept_iter.next();
      
      Document concept_doc = createDocumentsFromConcept(skos_concept);
      
      // System.out.println("Adding document to index " + concept_doc);
      
      writer.addDocument(concept_doc);
      
    }
    
    writer.close();
    
  }
  
  /**
   * Creates lucene documents from SKOS concept. In order to allow language
   * restrictions, one document per language is created.
   * 
   * @param skos_concept
   * @return
   */
  private Document createDocumentsFromConcept(Resource skos_concept) {
    
    Document conceptDoc = new Document();
    
    String conceptURI = skos_concept.getURI();
    
    // store and index the concept URI
    Field uriField = new Field(SKOSEngineImpl.FIELD_URI, conceptURI,
        Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
    conceptDoc.add(uriField);
    
    // index the preferred lexical labels
    StmtIterator stmt_iter = skos_concept.listProperties(SKOS.prefLabel);
    while (stmt_iter.hasNext()) {
      
      Literal prefLabelLiteral = stmt_iter.nextStatement().getObject()
          .as(Literal.class);
      String prefLabel = prefLabelLiteral.getLexicalForm();
      String labelLang = prefLabelLiteral.getLanguage();
      
      if (this.languages != null && !this.languages.contains(labelLang)) {
        continue;
      }
      
      // converting label to lower-case
      prefLabel = prefLabel.toLowerCase();
      
      Field prefLabelField = new Field(SKOSEngineImpl.FIELD_PREF_LABEL,
          prefLabel, Field.Store.YES, Field.Index.NOT_ANALYZED);
      
      conceptDoc.add(prefLabelField);
      
      // count the number of terms in the label for determing max terms
      int noTerms = countLabelTerms(prefLabel);
      if (maxPrefLabelTerms < noTerms) {
        maxPrefLabelTerms = noTerms;
      }
      
    }
    
    // store the alternative lexical labels
    stmt_iter = skos_concept.listProperties(SKOS.altLabel);
    while (stmt_iter.hasNext()) {
      
      Literal altLabelLiteral = stmt_iter.nextStatement().getObject()
          .as(Literal.class);
      String altLabel = altLabelLiteral.getLexicalForm();
      String labelLang = altLabelLiteral.getLanguage();
      
      // converting label to lower-case
      altLabel = altLabel.toLowerCase();
      
      if (this.languages != null && !this.languages.contains(labelLang)) {
        continue;
      }
      
      Field altLabelField = new Field(SKOSEngineImpl.FIELD_ALT_LABEL, altLabel,
          Field.Store.YES, Field.Index.NO);
      
      conceptDoc.add(altLabelField);
      
    }
    
    // store the URIs of the broader concepts
    stmt_iter = skos_concept.listProperties(SKOS.broader);
    while (stmt_iter.hasNext()) {
      
      RDFNode broaderConcept = stmt_iter.nextStatement().getObject();
      
      if (!broaderConcept.canAs(Resource.class)) {
        System.err
            .println("Error when indexing broader relationship of concept "
                + skos_concept.getURI() + " .");
        continue;
      }
      
      Resource bc = (Resource) broaderConcept.as(Resource.class);
      
      Field broaderConceptField = new Field(SKOSEngineImpl.FIELD_BROADER,
          bc.getURI(), Field.Store.YES, Field.Index.NO);
      
      conceptDoc.add(broaderConceptField);
      
    }
    
    // store the URIs of the narrower concepts
    stmt_iter = skos_concept.listProperties(SKOS.narrower);
    while (stmt_iter.hasNext()) {
      
      RDFNode narrowerConcept = stmt_iter.nextStatement().getObject();
      
      if (!narrowerConcept.canAs(Resource.class)) {
        System.err
            .println("Error when indexing narrower relationship of concept "
                + skos_concept.getURI() + " .");
        continue;
        
      }
      
      Resource nc = (Resource) narrowerConcept.as(Resource.class);
      
      Field narrowerConceptField = new Field(SKOSEngineImpl.FIELD_NARROWER,
          nc.getURI(), Field.Store.YES, Field.Index.NO);
      
      conceptDoc.add(narrowerConceptField);
      
    }
    
    return conceptDoc;
    
  }
  
  /**
   * Returns the number of (whitespace separated) terms contained in a label
   * 
   * @param label
   * @return
   */
  private int countLabelTerms(String label) {
    
    return label.split(" ").length;
    
  }
  
  /**
   * Records the total number of matches
   * 
   * @author haslhofer
   * 
   */
  public static class AllDocCollector extends Collector {
    
    List<ScoreDoc> docs = new ArrayList<ScoreDoc>();
    
    private Scorer scorer;
    
    private int docBase;
    
    public boolean acceptsDocsOutOfOrder() {
      return true;
    }
    
    public void setScorer(Scorer scorer) {
      this.scorer = scorer;
    }
    
    public void setNextReader(IndexReader reader, int docBase) {
      this.docBase = docBase;
    }
    
    public void collect(int doc) throws IOException {
      docs.add(new ScoreDoc(doc + docBase, scorer.score()));
    }
    
    public void reset() {
      docs.clear();
    }
    
    public List<ScoreDoc> getHits() {
      return docs;
    }
    
  }
  
}
