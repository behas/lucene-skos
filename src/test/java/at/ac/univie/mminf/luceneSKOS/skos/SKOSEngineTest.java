package at.ac.univie.mminf.luceneSKOS.skos;

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
import java.util.Arrays;

import junit.framework.Assert;

import org.apache.lucene.util.Version;
import org.junit.Test;

/**
 * Tests the functionality of the Lucene-backed SKOS Engine implementation
 */
public class SKOSEngineTest {
  
  protected final Version matchVersion = Version.LUCENE_40;
  
  @Test
  public void testSimpleSKOSSamplesRDFXML() throws IOException {
    
    String skosFile = "src/test/resources/skos_samples/simple_test_skos.rdf";
    
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(matchVersion,
        skosFile);
    
    Assert.assertEquals(2, skosEngine.getAltTerms("quick").length);
    
    Assert.assertEquals(1, skosEngine.getAltTerms("over").length);
    
  }
  
  @Test
  public void testSimpleSKOSSamplesN3() throws IOException {
    
    String skosFile = "src/test/resources/skos_samples/simple_test_skos.rdf";
    
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(matchVersion,
        skosFile);
    
    Assert.assertEquals(2, skosEngine.getAltTerms("quick").length);
    
    Assert.assertEquals(1, skosEngine.getAltTerms("over").length);
    
  }
  
  /**
   * Tests retrieval of non-explicitly types SKOS concepts
   */
  @Test
  public void testSimpleSKOSSampleN3NoType() throws IOException {
      
      String skosFile = "src/test/resources/skos_samples/simple_test_skos.n3";

      SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(matchVersion,
          skosFile);

      Assert.assertEquals(2, skosEngine.getAltTerms("sheep").length);

      Assert.assertEquals(2, skosEngine.getAltTerms("kity").length);
  }
  
  
  @Test
  public void testSKOSSpecSamples() throws IOException {
    
    String skosFile = "src/test/resources/skos_samples/skos_spec_samples.n3";
    
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(matchVersion,
        skosFile);
    
    Assert.assertEquals(3, skosEngine.getAltTerms("animals").length);
    
    Assert.assertEquals(1,
        skosEngine.getAltTerms("Food and Agriculture Organization").length);
    
  }
  
  @Test
  public void testSKOSSpecSamplesWithLanguageRestriction() throws IOException {
    
    String skosFile = "src/test/resources/skos_samples/skos_spec_samples.n3";
    
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(matchVersion,
        skosFile, "en");
    
    String[] altTerms = skosEngine.getAltTerms("animals");
    
    Assert.assertEquals(1, altTerms.length);
    
    Assert.assertEquals("creatures", altTerms[0]);
    
  }
  
  @Test
  public void testUKATSamples() throws IOException {
    
    String skosFile = "src/test/resources/skos_samples/ukat_examples.n3";
    
    String conceptURI = "http://www.ukat.org.uk/thesaurus/concept/859";
    
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(matchVersion,
        skosFile);
    
    // testing pref-labels
    String[] prefLabel = skosEngine.getPrefLabels(conceptURI);
    
    Assert.assertEquals(1, prefLabel.length);
    
    Assert.assertEquals("weapons", prefLabel[0]);
    
    // testing alt-labels
    String[] altLabel = skosEngine.getAltLabels(conceptURI);
    
    Assert.assertEquals(2, altLabel.length);
    
    Assert.assertTrue(Arrays.asList(altLabel).contains("armaments"));
    
    Assert.assertTrue(Arrays.asList(altLabel).contains("arms"));
    
    // testing broader
    String[] broader = skosEngine.getBroaderConcepts(conceptURI);
    
    Assert.assertEquals(1, broader.length);
    
    Assert.assertEquals("http://www.ukat.org.uk/thesaurus/concept/5060",
        broader[0]);
    
    // testing narrower
    String[] narrower = skosEngine.getNarrowerConcepts(conceptURI);
    
    Assert.assertEquals(2, narrower.length);
    
    Assert.assertTrue(Arrays.asList(narrower).contains(
        "http://www.ukat.org.uk/thesaurus/concept/18874"));
    
    Assert.assertTrue(Arrays.asList(narrower).contains(
        "http://www.ukat.org.uk/thesaurus/concept/7630"));
    
    // testing broader labels
    String[] broaderLabels = skosEngine.getBroaderLabels(conceptURI);
    
    Assert.assertEquals(3, broaderLabels.length);
    
    Assert.assertTrue(Arrays.asList(broaderLabels).contains(
        "military equipment"));
    
    Assert.assertTrue(Arrays.asList(broaderLabels).contains(
        "defense equipment and supplies"));
    
    Assert.assertTrue(Arrays.asList(broaderLabels).contains("ordnance"));
    
    // testing narrower labels
    String[] narrowerLabels = skosEngine.getNarrowerLabels(conceptURI);
    
    Assert.assertEquals(2, narrowerLabels.length);
    
    Assert.assertTrue(Arrays.asList(narrowerLabels).contains("ammunition"));
    
    Assert.assertTrue(Arrays.asList(narrowerLabels).contains("artillery"));
    
  }
  
}
