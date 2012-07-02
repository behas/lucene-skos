package at.ac.univie.mminf.luceneSKOS.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine;

/**
 * A mock that simulates the behavior of a SKOS engine for testing purposes
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public class SKOSEngineMock implements SKOSEngine {
  
  /**
   * A data structure holding a SKOS Model
   */
  private Map<String,Map<SKOSType,List<String>>> conceptMap = new HashMap<String,Map<SKOSType,List<String>>>();
  
  /**
   * Stores the maximum number of terms contained in a prefLabel
   */
  private int maxPrefLabelTerms = -1;
  
  /**
   * Method for feeding mock with data
   * 
   * @param conceptURI
   * @param type
   * @param value
   */
  public void addEntry(String conceptURI, SKOSType type, String... values) {
    if (!conceptMap.containsKey(conceptURI)) {
      Map<SKOSType,List<String>> entryMap = new HashMap<SKOSType,List<String>>();
      conceptMap.put(conceptURI, entryMap);
    }
    
    Map<SKOSType,List<String>> entryMap = conceptMap.get(conceptURI);
    if (!entryMap.containsKey(type)) {
      List<String> entries = new ArrayList<String>();
      entryMap.put(type, entries);
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
  
  /** Returns the number of (whitespace separated) terms contained in a label */
  private int countLabelTerms(String label) {
    return label.split(" ").length;
  }
  
  @Override
  public String[] getAltLabels(String conceptURI) throws IOException {
    return readConceptFieldValues(conceptURI, SKOSType.ALT);
  }
  
  @Override
  public String[] getAltTerms(String prefLabel) throws IOException {
    List<String> altTerms = new ArrayList<String>();
    
    // convert the query to lower-case
    String queryString = prefLabel.toLowerCase();
    
    String[] conceptURIs = getConcepts(queryString);
    
    if (conceptURIs == null) {
      return null;
    }
    
    for (String conceptURI : conceptURIs) {
      String[] alt = getAltLabels(conceptURI);
      if (alt != null) {
        altTerms.addAll(Arrays.asList(alt));
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
  public String[] getConcepts(String prefLabel) throws IOException {
    String queryString = prefLabel.toLowerCase();
    
    List<String> conceptURIs = new ArrayList<String>();
    
    for (String conceptURI : conceptMap.keySet()) {
      Map<SKOSType,List<String>> entryMap = conceptMap.get(conceptURI);
      
      List<String> prefLabels = entryMap.get(SKOSType.PREF);
      
      if (prefLabels != null && prefLabels.contains(queryString)) {
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
    
    List<String> labels = new ArrayList<String>();
    
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
  
  /** Returns the values of a given field for a given concept */
  private String[] readConceptFieldValues(String conceptURI, SKOSType type)
      throws IOException {
    List<String> labels = conceptMap.get(conceptURI).get(type);
    
    if (labels != null) {
      return labels.toArray(new String[labels.size()]);
    }
    
    return new String[0];
  }
}
