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

import at.ac.univie.mminf.luceneSKOS.skos.engine.SKOSEngine;
import at.ac.univie.mminf.luceneSKOS.skos.engine.SKOSEngineFactory;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Tests the functionality of the Lucene-backed SKOS Engine implementation
 */
public class SKOSEngineTest extends Assert {

  @Test
  public void testSimpleSKOSSamplesRDFXML() throws IOException {
    InputStream skosFile = getClass().getResourceAsStream("/skos_samples/simple_test_skos.rdf");
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile, "RDF/XML");
    assertEquals(2, skosEngine.getAltTerms("quick").length);
    assertEquals(1, skosEngine.getAltTerms("over").length);
  }

  @Test
  public void testSimpleSKOSSamplesN3() throws IOException {
    InputStream skosFile = getClass().getResourceAsStream("/skos_samples/simple_test_skos.rdf");
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile, "RDF/XML");
    assertEquals(2, skosEngine.getAltTerms("quick").length);
    assertEquals(1, skosEngine.getAltTerms("over").length);
  }

  /**
   * Tests retrieval of non-explicitly types SKOS concepts
   */
  @Test
  public void testSimpleSKOSSampleN3NoType() throws IOException {
    InputStream skosFile = getClass().getResourceAsStream("/skos_samples/simple_test_skos.n3");
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile, "N3");
    assertEquals(2, skosEngine.getAltTerms("sheep").length);
    assertEquals(2, skosEngine.getAltTerms("kity").length);
  }

  @Test
  public void testSKOSSpecSamples() throws IOException {
    InputStream skosFile = getClass().getResourceAsStream("/skos_samples/skos_spec_samples.n3");
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile, "N3");
    assertEquals(skosEngine.getAltTerms("animals").length, 3);
    assertEquals(skosEngine.getAltTerms("Food and Agriculture Organization").length, 1);
  }

  @Test
  public void testSKOSSpecSamplesWithLanguageRestriction() throws IOException {
    InputStream skosFile = getClass().getResourceAsStream("/skos_samples/skos_spec_samples.n3");
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile, "N3", "en");
    String[] altTerms = skosEngine.getAltTerms("animals");
    assertEquals(1, altTerms.length);
    assertEquals("creatures", altTerms[0]);
  }

  @Test
  public void testUKATSamples() throws IOException {
    InputStream skosFile = getClass().getResourceAsStream("/skos_samples/ukat_examples.n3");
    String conceptURI = "http://www.ukat.org.uk/thesaurus/concept/859";
    SKOSEngine skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile, "N3");
    // testing pref-labels
    String[] prefLabel = skosEngine.getPrefLabels(conceptURI);
    assertEquals(1, prefLabel.length);
    assertEquals("weapons", prefLabel[0]);
    // testing alt-labels
    String[] altLabel = skosEngine.getAltLabels(conceptURI);
    assertEquals(2, altLabel.length);
    assertTrue(Arrays.asList(altLabel).contains("armaments"));
    assertTrue(Arrays.asList(altLabel).contains("arms"));
    // testing broader
    String[] broader = skosEngine.getBroaderConcepts(conceptURI);
    assertEquals(1, broader.length);
    assertEquals("http://www.ukat.org.uk/thesaurus/concept/5060", broader[0]);
    // testing narrower
    String[] narrower = skosEngine.getNarrowerConcepts(conceptURI);
    assertEquals(2, narrower.length);
    assertTrue(Arrays.asList(narrower).contains(
        "http://www.ukat.org.uk/thesaurus/concept/18874"));
    assertTrue(Arrays.asList(narrower).contains(
        "http://www.ukat.org.uk/thesaurus/concept/7630"));
    // testing broader labels
    String[] broaderLabels = skosEngine.getBroaderLabels(conceptURI);
    assertEquals(3, broaderLabels.length);
    assertTrue(Arrays.asList(broaderLabels).contains(
        "military equipment"));
    assertTrue(Arrays.asList(broaderLabels).contains(
        "defense equipment and supplies"));
    assertTrue(Arrays.asList(broaderLabels).contains("ordnance"));
    // testing narrower labels
    String[] narrowerLabels = skosEngine.getNarrowerLabels(conceptURI);
    assertEquals(2, narrowerLabels.length);
    assertTrue(Arrays.asList(narrowerLabels).contains("ammunition"));
    assertTrue(Arrays.asList(narrowerLabels).contains("artillery"));
  }
}
