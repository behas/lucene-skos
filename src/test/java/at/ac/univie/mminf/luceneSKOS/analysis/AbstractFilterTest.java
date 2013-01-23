package at.ac.univie.mminf.luceneSKOS.analysis;

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

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.junit.After;
import org.junit.Before;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.mock.SKOSEngineMock;

/**
 * Abstract class containing common code for filter tests
 */
public abstract class AbstractFilterTest {
  
  protected final Version matchVersion = Version.LUCENE_41;
  
  protected IndexSearcher searcher;
  
  protected IndexWriter writer;
  
  protected SKOSEngineMock skosEngine;
  
  protected SKOSAnalyzer skosAnalyzer;
  
  protected Directory directory;
  
  @Before
  protected void setUp() throws Exception {
    
    // adding some test data
    skosEngine = new SKOSEngineMock();
    
    skosEngine.addEntry("http://example.com/concept/1", SKOSType.PREF, "jumps");
    skosEngine.addEntry("http://example.com/concept/1", SKOSType.ALT, "leaps",
        "hops");
    
    skosEngine.addEntry("http://example.com/concept/2", SKOSType.PREF, "quick");
    skosEngine.addEntry("http://example.com/concept/2", SKOSType.ALT, "fast",
        "speedy");
    
    skosEngine.addEntry("http://example.com/concept/3", SKOSType.PREF, "over");
    skosEngine.addEntry("http://example.com/concept/3", SKOSType.ALT, "above");
    
    skosEngine.addEntry("http://example.com/concept/4", SKOSType.PREF, "lazy");
    skosEngine.addEntry("http://example.com/concept/4", SKOSType.ALT,
        "apathic", "sluggish");
    
    skosEngine.addEntry("http://example.com/concept/5", SKOSType.PREF, "dog");
    skosEngine.addEntry("http://example.com/concept/5", SKOSType.ALT, "canine",
        "pooch");
    
    skosEngine.addEntry("http://example.com/concept/6", SKOSType.PREF,
        "united nations");
    skosEngine.addEntry("http://example.com/concept/6", SKOSType.ALT, "UN");
    
    skosEngine.addEntry("http://example.com/concept/7", SKOSType.PREF,
        "lazy dog");
    skosEngine.addEntry("http://example.com/concept/7", SKOSType.ALT, "Odie");
    
    directory = new RAMDirectory();
    
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
  
}
