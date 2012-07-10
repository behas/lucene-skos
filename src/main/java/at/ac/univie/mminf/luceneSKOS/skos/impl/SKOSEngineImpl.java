package at.ac.univie.mminf.luceneSKOS.skos.impl;

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
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
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

import com.hp.hpl.jena.ontology.AnnotationProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
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
  
  /** Records the total number of matches */
  public static class AllDocCollector extends Collector {
    private final List<Integer> docs = new ArrayList<Integer>();
    private int base;
    
    @Override
    public boolean acceptsDocsOutOfOrder() {
      return true;
    }
    
    @Override
    public void collect(int doc) throws IOException {
      docs.add(doc + base);
    }
    
    public List<Integer> getDocs() {
      return docs;
    }
    
    @Override
    public void setNextReader(IndexReader reader, int docBase)
        throws IOException {
      base = docBase;
    }
    
    @Override
    public void setScorer(Scorer scorer) throws IOException {
      // not needed
    }
  }
  
  protected final Version matchVersion;
  
  /*
   * Static fields used in the Lucene Index
   */
  private static final String FIELD_URI = "uri";
  private static final String FIELD_PREF_LABEL = "pref";
  private static final String FIELD_ALT_LABEL = "alt";
  private static final String FIELD_HIDDEN_LABEL = "hidden";
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
   */
  private final Analyzer analyzer;
  
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
  public SKOSEngineImpl(final Version version, InputStream inputStream,
      String lang) throws IOException {
    
    if (!("N3".equals(lang) || "RDF/XML".equals(lang) || "TURTLE".equals(lang))) {
      throw new IOException("Invalid RDF serialization format");
    }
    
    matchVersion = version;
    
    analyzer = new SimpleAnalyzer(matchVersion);
    
    skosModel = ModelFactory.createDefaultModel();
    
    skosModel.read(inputStream, null, lang);
    
    indexDir = new RAMDirectory();
    
    indexSKOSModel();
    
    searcher = new IndexSearcher(IndexReader.open(indexDir));
  }
  
  /**
   * Constructor for all label-languages
   * 
   * @param filenameOrURI
   *          the name of the skos file to be loaded
   * @throws IOException
   */
  public SKOSEngineImpl(final Version version, String filenameOrURI)
      throws IOException {
    this(version, filenameOrURI, (String[]) null);
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
  public SKOSEngineImpl(final Version version, String filenameOrURI,
      String... languages) throws IOException {
    matchVersion = version;
    analyzer = new SimpleAnalyzer(matchVersion);
    
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
      FileManager fileManager = new FileManager();
      fileManager.addLocatorFile();
      fileManager.addLocatorURL();
      fileManager.addLocatorClassLoader(SKOSEngineImpl.class.getClassLoader());
      
      if (FilenameUtils.getExtension(filenameOrURI).equals("zip")) {
        fileManager.addLocatorZip(filenameOrURI);
        filenameOrURI = FilenameUtils.getBaseName(filenameOrURI);
      }
      
      skosModel = fileManager.loadModel(filenameOrURI);
      
      indexSKOSModel();
    }
    
    searcher = new IndexSearcher(IndexReader.open(indexDir));
  }
  
  /**
   * Creates lucene documents from SKOS concept. In order to allow language
   * restrictions, one document per language is created.
   */
  private Document createDocumentsFromConcept(Resource skos_concept) {
    Document conceptDoc = new Document();
    
    String conceptURI = skos_concept.getURI();
    Field uriField = new Field(FIELD_URI, conceptURI, Field.Store.YES,
        Field.Index.NOT_ANALYZED_NO_NORMS);
    conceptDoc.add(uriField);
    
    // store the preferred lexical labels
    indexAnnotation(skos_concept, conceptDoc, SKOS.prefLabel, FIELD_PREF_LABEL);
    
    // store the alternative lexical labels
    indexAnnotation(skos_concept, conceptDoc, SKOS.altLabel, FIELD_ALT_LABEL);
    
    // store the hidden lexical labels
    indexAnnotation(skos_concept, conceptDoc, SKOS.hiddenLabel,
        FIELD_HIDDEN_LABEL);
    
    // store the URIs of the broader concepts
    indexObject(skos_concept, conceptDoc, SKOS.broader, FIELD_BROADER);
    
    // store the URIs of the broader transitive concepts
    indexObject(skos_concept, conceptDoc, SKOS.broaderTransitive,
        FIELD_BROADER_TRANSITIVE);
    
    // store the URIs of the narrower concepts
    indexObject(skos_concept, conceptDoc, SKOS.narrower, FIELD_NARROWER);
    
    // store the URIs of the narrower transitive concepts
    indexObject(skos_concept, conceptDoc, SKOS.narrowerTransitive,
        FIELD_NARROWER_TRANSITIVE);
    
    // store the URIs of the related concepts
    indexObject(skos_concept, conceptDoc, SKOS.related, FIELD_RELATED);
    
    return conceptDoc;
  }
  
  @Override
  public String[] getAltLabels(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, FIELD_ALT_LABEL);
  }
  
  @Override
  public String[] getAltTerms(String prefLabel) throws IOException {
    List<String> result = new ArrayList<String>();
    
    // convert the query to lower-case
    String queryString = prefLabel.toLowerCase();
    
    try {
      String[] conceptURIs = getConcepts(queryString);
      
      for (String conceptURI : conceptURIs) {
        String[] labels = getAltLabels(conceptURI);
        if (labels != null) {
          for (String label : labels) {
            result.add(label);
          }
        }
      }
    } catch (Exception e) {
      System.err
          .println("Error when accessing SKOS Engine.\n" + e.getMessage());
    }
    
    return result.toArray(new String[result.size()]);
  }
  
  @Override
  public String[] getHiddenLabels(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, FIELD_HIDDEN_LABEL);
  }
  
  @Override
  public String[] getBroaderConcepts(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, FIELD_BROADER);
  }
  
  @Override
  public String[] getBroaderLabels(String conceptURI) throws IOException {
    return getLabels(conceptURI, FIELD_BROADER);
  }
  
  @Override
  public String[] getBroaderTransitiveConcepts(String conceptURI)
      throws IOException {
    return readConceptFieldValues(conceptURI, FIELD_BROADER_TRANSITIVE);
  }
  
  @Override
  public String[] getBroaderTransitiveLabels(String conceptURI)
      throws IOException {
    return getLabels(conceptURI, FIELD_BROADER_TRANSITIVE);
  }
  
  @Override
  public String[] getConcepts(String prefLabel) throws IOException {
    List<String> concepts = new ArrayList<String>();
    
    // convert the query to lower-case
    String queryString = prefLabel.toLowerCase();
    
    AllDocCollector collector = new AllDocCollector();
    
    DisjunctionMaxQuery query = new DisjunctionMaxQuery(0.0f);
    query.add(new TermQuery(new Term(FIELD_PREF_LABEL, queryString)));
    query.add(new TermQuery(new Term(FIELD_ALT_LABEL, queryString)));
    query.add(new TermQuery(new Term(FIELD_HIDDEN_LABEL, queryString)));
    searcher.search(query, collector);
    
    for (Integer hit : collector.getDocs()) {
      Document doc = searcher.doc(hit);
      String conceptURI = doc.getValues(FIELD_URI)[0];
      concepts.add(conceptURI);
    }
    
    return concepts.toArray(new String[concepts.size()]);
  }
  
  private String[] getLabels(String conceptURI, String field)
      throws IOException {
    List<String> labels = new ArrayList<String>();
    String[] concepts = readConceptFieldValues(conceptURI, field);
    
    for (String aConceptURI : concepts) {
      String[] prefLabels = getPrefLabels(aConceptURI);
      labels.addAll(Arrays.asList(prefLabels));
      
      String[] altLabels = getAltLabels(aConceptURI);
      labels.addAll(Arrays.asList(altLabels));
    }
    
    return labels.toArray(new String[labels.size()]);
  }
  
  @Override
  public String[] getNarrowerConcepts(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, FIELD_NARROWER);
  }
  
  @Override
  public String[] getNarrowerLabels(String conceptURI) throws IOException {
    return getLabels(conceptURI, FIELD_NARROWER);
  }
  
  @Override
  public String[] getNarrowerTransitiveConcepts(String conceptURI)
      throws IOException {
    return readConceptFieldValues(conceptURI, FIELD_NARROWER_TRANSITIVE);
  }
  
  @Override
  public String[] getNarrowerTransitiveLabels(String conceptURI)
      throws IOException {
    return getLabels(conceptURI, FIELD_NARROWER_TRANSITIVE);
  }
  
  @Override
  public String[] getPrefLabels(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, FIELD_PREF_LABEL);
  }
  
  @Override
  public String[] getRelatedConcepts(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, FIELD_RELATED);
  }
  
  @Override
  public String[] getRelatedLabels(String conceptURI) throws IOException {
    return getLabels(conceptURI, FIELD_RELATED);
  }
  
  private void indexAnnotation(Resource skos_concept, Document conceptDoc,
      AnnotationProperty property, String field) {
    StmtIterator stmt_iter = skos_concept.listProperties(property);
    while (stmt_iter.hasNext()) {
      Literal labelLiteral = stmt_iter.nextStatement().getObject()
          .as(Literal.class);
      String label = labelLiteral.getLexicalForm();
      String labelLang = labelLiteral.getLanguage();
      
      if (this.languages != null && !this.languages.contains(labelLang)) {
        continue;
      }
      
      // converting label to lower-case
      label = label.toLowerCase();
      
      Field labelField = new Field(field, label, Field.Store.YES,
          Field.Index.NOT_ANALYZED);
      
      conceptDoc.add(labelField);
    }
  }
  
  private void indexObject(Resource skos_concept, Document conceptDoc,
      ObjectProperty property, String field) {
    StmtIterator stmt_iter = skos_concept.listProperties(property);
    while (stmt_iter.hasNext()) {
      RDFNode concept = stmt_iter.nextStatement().getObject();
      
      if (!concept.canAs(Resource.class)) {
        System.err.println("Error when indexing relationship of concept "
            + skos_concept.getURI() + " .");
        continue;
      }
      
      Resource resource = concept.as(Resource.class);
      
      Field conceptField = new Field(field, resource.getURI(), Field.Store.YES,
          Field.Index.NO);
      
      conceptDoc.add(conceptField);
    }
  }
  
  /**
   * Creates the synonym index
   * 
   * @throws IOException
   */
  private void indexSKOSModel() throws IOException {
    IndexWriterConfig cfg = new IndexWriterConfig(matchVersion, analyzer);
    IndexWriter writer = new IndexWriter(indexDir, cfg);
    writer.getConfig().setRAMBufferSizeMB(48);
    
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
  
  /** Returns the values of a given field for a given concept */
  private String[] readConceptFieldValues(String conceptURI, String field)
      throws IOException {
    
    Query query = new TermQuery(new Term(FIELD_URI, conceptURI));
    
    TopDocs docs = searcher.search(query, 1);
    
    ScoreDoc[] results = docs.scoreDocs;
    
    if (results.length != 1) {
      System.out.println("Unknown concept " + conceptURI);
      return null;
    }
    
    Document conceptDoc = searcher.doc(results[0].doc);
    
    return conceptDoc.getValues(field);
  }
  
}
