package at.ac.univie.mminf.luceneSKOS.queryparser.flexible.standard;

/**
 * Copyright 2012 Flavio Martins
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

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer.ExpansionType;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.mock.SKOSEngineMock;
import at.ac.univie.mminf.luceneSKOS.util.TestUtil;

public class SKOSStandardQueryParserTest {

    protected IndexSearcher searcher;

    protected IndexWriter writer;

    protected SKOSEngineMock skosEngine;

    protected SKOSAnalyzer skosAnalyzer;

    protected Directory directory;

    @Before
    public void setUp() throws Exception {

        // adding some test data
        skosEngine = new SKOSEngineMock();

        skosEngine.addEntry("http://example.com/concept/1", SKOSType.PREF, "jumps");
        skosEngine.addEntry("http://example.com/concept/1", SKOSType.ALT, "leaps", "hops");

        skosEngine.addEntry("http://example.com/concept/2", SKOSType.PREF, "quick");
        skosEngine.addEntry("http://example.com/concept/2", SKOSType.ALT, "fast", "speedy");

        skosEngine.addEntry("http://example.com/concept/3", SKOSType.PREF, "over");
        skosEngine.addEntry("http://example.com/concept/3", SKOSType.ALT, "above");

        skosEngine.addEntry("http://example.com/concept/4", SKOSType.PREF, "lazy");
        skosEngine.addEntry("http://example.com/concept/4", SKOSType.ALT, "apathic", "sluggish");

        skosEngine.addEntry("http://example.com/concept/5", SKOSType.PREF, "dog");
        skosEngine.addEntry("http://example.com/concept/5", SKOSType.ALT, "canine", "pooch");

        skosEngine.addEntry("http://example.com/concept/6", SKOSType.PREF, "united nations");
        skosEngine.addEntry("http://example.com/concept/6", SKOSType.ALT, "UN");

        skosEngine.addEntry("http://example.com/concept/7", SKOSType.PREF, "lazy dog");
        skosEngine.addEntry("http://example.com/concept/7", SKOSType.ALT, "Odie");

        directory = new RAMDirectory();

        skosAnalyzer = new SKOSAnalyzer(skosEngine, ExpansionType.LABEL);

        writer = new IndexWriter(directory, new IndexWriterConfig(skosAnalyzer));

    }

    @After
    public void tearDown() throws Exception {

        if (writer != null) {
            writer.close();
        }

        if (searcher != null) {
            searcher.getIndexReader().close();
        }

    }

    @Test
    public void queryParserSearch() throws IOException, QueryNodeException {

        Document doc = new Document();
        doc.add(new Field("content", "The quick brown fox jumps over the lazy dog", TextField.TYPE_STORED));

        writer.addDocument(doc);

        searcher = new IndexSearcher(DirectoryReader.open(writer, false));

        Query query = new SKOSStandardQueryParser(skosAnalyzer).parse("\"fox jumps\"", "content");

        Assert.assertEquals(1, TestUtil.hitCount(searcher, query));

        Assert.assertEquals("content:\"fox (jumps hops leaps)\"", query.toString());
        Assert.assertEquals("org.apache.lucene.search.MultiPhraseQuery", query.getClass().getName());

        query = new StandardQueryParser(new StandardAnalyzer()).parse("\"fox jumps\"", "content");
        Assert.assertEquals(1, TestUtil.hitCount(searcher, query));

        Assert.assertEquals("content:\"fox jumps\"", query.toString());
        Assert.assertEquals("org.apache.lucene.search.PhraseQuery", query.getClass().getName());

    }

    @Test
    public void queryParserSearchWithBoosts() throws IOException, QueryNodeException {

        Document doc = new Document();
        doc.add(new Field("content", "The quick brown fox jumps over the lazy dog", TextField.TYPE_STORED));

        writer.addDocument(doc);

        searcher = new IndexSearcher(DirectoryReader.open(writer, false));

        SKOSStandardQueryParser parser = new SKOSStandardQueryParser(skosAnalyzer);
        parser.setBoost(SKOSType.ALT, 0.5f);

        Query query = parser.parse("\"fox jumps\"", "content");

        Assert.assertEquals(1, TestUtil.hitCount(searcher, query));

        // boosts do not work in phrase queries
        Assert.assertEquals("content:\"fox (jumps hops leaps)\"", query.toString());
        Assert.assertEquals("org.apache.lucene.search.MultiPhraseQuery", query.getClass().getName());

        query = parser.parse("fox jumps", "content");

        Assert.assertEquals(1, TestUtil.hitCount(searcher, query));

        Assert.assertEquals("content:fox (content:jumps content:hops^0.5 content:leaps^0.5)", query.toString());
        Assert.assertEquals("org.apache.lucene.search.BooleanQuery", query.getClass().getName());

        query = new SKOSStandardQueryParser(new StandardAnalyzer()).parse("fox jumps", "content");
        Assert.assertEquals(1, TestUtil.hitCount(searcher, query));

        Assert.assertEquals("content:fox content:jumps", query.toString());
        Assert.assertEquals("org.apache.lucene.search.BooleanQuery", query.getClass().getName());

    }

}
