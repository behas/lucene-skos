# SKOS Support for Apache Lucene and Solr

## What is SKOS?

The [Simple Knowledge Organization System (SKOS)][skos] is a model for expressing controlled structured vocabularies (classification schemes, thesauri, taxonomies, etc.). As an application of the [Resource Description Framework (RDF)][rdf], SKOS allows these vocabularies to be published as dereferenceable resources on the Web, which makes them easy to retrieve and reuse in applications. SKOS plays a major role in the ongoing [Linked Data][ld] activities.

## What is lucene-skos?

lucene-skos is an analyzer module for [Apache Lucene 3.x][lucene] and [Solr 3.x][solr]. It takes existing SKOS concepts schemes and performs term expansion for given Lucene documents and/or queries. At the moment, the implementation provides custom SKOS [Analyzers](https://lucene.apache.org/core/3_6_1/api/all/org/apache/lucene/analysis/Analyzer.html) and [TokenFilters](https://lucene.apache.org/core/3_6_1/api/all/org/apache/lucene/analysis/TokenFilter.html).

## Features

The module supports the following use cases:

 * [UC 1](https://github.com/behas/lucene-skos/wiki/UseCases): Expansion of URI terms to SKOS labels: URI-references to SKOS concepts in given Lucene documents are expanded by the labels behind those concepts.

 * [UC2](https://github.com/behas/lucene-skos/wiki/UseCases): Expansion of text terms to SKOS labels: Labels in given Lucene documents, which are defined as preferred concept labels in a given SKOS vocabulary, are expanded by additional labels defined in that vocabulary.

## Installation

The SKOS Analyzer Module can be used with [Apache Lucene][lucene] and [Solr][solr].

### Download Binaries

Download and unzip an archive from the [download area](https://github.com/behas/lucene-skos/downloads) and copy all contained jars to your applications' classpath.

### Build from Source

Make sure you have [Apache Maven](http://maven.apache.org/) installed. Verify this as follows:

    mvn --version

Check out the sources

    git clone git://github.com/behas/lucene-skos.git

Build and package the sources

    cd lucene-skos
    mvn package

Choose and unpack a lucene-skos archive from the _dist_ subdirectory

    mkdir dist/out
    tar -xzf dist/lucene-skos-0.2.tar.gz -C dist/out

### Using lucene-skos with Apache Lucene

If you want to use lucene-skos in an application that already uses Lucene make sure that the jar `lucene-skos-0.2.jar` and all its dependencies (currently only [Jena][jena]) are located in your classpath (= build path in Eclipse).

### Using lucene-skos with Apache Solr

Installing lucene-skos with Solr largely depends on how you deployed Solr in your environment. Follow the _Getting Started_ section of the [Solr Tutorial](http://lucene.apache.org/solr/tutorial.html) and make sure you can start up your Solr instance. Make sure _SOLR_HOME_ is set.

    cd $SOLR_HOME/example
    java -jar start.jar

Stop Solr

    CTRL-C

Download or build the lucene-skos jar and its dependencies as described previously and extract the obtained archive.

Create a folder _lib_ in your _SOLR_HOME_ directory. In the default installation $SOLR_HOME$ is ./apache-solr-x.x.x/example/solr

    cd $SOLR_HOME/example/solr
    mkdir lib
    cp lucene-skos-directory/*.jar $SOLR_HOME/example/solr/lib

Download an [example thesaurus](./docs/solr) and copy it to $SOLR_HOME/solr/conf 

    cp lucene-skos-directory/docs/solr/ukat_examples.n3 $SOLR_HOME/conf/

## UC1: URI-based term expansion

The analyzer module can be used to expand references to SKOS concepts in given Lucene documents by the concepts' labels. 

### Lucene

Create a [Lucene Document](https://lucene.apache.org/core/3_6_1/api/core/org/apache/lucene/document/Document.html) containing the data to be indexed. In this case, the document's subject field contains a link to a SKOS concept: http://www.ukat.org.uk/thesaurus/concept/859

    Document doc = new Document();
    doc.add(new Field("title", "Spearhead", Field.Store.YES, Field.Index.ANALYZED));
    doc.add(new Field("description",
        "Roman iron spearhead. The spearhead was attached to one end of a wooden shaft..."
                + "The spear was mainly a thrusting weapon, but could also be thrown. "
                + "It was the principal weapon of the auxiliary soldier... "
                + "(second - fourth century, Arbeia Roman Fort).", Field.Store.NO,
        Field.Index.ANALYZED));
    doc.add(new Field("subject", "http://www.ukat.org.uk/thesaurus/concept/859",
        Field.Store.NO, Field.Index.ANALYZED));

Instantiate a new SKOSAnalyzer instance by passing the path of the SKOS vocabulary serialization to be used. The parameter *ExpansionType.URI* indicates that the analyzer should perform URI-based term expansion. The argument matchVersion can be set to *Version.LUCENE_36* if you are using Lucene 3.6, for example.

    String skosFile = "src/test/resources/skos_samples/ukat_examples.n3";
    Analyzer skosAnalyzer = new SKOSAnalyzer(matchVersion, skosFile, ExpansionType.URI);

You might want to use different [Analyzers](https://lucene.apache.org/core/3_6_1/api/core/org/apache/lucene/analysis/Analyzer.html) for different [Lucene Fields](https://lucene.apache.org/core/3_6_1/api/core/org/apache/lucene/document/Field.html). Lucene's [PerFieldAnalyzerWrapper](https://lucene.apache.org/core/3_6_1/api/core/org/apache/lucene/analysis/PerFieldAnalyzerWrapper.html) is the solution for that. Here we apply our SKOSAnalyzer only for the *subject* field. For all other fields, the PerFieldAnalyzerWrapper falls back to a default [SimpleAnalyzer](https://lucene.apache.org/core/3_6_1/api/core/org/apache/lucene/analysis/SimpleAnalyzer.html) instance.

    Map<String,Analyzer> analyzerPerField = new HashMap<String,Analyzer>();
    analyzerPerField.put("subject", skosAnalyzer);
    PerFieldAnalyzerWrapper indexAnalyzer = new PerFieldAnalyzerWrapper(new SimpleAnalyzer(matchVersion), analyzerPerField);

Set up a writer using the previously created analyzer and add the document to the index.

    IndexWriter writer = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(matchVersion, indexAnalyzer));
    writer.addDocument(doc);

Now a search for *arms*, for instance, returns the indexed document in the result list because the SKOS URI http://www.ukat.org.uk/thesaurus/concept/859 has been expanded by terms defined in the vocabulary.

    BooleanQuery query1 = new BooleanQuery();
    query1.add(new TermQuery(new Term("title", "arms")), BooleanClause.Occur.SHOULD);
    query1.add(new TermQuery(new Term("description", "arms")), BooleanClause.Occur.SHOULD);
    query1.add(new TermQuery(new Term("subject", "arms")), BooleanClause.Occur.SHOULD);

    IndexSearcher searcher = new IndexSearcher(IndexReader.open(writer, false));

    TopDocs results = searcher.search(query1, 10);

    Assert.assertEquals(1, results.totalHits);

For the complete code, please check our [JUnit Test](https://github.com/behas/lucene-skos/blob/master/src/test/java/at/ac/univie/mminf/luceneSKOS/URIbasedTermExpansionTest.java) in the Git Repository

### Solr

Create the following fieldType definition in Solr's schema.xml file located in $SOLR_HOME$/conf/schema.xml

    <fieldType name="skosReference" class="solr.TextField"
    positionIncrementGap="100">
    <analyzer type="index">
        <tokenizer class="solr.KeywordTokenizerFactory" />
        <filter class="at.ac.univie.mminf.luceneSKOS.solr.SKOSFilterFactory"
            skosFile="ukat_examples.n3" expansionType="URI" />
    </analyzer>
    <analyzer type="query">
        <tokenizer class="solr.StandardTokenizerFactory" />
        <filter class="solr.LowerCaseFilterFactory" />
    </analyzer>
    </fieldType>

...and apply it to the subject field:

    <field name="subject" type="skosReference" indexed="true" stored="true" />

A complete, minimal _schema.xml_ configuration for the described use case is available [here](https://github.com/behas/lucene-skos/blob/master/docs/solr/schema.xml).

Now you can add a sample document by following the instructions described in _Indexing Data_ section of the [Solr tutorial](http://lucene.apache.org/solr/tutorial.html). Here is a sample:

    <add>
        <doc>
            <field name="id">record1</field>
            <field name="title">Spearhead</field>
            <field name="description">Roman iron spearhead. The spearhead was attached to one end of a wooden shaft.The spear was mainly a thrusting weapon, but could also be thrown. It was the principal weapon of the auxiliary soldier. (second - fourth century, Arbeia Roman Fort).</field>
            <field name="subject">http://www.ukat.org.uk/thesaurus/concept/859</field>
        </doc>
    </add>

Now a search for "subject:arms" or "subject:weapons" returns the indexed document in the results list because the SKOS URI http://www.ukat.org.uk/thesaurus/concept/859 has been expanded by terms defined in the vocabulary.


## UC2: Label-based term expansion

The module can also be used to expand plain text labels in given fields by corresponding labels in given SKOS vocabularies.

### Lucene

Similar to UC1 - URI-based_term expansion UC1, we create a Lucene document containing the data to be indexed. The difference is that this record doesn't contain a reference to a SKOS concept in the subject field but a plain text label "weapons".

    Document doc = new Document();
    doc.add(new Field("title", "Spearhead", Field.Store.YES, Field.Index.ANALYZED));
    doc.add(new Field("description",
        "Roman iron spearhead. The spearhead was attached to one end of a wooden shaft..."
                + "The spear was mainly a thrusting weapon, but could also be thrown. "
                + "It was the principal weapon of the auxiliary soldier... "
                + "(second - fourth century, Arbeia Roman Fort).", Field.Store.NO,
        Field.Index.ANALYZED));
    doc.add(new Field("subject", "weapons", Field.Store.NO, Field.Index.ANALYZED));

Instantiate a new SKOSAnalyzer instance by passing the path of the SKOS vocabulary serialization to be used. The parameter *ExpansionType.LABEL* indicates that the analyzer should perform Label-based term expansion.

    String skosFile = "src/test/resources/skos_samples/ukat_examples.n3";
    Analyzer skosAnalyzer = new SKOSAnalyzer(matchVersion, skosFile, ExpansionType.LABEL);

As in the previous use case, we define a PerFieldAnalyzerWrapper instance and index the document.

    Map<String,Analyzer> analyzerPerField = new HashMap<String,Analyzer>();
    analyzerPerField.put("subject", skosAnalyzer);
    PerFieldAnalyzerWrapper indexAnalyzer = new PerFieldAnalyzerWrapper(new SimpleAnalyzer(matchVersion), analyzerPerField);

    IndexWriter writer = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(matchVersion, indexAnalyzer));
    writer.addDocument(doc);

Now a search for "arms", for instance, returns the indexed document in the results list because the label "weapons", which is defined as prefLabel of some SKOS concept, has been expanded by additional terms including "arms".

    BooleanQuery query1 = new BooleanQuery();
    query1.add(new TermQuery(new Term("title", "arms")), BooleanClause.Occur.SHOULD);
    query1.add(new TermQuery(new Term("description", "arms")), BooleanClause.Occur.SHOULD);
    query1.add(new TermQuery(new Term("subject", "arms")), BooleanClause.Occur.SHOULD);

    IndexSearcher searcher = new IndexSearcher(IndexReader.open(writer, false));

    TopDocs results = searcher.search(query1, 10);

    Assert.assertEquals(1, results.totalHits);


For the complete code, please check our [JUnit Test](https://github.com/behas/lucene-skos/blob/master/src/test/java/at/ac/univie/mminf/luceneSKOS//LabelbasedTermExpansionTest.java).


### Solr

Follow the instructions from the previous use case with the following differences:

Introduce another fieldType...

    <fieldType name="skosLabel" class="solr.TextField"
    positionIncrementGap="100">
    <analyzer type="index">
        <tokenizer class="solr.StandardTokenizerFactory" />
        <filter class="solr.StandardFilterFactory" />
        <filter class="at.ac.univie.mminf.luceneSKOS.solr.SKOSFilterFactory"
            skosFile="ukat_examples.n3" expansionType="LABEL" bufferSize="2" />
        <filter class="solr.LowerCaseFilterFactory" />
    </analyzer>
    <analyzer type="query">
        <tokenizer class="solr.StandardTokenizerFactory" />
        <filter class="solr.LowerCaseFilterFactory" />
    </analyzer>
    </fieldType>

...and set the _subject_ field to that type

    <field name="subject" type="skosLabel" indexed="true" stored="true" />

In the example above the labels are not restricted to any specific language, however you can restrict them to the English language tag by adding language="en" to the filter attributes. You can also specify a list of languages like for example language="en pt" for English and Portuguese cross-language expansion.
Notice that bufferSize controls the maximum length (in number of words) of concept labels that will be checked for expansion.

Again, you can add a sample document such as the following and retrieve results for queries (e.g., subject:arms) containing terms that are not explicitly contained in the indexed document.

    <add>
        <doc>
            <field name="id">record2</field>
            <field name="title">Spearhead</field>
            <field name="description">Roman iron spearhead. The spearhead was attached to one end of a wooden shaft.The spear was mainly a thrusting weapon, but could also be thrown. It was the principal weapon of the auxiliary soldier. (second - fourth century, Arbeia Roman Fort).</field>
            <field name="subject">Weapons</field>
        </doc>
    </add>
    
## Publications

Further details about Lucene-SKOS have been published in the following papers:

- [Using SKOS Vocabularies for Improving Web Search](http://eprints.cs.univie.ac.at/3689/)
 
        @inproceedings{Haslhofer:2013qf,
            Address = {Rio de Janeiro},
            Author = {Bernhard Haslhofer and Fl{\'a}vio Martins and Jo{\~a}o Magalh{\~a}es},
            Booktitle = {Web of Linked Entities (WoLE)},
            Month = {May},
            Series = {Web of Linked Entities (WoLE) Workshop, co-located with WWW2013},
            Title = {Using SKOS vocabularies for improving Web Search},
            Url = {http://eprints.cs.univie.ac.at/3689/},
            Year = {2013},



## HELP!?

Try to contact us in the #lucene-skos IRC channel, or write an email to the authors.

[skos]: http://www.w3.org/TR/skos-primer/ "SKOS Primer"
[rdf]: http://www.w3.org/TR/rdf-primer/ "RDF Primer"
[ld]: http://www.w3.org/standards/semanticweb/data "Linked Data"
[lucene]: http://lucene.apache.org/core/ "Apache Lucene"
[solr]: http://lucene.apache.org/solr/ "Apache Solr"
[jena]: http://jena.apache.org/ "Apache Jena"
