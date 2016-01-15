package at.ac.univie.mminf.luceneSKOS.test.termexpansion;

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

import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer.ExpansionType;
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
import org.apache.lucene.search.*;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * This test-case verifies and demonstrates the "Label-based term expansion" use
 * case as described in https://code.
 * google.com/p/lucene-skos/wiki/UseCases#UC2:_Label-based_term_expansion
 */
public class LabelbasedTermExpansionTest extends AbstractTermExpansionTest {

  /**
   * This test indexes a sample metadata record (=lucene document) having a
   * "title", "description", and "subject" field.
   * <p/>
   * A search for "arms" returns that record as a result because "arms" is
   * defined as an alternative label for "weapons", the term which is
   * contained in the subject field.
   *
   * @throws IOException
   */
  @Test
  public void labelBasedTermExpansion() throws IOException {

        /* defining the document to be indexed */
    Document doc = new Document();
    doc.add(new Field("title", "Spearhead",
            TextField.TYPE_STORED));
    doc.add(new Field(
            "description",
            "Roman iron spearhead. The spearhead was attached to one end of a wooden shaft..."
                    + "The spear was mainly a thrusting weapon, but could also be thrown. "
                    + "It was the principal weapon of the auxiliary soldier... "
                    + "(second - fourth century, Arbeia Roman Fort).",
            TextField.TYPE_NOT_STORED));
    doc.add(new Field("subject", "weapons",
            TextField.TYPE_NOT_STORED));

        /* setting up the SKOS analyzer */
    String skosFile = "src/test/resources/skos_samples/ukat_examples.n3";
    String indexPath = "build/";

        /* ExpansionType.URI->the field to be analyzed (expanded) contains URIs */
    Analyzer skosAnalyzer = new SKOSAnalyzer(indexPath, skosFile,
            ExpansionType.LABEL);

        /* Define different analyzers for different fields */
    Map<String, Analyzer> analyzerPerField = new HashMap<>();
    analyzerPerField.put("subject", skosAnalyzer);
    PerFieldAnalyzerWrapper indexAnalyzer = new PerFieldAnalyzerWrapper(new SimpleAnalyzer(), analyzerPerField);

        /* setting up a writer with a default (simple) analyzer */
    writer = new IndexWriter(new RAMDirectory(), new IndexWriterConfig(indexAnalyzer));

        /* adding the document to the index */
    writer.addDocument(doc);

        /* defining a query that searches over all fields */
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    builder.add(new TermQuery(new Term("title", "arms")), BooleanClause.Occur.SHOULD)
            .add(new TermQuery(new Term("description", "arms")), BooleanClause.Occur.SHOULD)
            .add(new TermQuery(new Term("subject", "arms")), BooleanClause.Occur.SHOULD);

        /* creating a new searcher */
    searcher = new IndexSearcher(DirectoryReader.open(writer, false));

    TopDocs results = searcher.search(builder.build(), 10);

        /* the document matches because "arms" is among the expanded terms */
    assertEquals(1, results.totalHits);

        /* defining a query that searches for a broader concept */
    Query query = new TermQuery(new Term("subject", "military equipment"));

    results = searcher.search(query, 10);

        /* ... also returns the document as result */
    assertEquals(1, results.totalHits);
  }
}
