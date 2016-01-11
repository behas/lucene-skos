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
import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.update.GraphStore;
import org.apache.jena.update.GraphStoreFactory;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDF;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;

import at.ac.univie.mminf.luceneSKOS.skos.SKOS;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine;

/**
 * A Lucene-backed SKOSEngine Implementation. Each SKOS concept is stored/indexed as a Lucene document. All labels are converted to lowercase
 */
public class SKOSEngineImpl implements SKOSEngine {

    /** Records the total number of matches */
    public static class AllDocCollector implements Collector {
        private final List<Integer> docs = new ArrayList<Integer>();
        private int base;

        public List<Integer> getDocs() {
            return docs;
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
            // not needed
            base = context.docBase;
            return new LeafCollector() {

                @Override
                public void setScorer(Scorer scorer) throws IOException {
                    // Not needed

                }

                @Override
                public void collect(int doc) throws IOException {
                    docs.add(base + doc);
                }
            };
        }

        @Override
        public boolean needsScores() {
            // not needed
            return false;
        }
    }

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
     * The languages to be considered when returning labels. If NULL, all languages are supported
     */
    private Set<String> languages;

    /**
     * The analyzer used during indexing of / querying for concepts SimpleAnalyzer = LetterTokenizer + LowerCaseFilter
     */
    private final Analyzer analyzer;

    /**
     * This constructor loads the SKOS model from a given InputStream using the given serialization language parameter, which must be either N3, RDF/XML, or TURTLE.
     * 
     * @param inputStream the input stream
     * @param lang the serialization language
     * @throws IOException if the model cannot be loaded
     */
    public SKOSEngineImpl(InputStream inputStream, String lang) throws IOException {

        if (!("N3".equals(lang) || "RDF/XML".equals(lang) || "TURTLE".equals(lang))) {
            throw new IOException("Invalid RDF serialization format");
        }

        analyzer = new SimpleAnalyzer();

        skosModel = ModelFactory.createDefaultModel();

        skosModel.read(inputStream, null, lang);

        indexDir = new RAMDirectory();

        entailSKOSModel();

        indexSKOSModel();

        searcher = new IndexSearcher(DirectoryReader.open(indexDir));
    }

    /**
     * Constructor for all label-languages
     * 
     * @param filenameOrURI the name of the skos file to be loaded
     * @throws IOException
     */
    public SKOSEngineImpl(String filenameOrURI) throws IOException {
        this(filenameOrURI, (String[]) null);
    }

    /**
     * This constructor loads the SKOS model from a given filename or URI, starts the indexing process and sets up the index searcher.
     * 
     * @param languages the languages to be considered
     * @param filenameOrURI
     * @throws IOException
     */
    public SKOSEngineImpl(String filenameOrURI, String... languages) throws IOException {
        analyzer = new SimpleAnalyzer();

        String langSig = "";
        if (languages != null) {
            this.languages = new TreeSet<String>(Arrays.asList(languages));
            langSig = "-" + StringUtils.join(this.languages, ".");
        }

        String name = FilenameUtils.getName(filenameOrURI);
        File dir = new File("skosdata/" + name + langSig);
        boolean regenerate = !dir.exists();
        indexDir = FSDirectory.open(dir.toPath());

        // TODO: Generate also if source file is modified
        if (regenerate) {
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

            entailSKOSModel();

            indexSKOSModel();
        }

        searcher = new IndexSearcher(DirectoryReader.open(indexDir));
    }

    private void entailSKOSModel() {
        GraphStore graphStore = GraphStoreFactory.create(skosModel);
        String sparqlQuery = StringUtils.join(new String[] { "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>", "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>", "INSERT { ?subject rdf:type skos:Concept }", "WHERE {", "{ ?subject skos:prefLabel ?text } UNION", "{ ?subject skos:altLabel ?text } UNION", "{ ?subject skos:hiddenLabel ?text }", "}", }, "\n");
        UpdateRequest request = UpdateFactory.create(sparqlQuery);
        UpdateAction.execute(request, graphStore);
    }

