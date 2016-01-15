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

import at.ac.univie.mminf.luceneSKOS.analysis.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.analysis.engine.SKOSEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
     * @param type the type
     * @param values the values
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
            entries.add(value.toLowerCase(Locale.ROOT));
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
    public List<String> getAltLabels(String conceptURI) {
        return readConceptFieldValues(conceptURI, SKOSType.ALT);
    }

    @Override
    public List<String> getAltTerms(String label) {
        List<String> altTerms = new ArrayList<>();
        // convert the query to lower-case
        String queryString = label.toLowerCase(Locale.ROOT);
        List<String> conceptURIs = getConcepts(queryString);
        if (conceptURIs == null) {
            return null;
        }
        for (String conceptURI : conceptURIs) {
            List<String> altLabels = getAltLabels(conceptURI);
            if (altLabels != null) {
                altTerms.addAll(altLabels);
            }
        }
        return altTerms;
    }

    @Override
    public List<String> getHiddenLabels(String conceptURI) {
        return readConceptFieldValues(conceptURI, SKOSType.HIDDEN);
    }

    @Override
    public List<String> getBroaderConcepts(String conceptURI) {
        return readConceptFieldValues(conceptURI, SKOSType.BROADER);
    }

    @Override
    public List<String> getBroaderLabels(String conceptURI) {
        return getLabels(conceptURI, SKOSType.BROADER);
    }

    @Override
    public List<String> getBroaderTransitiveConcepts(String conceptURI) {
        return readConceptFieldValues(conceptURI, SKOSType.BROADERTRANSITIVE);
    }

    @Override
    public List<String> getBroaderTransitiveLabels(String conceptURI) {
        return getLabels(conceptURI, SKOSType.BROADERTRANSITIVE);
    }

    @Override
    public List<String> getConcepts(String label) {
        String queryString = label.toLowerCase(Locale.ROOT);
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
        return conceptURIs;
    }

    private List<String> getLabels(String conceptURI, SKOSType type) {
        List<String> concepts = readConceptFieldValues(conceptURI, type);
        if (concepts == null || concepts.isEmpty()) {
            return null;
        }
        List<String> labels = new ArrayList<>();
        for (String aConcept : concepts) {
            List<String> prefLabels = getPrefLabels(aConcept);
            if (prefLabels != null) {
                labels.addAll(prefLabels);
            }
            List<String> altLabels = getAltLabels(aConcept);
            if (altLabels != null) {
                labels.addAll(altLabels);
            }
        }
        return labels;
    }

    @Override
    public List<String> getNarrowerConcepts(String conceptURI) {
        return readConceptFieldValues(conceptURI, SKOSType.NARROWER);
    }

    @Override
    public List<String> getNarrowerLabels(String conceptURI) {
        return getLabels(conceptURI, SKOSType.NARROWER);
    }

    @Override
    public List<String> getNarrowerTransitiveConcepts(String conceptURI) {
        return readConceptFieldValues(conceptURI, SKOSType.NARROWERTRANSITIVE);
    }

    @Override
    public List<String> getNarrowerTransitiveLabels(String conceptURI) {
        return getLabels(conceptURI, SKOSType.NARROWERTRANSITIVE);
    }

    @Override
    public List<String> getPrefLabels(String conceptURI) {
        return readConceptFieldValues(conceptURI, SKOSType.PREF);
    }

    @Override
    public List<String> getRelatedConcepts(String conceptURI) {
        return readConceptFieldValues(conceptURI, SKOSType.RELATED);
    }

    @Override
    public List<String> getRelatedLabels(String conceptURI) {
        return getLabels(conceptURI, SKOSType.RELATED);
    }

    /**
     * Returns the values of a given field for a given concept
     */
    private List<String> readConceptFieldValues(String conceptURI, SKOSType type) {
        List<String> labels = conceptMap.get(conceptURI).get(type);
        if (labels != null) {
            return labels;
        }
        return Collections.emptyList();
    }
}
