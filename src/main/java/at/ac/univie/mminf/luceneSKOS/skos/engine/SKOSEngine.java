package at.ac.univie.mminf.luceneSKOS.skos.engine;

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

/**
 * An interface to the used SKOS model. It provides accessors to all the data
 * needed for the expansion process.
 */
public interface SKOSEngine {

  /**
   * Returns the preferred labels (prefLabel) for a given concept URI
   *
   * @param conceptURI the concept URI
   * @return String[] the preferrrd label
   * @throws IOException if method fails
   */
  String[] getPrefLabels(String conceptURI) throws IOException;

  /**
   * Returns the alternative labels (altLabel) for a given concept URI
   *
   * @param conceptURI the concept URI
   * @return String[] the alternative labels
   * @throws IOException if method fails
   */
  String[] getAltLabels(String conceptURI) throws IOException;

  /**
   * Returns the hidden labels (hiddenLabel) for a given concept URI
   *
   * @param conceptURI the concept URI
   * @return String[] the hidden labels
   * @throws IOException if method fails
   */
  String[] getHiddenLabels(String conceptURI) throws IOException;

  /**
   * Returns the related labels (related) for a given concept URI
   *
   * @param conceptURI the concept URI
   * @return String[] the related labels
   * @throws IOException if method fails
   */
  String[] getRelatedLabels(String conceptURI) throws IOException;

  /**
   * Returns the URIs of all related concepts for a given concept URI
   *
   * @param conceptURI the concept URI
   * @return String[] the related concepts
   * @throws IOException if method fails
   */
  String[] getRelatedConcepts(String conceptURI) throws IOException;

  /**
   * Returns the URIs of all broader concepts for a given concept URI
   *
   * @param conceptURI the concept URI
   * @return String[] the broader concepts
   * @throws IOException if method fails
   */
  String[] getBroaderConcepts(String conceptURI) throws IOException;

  /**
   * Returns the URIs of all narrower concepts for a given concept URI
   *
   * @param conceptURI the concept URI
   * @return String[] the narrower concepts
   * @throws IOException if method fails
   */
  String[] getNarrowerConcepts(String conceptURI) throws IOException;

  /**
   * Returns the labels (prefLabel + altLabel) of ALL broader concepts for a
   * given concept URI
   *
   * @param conceptURI the concept URI
   * @return String[] the broader labels
   * @throws IOException if method fails
   */
  String[] getBroaderLabels(String conceptURI) throws IOException;

  /**
   * Returns the labels (prefLabel + altLabel) of ALL narrower concepts for a
   * given URI
   *
   * @param conceptURI the concept URI
   * @return String[] the narrower labels
   * @throws IOException if method fails
   */
  String[] getNarrowerLabels(String conceptURI) throws IOException;

  /**
   * Returns the URIs of all broader transitive concepts for a given concept
   * URI
   *
   * @param conceptURI the concept URI
   * @return String[] the broader transitive concepts
   * @throws IOException if method fails
   */
  String[] getBroaderTransitiveConcepts(String conceptURI) throws IOException;

  /**
   * Returns the URIs of all narrower transitive concepts for a given concept
   * URI
   *
   * @param conceptURI the concept URI
   * @return String[] the nattower transitive concepts
   * @throws IOException if method fails
   */
  String[] getNarrowerTransitiveConcepts(String conceptURI) throws IOException;

  /**
   * Returns the labels (prefLabel + altLabel) of ALL broader transitive
   * concepts for a given concept URI
   *
   * @param conceptURI the concept URI
   * @return String[] the broader transitive concepts
   * @throws IOException if method fails
   */
  String[] getBroaderTransitiveLabels(String conceptURI) throws IOException;

  /**
   * Returns the labels (prefLabel + altLabel) of ALL narrower transitive
   * concepts for a given URI
   *
   * @param conceptURI the concept URI
   * @return String[] the narrower trasitive concepts
   * @throws IOException if method fails
   */
  String[] getNarrowerTransitiveLabels(String conceptURI) throws IOException;

  /**
   * Returns all concepts (URIs) matching a given label
   *
   * @param label the label
   * @return String[] the concepts
   * @throws IOException if method fails
   */
  String[] getConcepts(String label) throws IOException;

  /**
   * Returns all alternative terms for a given label
   *
   * @param label the label
   * @return String[] the alternative terms
   * @throws IOException if method fails
   */
  String[] getAltTerms(String label) throws IOException;
}
