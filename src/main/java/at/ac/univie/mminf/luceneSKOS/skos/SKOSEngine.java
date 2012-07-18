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

/**
 * An interface to the used SKOS model. It provides accessors to all the data
 * needed for the expansion process.
 */
public interface SKOSEngine {
  
  /**
   * Returns the preferred labels (prefLabel) for a given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws IOException
   */
  String[] getPrefLabels(String conceptURI) throws IOException;
  
  /**
   * Returns the alternative labels (altLabel) for a given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws IOException
   */
  String[] getAltLabels(String conceptURI) throws IOException;
  
  /**
   * Returns the hidden labels (hiddenLabel) for a given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws IOException
   */
  String[] getHiddenLabels(String conceptURI) throws IOException;
  
  /**
   * Returns the related labels (related) for a given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws IOException
   */
  String[] getRelatedLabels(String conceptURI) throws IOException;
  
  /**
   * Returns the URIs of all related concepts for a given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws Exception
   */
  String[] getRelatedConcepts(String conceptURI) throws IOException;
  
  /**
   * Returns the URIs of all broader concepts for a given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws Exception
   */
  String[] getBroaderConcepts(String conceptURI) throws IOException;
  
  /**
   * Returns the URIs of all narrower concepts for a given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws Exception
   */
  String[] getNarrowerConcepts(String conceptURI) throws IOException;
  
  /**
   * Returns the labels (prefLabel + altLabel) of ALL broader concepts for a
   * given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws IOException
   */
  String[] getBroaderLabels(String conceptURI) throws IOException;
  
  /**
   * Returns the labels (prefLabel + altLabel) of ALL narrower concepts for a
   * given URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws IOException
   */
  String[] getNarrowerLabels(String conceptURI) throws IOException;
  
  /**
   * Returns the URIs of all broader transitive concepts for a given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws Exception
   */
  String[] getBroaderTransitiveConcepts(String conceptURI) throws IOException;
  
  /**
   * Returns the URIs of all narrower transitive concepts for a given concept
   * URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws Exception
   */
  String[] getNarrowerTransitiveConcepts(String conceptURI) throws IOException;
  
  /**
   * Returns the labels (prefLabel + altLabel) of ALL broader transitive
   * concepts for a given concept URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws IOException
   */
  String[] getBroaderTransitiveLabels(String conceptURI) throws IOException;
  
  /**
   * Returns the labels (prefLabel + altLabel) of ALL narrower transitive
   * concepts for a given URI
   * 
   * @param conceptURI
   * @return String[]
   * @throws IOException
   */
  String[] getNarrowerTransitiveLabels(String conceptURI) throws IOException;
  
  /**
   * Returns all concepts (URIs) matching a given label
   * 
   * @param label
   * @return String[]
   * @throws IOException
   */
  String[] getConcepts(String label) throws IOException;
  
  /**
   * Returns all alternative terms for a given label
   * 
   * @param label
   * @return String[]
   * @throws IOException
   */
  String[] getAltTerms(String label) throws IOException;
  
}
