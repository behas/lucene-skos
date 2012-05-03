package at.ac.univie.mminf.luceneSKOS.skos.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

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
  private static final String FIELD_BROADER_TRANSITIVE = "broaderTransitive";
  private static final String FIELD_NARROWER_TRANSITIVE = "narrowerTransitive";
  private static final String FIELD_RELATED = "related";
  
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
  private Set<String> languages;
  
  /**
   * The analyzer used during indexing of / querying for concepts
   * 
   * SimpleAnalyzer = LetterTokenizer + LowerCaseFilter
   * 
   */
  private Analyzer analyzer = new SimpleAnalyzer(Version.LUCENE_40);
  
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
    
    indexDir = new RAMDirectory();
    
    indexSKOSModel();
    
    searcher = new IndexSearcher(DirectoryReader.open(indexDir));
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
    String langSig = "";
    if (languages != null) {
      this.languages = new TreeSet<String>(Arrays.asList(languages));
      langSig = "-" + StringUtils.join(this.languages, ".");
    }
    
    String baseName = FilenameUtils.getBaseName(filenameOrURI);
    File dir = new File("skosdata/" + baseName + langSig);
    indexDir = FSDirectory.open(dir);
    
    // TODO: Generate also if source file is modified
    if (!dir.isDirectory()) {
      // load the skos model from the given file
      skosModel = FileManager.get().loadModel(filenameOrURI);
      
      indexSKOSModel();
    }
    
    searcher = new IndexSearcher(DirectoryReader.open(indexDir));
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
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getRelatedConcepts(java
   * .lang.String)
   */
  @Override
  public String[] getRelatedConcepts(String conceptURI) throws IOException {
    
    return readConceptFieldValues(conceptURI, FIELD_RELATED);
    
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
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getBroaderTransitiveConcepts
   * (java .lang.String)
   */
  @Override
  public String[] getBroaderTransitiveConcepts(String conceptURI)
      throws IOException {
    
    return readConceptFieldValues(conceptURI, FIELD_BROADER_TRANSITIVE);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getNarrowerTransitiveConcepts
   * (java .lang.String)
   */
  @Override
  public String[] getNarrowerTransitiveConcepts(String conceptURI)
      throws IOException {
    
    return readConceptFieldValues(conceptURI, FIELD_NARROWER_TRANSITIVE);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getRelatedLabels(java.lang
   * .String)
   */
  @Override
  public String[] getRelatedLabels(String conceptURI) throws IOException {
    
    List<String> relatedLabels = new ArrayList<String>();
    
    String[] relatedConcepts = getRelatedConcepts(conceptURI);
    
    for (String brConceptURI : relatedConcepts) {
      String[] relPrefLabels = getPrefLabels(brConceptURI);
      relatedLabels.addAll(Arrays.asList(relPrefLabels));
      
      String[] relAltLabels = getAltLabels(brConceptURI);
      relatedLabels.addAll(Arrays.asList(relAltLabels));
      
    }
    
    return relatedLabels.toArray(new String[0]);
    
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
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getBroaderTransitiveLabels
   * (java.lang .String)
   */
  @Override
  public String[] getBroaderTransitiveLabels(String conceptURI)
      throws IOException {
    
    List<String> broaderLabels = new ArrayList<String>();
    
    String[] broaderConcepts = getBroaderTransitiveConcepts(conceptURI);
    
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
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getNarrowerTransitiveLabels
   * (java. lang.String)
   */
  @Override
  public String[] getNarrowerTransitiveLabels(String conceptURI)
      throws IOException {
    
    List<String> narrowerLabels = new ArrayList<String>();
    
    String[] narrowerConcepts = getNarrowerTransitiveConcepts(conceptURI);
    
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
    
    DisjunctionMaxQuery query1 = new DisjunctionMaxQuery(0.0f);
    query1.add(new TermQuery(new Term(SKOSEngineImpl.FIELD_PREF_LABEL,
        queryString)));
    query1.add(new TermQuery(new Term(SKOSEngineImpl.FIELD_ALT_LABEL,
        queryString)));
    searcher.search(query1, collector);
    
    for (Integer hit : collector.getDocs()) {
      Document doc = searcher.doc(hit);
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
    
    for (Integer hit : collector.getDocs()) {
      
      Document doc = searcher.doc(hit);
      
      String[] values = doc.getValues(SKOSEngineImpl.FIELD_ALT_LABEL);
      
      for (String syn : values) {
        synList.add(syn);
      }
      
    }
    
    return synList.toArray(new String[0]);
    
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
    
    IndexWriterConfig cfg = new IndexWriterConfig(Version.LUCENE_40, analyzer);
    
    IndexWriter writer = new IndexWriter(indexDir, cfg);
    
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
    
    Field uriField = new Field(SKOSEngineImpl.FIELD_URI, conceptURI,
        StringField.TYPE_STORED);
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
          prefLabel, StringField.TYPE_STORED);
      
      conceptDoc.add(prefLabelField);
      
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
          StringField.TYPE_STORED);
      
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
      
      Resource bc = broaderConcept.as(Resource.class);
      
      Field broaderConceptField = new Field(SKOSEngineImpl.FIELD_BROADER,
          bc.getURI(), StringField.TYPE_STORED);
      
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
      
      Resource nc = narrowerConcept.as(Resource.class);
      
      Field narrowerConceptField = new Field(SKOSEngineImpl.FIELD_NARROWER,
          nc.getURI(), StringField.TYPE_STORED);
      
      conceptDoc.add(narrowerConceptField);
      
    }
    
    // store the URIs of the broader transitive concepts
    stmt_iter = skos_concept.listProperties(SKOS.broaderTransitive);
    while (stmt_iter.hasNext()) {
      
      RDFNode broaderConcept = stmt_iter.nextStatement().getObject();
      
      if (!broaderConcept.canAs(Resource.class)) {
        System.err
            .println("Error when indexing broader transitive relationship of concept "
                + skos_concept.getURI() + " .");
        continue;
      }
      
      Resource bc = broaderConcept.as(Resource.class);
      
      Field broaderConceptField = new Field(
          SKOSEngineImpl.FIELD_BROADER_TRANSITIVE, bc.getURI(),
          StringField.TYPE_STORED);
      
      conceptDoc.add(broaderConceptField);
      
    }
    
    // store the URIs of the narrower transitive concepts
    stmt_iter = skos_concept.listProperties(SKOS.narrowerTransitive);
    while (stmt_iter.hasNext()) {
      
      RDFNode narrowerConcept = stmt_iter.nextStatement().getObject();
      
      if (!narrowerConcept.canAs(Resource.class)) {
        System.err
            .println("Error when indexing narrower transitive relationship of concept "
                + skos_concept.getURI() + " .");
        continue;
        
      }
      
      Resource nc = narrowerConcept.as(Resource.class);
      
      Field narrowerConceptField = new Field(
          SKOSEngineImpl.FIELD_NARROWER_TRANSITIVE, nc.getURI(),
          StringField.TYPE_STORED);
      
      conceptDoc.add(narrowerConceptField);
      
    }
    
    // store the URIs of the related concepts
    stmt_iter = skos_concept.listProperties(SKOS.related);
    while (stmt_iter.hasNext()) {
      
      RDFNode relatedConcept = stmt_iter.nextStatement().getObject();
      
      if (!relatedConcept.canAs(Resource.class)) {
        System.err
            .println("Error when indexing related relationship of concept "
                + skos_concept.getURI() + " .");
        continue;
        
      }
      
      Resource nc = relatedConcept.as(Resource.class);
      
      Field relatedConceptField = new Field(SKOSEngineImpl.FIELD_RELATED,
          nc.getURI(), StringField.TYPE_STORED);
      
      conceptDoc.add(relatedConceptField);
      
    }
    
    return conceptDoc;
    
  }
  
  /**
   * Records the total number of matches
   * 
   * @author haslhofer
   * 
   */
  public static class AllDocCollector extends Collector {
    List<Integer> docs = new ArrayList<Integer>();
    int base;
    
    @Override
    public boolean acceptsDocsOutOfOrder() {
      return true;
    }
    
    @Override
    public void setScorer(Scorer scorer) throws IOException {}
    
    @Override
    public void collect(int doc) throws IOException {
      doc += base;
      docs.add(doc);
    }
    
    public List<Integer> getDocs() {
      return docs;
    }
    
    @Override
    public void setNextReader(AtomicReaderContext context) throws IOException {
      base = context.docBase;
    }
  }
}
