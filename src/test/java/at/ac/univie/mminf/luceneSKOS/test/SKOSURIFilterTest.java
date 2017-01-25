package at.ac.univie.mminf.luceneSKOS.test;

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

import java.io.IOException;

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

import org.junit.Before;
import org.junit.Test;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer.ExpansionType;

import static org.junit.Assert.assertEquals;

/**
 * Testing the SKOS URI Filter
 */
public class SKOSURIFilterTest extends AbstractFilterTest {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        skosAnalyzer = new SKOSAnalyzer(skosEngine, ExpansionType.URI);
        writer = new IndexWriter(directory, new IndexWriterConfig(skosAnalyzer));
    }

    @Test
    public void singleUriExpansionWithStoredField() throws IOException {
        Document doc = new Document();
        doc.add(new Field("subject", "http://example.com/concept/1", TextField.TYPE_STORED));
        writer.addDocument(doc);
        searcher = new IndexSearcher(DirectoryReader.open(writer, false, false));
        Query query = new TermQuery(new Term("subject", "leaps"));
        TopDocs results = searcher.search(query, 10);
        assertEquals(1, results.totalHits);

        Document indexDoc = searcher.doc(results.scoreDocs[0].doc);
        String[] fieldValues = indexDoc.getValues("subject");
        assertEquals(1, fieldValues.length);
        assertEquals(fieldValues[0], "http://example.com/concept/1");
    }

    @Test
    public void singleUriExpansionWithUnstoredField() throws IOException {
        Document doc = new Document();
        doc.add(new Field("subject", "http://example.com/concept/1", TextField.TYPE_NOT_STORED));
        writer.addDocument(doc);
        searcher = new IndexSearcher(DirectoryReader.open(writer, false, false));
        Query query = new TermQuery(new Term("subject", "jumps"));
        TopDocs results = searcher.search(query, 10);
        assertEquals(1, results.totalHits);

        Document indexDoc = searcher.doc(results.scoreDocs[0].doc);
        String[] fieldValues = indexDoc.getValues("subject");
        assertEquals(0, fieldValues.length);
    }

    @Test
    public void multipleURIExpansion() throws IOException {
        Document doc = new Document();
        doc.add(new Field("subject", "http://example.com/concept/1", TextField.TYPE_STORED));
        doc.add(new Field("subject", "http://example.com/concept/2", TextField.TYPE_STORED));
        writer.addDocument(doc);
        searcher = new IndexSearcher(DirectoryReader.open(writer, false, false));

        // querying for alternative term of concept 1
        Query query = new TermQuery(new Term("subject", "hops"));
        TopDocs results = searcher.search(query, 10);
        assertEquals(1, results.totalHits);
        Document indexDoc = searcher.doc(results.scoreDocs[0].doc);
        String[] fieldValues = indexDoc.getValues("subject");
        assertEquals(2, fieldValues.length);

        // querying for alternative term of concept 2
        query = new TermQuery(new Term("subject", "speedy"));
        results = searcher.search(query, 10);
        assertEquals(1, results.totalHits);

        indexDoc = searcher.doc(results.scoreDocs[0].doc);
        fieldValues = indexDoc.getValues("subject");
        assertEquals(2, fieldValues.length);
    }
}