    /**
     * Creates lucene documents from SKOS concept. In order to allow language restrictions, one document per language is created.
     */
    private Document createDocumentsFromConcept(Resource skos_concept) {
        Document conceptDoc = new Document();

        String conceptURI = skos_concept.getURI();
        if (conceptURI == null) {
            System.err.println("Error when indexing concept NO_URI.");
            return null;
        }

        Field uriField = new Field(FIELD_URI, conceptURI, StringField.TYPE_STORED);
        conceptDoc.add(uriField);

        // store the preferred lexical labels
        indexAnnotation(skos_concept, conceptDoc, SKOS.prefLabel, FIELD_PREF_LABEL);

        // store the alternative lexical labels
        indexAnnotation(skos_concept, conceptDoc, SKOS.altLabel, FIELD_ALT_LABEL);

        // store the hidden lexical labels
        indexAnnotation(skos_concept, conceptDoc, SKOS.hiddenLabel, FIELD_HIDDEN_LABEL);

        // store the URIs of the broader concepts
        indexObject(skos_concept, conceptDoc, SKOS.broader, FIELD_BROADER);

        // store the URIs of the broader transitive concepts
        indexObject(skos_concept, conceptDoc, SKOS.broaderTransitive, FIELD_BROADER_TRANSITIVE);

        // store the URIs of the narrower concepts
        indexObject(skos_concept, conceptDoc, SKOS.narrower, FIELD_NARROWER);

        // store the URIs of the narrower transitive concepts
        indexObject(skos_concept, conceptDoc, SKOS.narrowerTransitive, FIELD_NARROWER_TRANSITIVE);

        // store the URIs of the related concepts
        indexObject(skos_concept, conceptDoc, SKOS.related, FIELD_RELATED);

        return conceptDoc;
    }

    @Override
    public String[] getAltLabels(String conceptURI) throws IOException {
        return readConceptFieldValues(conceptURI, FIELD_ALT_LABEL);
    }

    @Override
    public String[] getAltTerms(String label) throws IOException {
        List<String> result = new ArrayList<String>();

        // convert the query to lower-case
        String queryString = label.toLowerCase();

        try {
            String[] conceptURIs = getConcepts(queryString);

            for (String conceptURI : conceptURIs) {
                String[] altLabels = getAltLabels(conceptURI);
                if (altLabels != null) {
                    for (String altLabel : altLabels) {
                        result.add(altLabel);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error when accessing SKOS Engine.\n" + e.getMessage());
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
    public String[] getBroaderTransitiveConcepts(String conceptURI) throws IOException {
        return readConceptFieldValues(conceptURI, FIELD_BROADER_TRANSITIVE);
    }

    @Override
    public String[] getBroaderTransitiveLabels(String conceptURI) throws IOException {
        return getLabels(conceptURI, FIELD_BROADER_TRANSITIVE);
    }

    @Override
    public String[] getConcepts(String label) throws IOException {
        List<String> concepts = new ArrayList<String>();

        // convert the query to lower-case
        String queryString = label.toLowerCase();

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

    private String[] getLabels(String conceptURI, String field) throws IOException {
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
    public String[] getNarrowerTransitiveConcepts(String conceptURI) throws IOException {
        return readConceptFieldValues(conceptURI, FIELD_NARROWER_TRANSITIVE);
    }

    @Override
    public String[] getNarrowerTransitiveLabels(String conceptURI) throws IOException {
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

    private void indexAnnotation(Resource skos_concept, Document conceptDoc, AnnotationProperty property, String field) {
        StmtIterator stmt_iter = skos_concept.listProperties(property);
        while (stmt_iter.hasNext()) {
            Literal labelLiteral = stmt_iter.nextStatement().getObject().as(Literal.class);
            String label = labelLiteral.getLexicalForm();
            String labelLang = labelLiteral.getLanguage();

            if (this.languages != null && !this.languages.contains(labelLang)) {
                continue;
            }

            // converting label to lower-case
            label = label.toLowerCase();

            Field labelField = new Field(field, label, StringField.TYPE_STORED);

            conceptDoc.add(labelField);
        }
    }

    private void indexObject(Resource skos_concept, Document conceptDoc, ObjectProperty property, String field) {
        StmtIterator stmt_iter = skos_concept.listProperties(property);
        while (stmt_iter.hasNext()) {
            RDFNode concept = stmt_iter.nextStatement().getObject();

            if (!concept.canAs(Resource.class)) {
                System.err.println("Error when indexing relationship of concept " + skos_concept.getURI() + ".");
                continue;
            }

            Resource resource = concept.as(Resource.class);

            String uri = resource.getURI();
            if (uri == null) {
                System.err.println("Error when indexing relationship of concept " + skos_concept.getURI() + ".");
                continue;
            }

            Field conceptField = new Field(field, uri, StringField.TYPE_STORED);

            conceptDoc.add(conceptField);
        }
    }

    /**
     * Creates the synonym index
     * 
     * @throws IOException
     */
    private void indexSKOSModel() throws IOException {
        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(indexDir, cfg);
        writer.getConfig().setRAMBufferSizeMB(48);

        /* iterate SKOS concepts, create Lucene docs and add them to the index */
        ResIterator concept_iter = skosModel.listResourcesWithProperty(RDF.type, SKOS.Concept);
        while (concept_iter.hasNext()) {
            Resource skos_concept = concept_iter.next();

            Document concept_doc = createDocumentsFromConcept(skos_concept);
            if (concept_doc != null) {
                writer.addDocument(concept_doc);
            }
        }

        writer.close();
    }

    /** Returns the values of a given field for a given concept */
    private String[] readConceptFieldValues(String conceptURI, String field) throws IOException {

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
