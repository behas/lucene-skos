package at.ac.univie.mminf.luceneSKOS.skos.engine.mock;

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
import at.ac.univie.mminf.luceneSKOS.tokenattributes.SKOSTypeAttribute.SKOSType;

import java.io.IOException;
import java.util.*;

/**
 * A mock that simulates the behavior of a SKOS engine for testing purposes
 */
public class SKOSEngineMock implements SKOSEngine {

  /**
   * A data structure holding a SKOS Model
   */
  private Map<String, Map<SKOSType, List<String>>> conceptMap = new HashMap<>();
  /**
   * Stores the maximum number of terms contained in a prefLabel
   */
  private int maxPrefLabelTerms = -1;

  /**
   * Method for feeding mock with data
   *
   * @param conceptURI the conecpt URI
   * @param type       the type
   * @param values     the values
   */
  public void addEntry(String conceptURI, SKOSType type, String... values) {
    if (!conceptMap.containsKey(conceptURI)) {
      conceptMap.put(conceptURI, new HashMap<SKOSType, List<String>>());
    }
    Map<SKOSType, List<String>> entryMap = conceptMap.get(conceptURI);
    if (!entryMap.containsKey(type)) {
      entryMap.put(type, new ArrayList<String>());
    }
    List<String> entries = entryMap.get(type);
    for (String value : values) {
      entries.add(value.toLowerCase());
      // check for longest prefLabel
      if (type.equals(SKOSType.PREF)) {
        int noTerms = countLabelTerms(value);
        if (maxPrefLabelTerms < noTerms) {
          maxPrefLabelTerms = noTerms;
        }
      }
    }
  }

  /**
   * Returns the number of (whitespace separated) terms contained in a label
   */
  private int countLabelTerms(String label) {
    return label.split(" ").length;
  }

  @Override
  public String[] getAltLabels(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, SKOSType.ALT);
  }

  @Override
  public String[] getAltTerms(String label) throws IOException {
    List<String> altTerms = new ArrayList<>();
    // convert the query to lower-case
    String queryString = label.toLowerCase();
    String[] conceptURIs = getConcepts(queryString);
    if (conceptURIs == null) {
      return null;
    }
    for (String conceptURI : conceptURIs) {
      String[] altLabels = getAltLabels(conceptURI);
      if (altLabels != null) {
        altTerms.addAll(Arrays.asList(altLabels));
      }
    }
    return altTerms.toArray(new String[altTerms.size()]);
  }

  @Override
  public String[] getHiddenLabels(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, SKOSType.HIDDEN);
  }

  @Override
  public String[] getBroaderConcepts(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, SKOSType.BROADER);
  }

  @Override
  public String[] getBroaderLabels(String conceptURI) throws IOException {
    return getLabels(conceptURI, SKOSType.BROADER);
  }

  @Override
  public String[] getBroaderTransitiveConcepts(String conceptURI)
      throws IOException {
    return readConceptFieldValues(conceptURI, SKOSType.BROADERTRANSITIVE);
  }

  @Override
  public String[] getBroaderTransitiveLabels(String conceptURI)
      throws IOException {
    return getLabels(conceptURI, SKOSType.BROADERTRANSITIVE);
  }

  @Override
  public String[] getConcepts(String label) throws IOException {
    String queryString = label.toLowerCase();
    List<String> conceptURIs = new ArrayList<>();
    for (String conceptURI : conceptMap.keySet()) {
      Map<SKOSType, List<String>> entryMap = conceptMap.get(conceptURI);
      List<String> prefLabels = entryMap.get(SKOSType.PREF);
      if (prefLabels != null && prefLabels.contains(queryString)) {
        conceptURIs.add(conceptURI);
      }
      List<String> altLabels = entryMap.get(SKOSType.ALT);
      if (altLabels != null && altLabels.contains(queryString)) {
        conceptURIs.add(conceptURI);
      }
      List<String> hiddenLabels = entryMap.get(SKOSType.HIDDEN);
      if (hiddenLabels != null && hiddenLabels.contains(queryString)) {
        conceptURIs.add(conceptURI);
      }
    }
    return conceptURIs.toArray(new String[conceptURIs.size()]);
  }

  private String[] getLabels(String conceptURI, SKOSType type)
      throws IOException {
    String[] concepts = readConceptFieldValues(conceptURI, type);
    if (concepts == null) {
      return null;
    }
    List<String> labels = new ArrayList<>();
    for (String aConcept : concepts) {
      String[] prefLabels = getPrefLabels(aConcept);
      if (prefLabels != null) {
        labels.addAll(Arrays.asList(prefLabels));
      }
      String[] altLabels = getAltLabels(aConcept);
      if (altLabels != null) {
        labels.addAll(Arrays.asList(altLabels));
      }
    }
    return labels.toArray(new String[labels.size()]);
  }

  @Override
  public String[] getNarrowerConcepts(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, SKOSType.NARROWER);
  }

  @Override
  public String[] getNarrowerLabels(String conceptURI) throws IOException {
    return getLabels(conceptURI, SKOSType.NARROWER);
  }

  @Override
  public String[] getNarrowerTransitiveConcepts(String conceptURI)
      throws IOException {
    return readConceptFieldValues(conceptURI, SKOSType.NARROWERTRANSITIVE);
  }

  @Override
  public String[] getNarrowerTransitiveLabels(String conceptURI)
      throws IOException {
    return getLabels(conceptURI, SKOSType.NARROWERTRANSITIVE);
  }

  @Override
  public String[] getPrefLabels(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, SKOSType.PREF);
  }

  @Override
  public String[] getRelatedConcepts(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, SKOSType.RELATED);
  }

  @Override
  public String[] getRelatedLabels(String conceptURI) throws IOException {
    return getLabels(conceptURI, SKOSType.RELATED);
  }

  /**
   * Returns the values of a given field for a given concept
   */
  private String[] readConceptFieldValues(String conceptURI, SKOSType type)
      throws IOException {
    List<String> labels = conceptMap.get(conceptURI).get(type);
    if (labels != null) {
      return labels.toArray(new String[labels.size()]);
    }
    return new String[0];
  }
}
