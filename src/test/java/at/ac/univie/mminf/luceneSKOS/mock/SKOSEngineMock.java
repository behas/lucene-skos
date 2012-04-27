package at.ac.univie.mminf.luceneSKOS.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttribute.SKOSType;
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
   * Default Constructor
   */
  public SKOSEngineMock() {
    
  }
  
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
  
  @Override
  public String[] getConcepts(String prefLabel) throws IOException {
    
    prefLabel = prefLabel.toLowerCase();
    
    List<String> conceptURIs = new ArrayList<String>();
    
    for (String conceptURI : conceptMap.keySet()) {
      
      Map<SKOSType,List<String>> entryMap = conceptMap.get(conceptURI);
      
      List<String> prefLabels = entryMap.get(SKOSType.PREF);
      
      if (prefLabels != null) {
        if (prefLabels.contains(prefLabel)) {
          conceptURIs.add(conceptURI);
        }
      }
      
    }
    
    return conceptURIs.toArray(new String[0]);
  }
  
  @Override
  public String[] getAltTerms(String prefLabel) throws IOException {
    
    prefLabel = prefLabel.toLowerCase();
    
    List<String> altTerms = new ArrayList<String>();
    
    String[] conceptURIs = getConcepts(prefLabel);
    
    if (conceptURIs == null) {
      return null;
    }
    
    for (String conceptURI : conceptURIs) {
      
      String[] alt = getAltLabels(conceptURI);
      if (alt != null) {
        altTerms.addAll(Arrays.asList(alt));
      }
      
    }
    
    return altTerms.toArray(new String[0]);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getPrefLabel(java.lang.
   * String)
   */
  @Override
  public String[] getPrefLabels(String conceptURI) throws IOException {
    
    List<String> prefLabels = conceptMap.get(conceptURI).get(SKOSType.PREF);
    
    if (prefLabels != null) {
      return prefLabels.toArray(new String[0]);
    }
    
    return null;
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getAltLabels(java.lang.
   * String)
   */
  @Override
  public String[] getAltLabels(String conceptURI) throws IOException {
    
    List<String> altLabels = conceptMap.get(conceptURI).get(SKOSType.ALT);
    
    if (altLabels != null) {
      return altLabels.toArray(new String[0]);
    }
    
    return null;
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getRelatedConcepts(java
   * .lang.String)
   */
  @Override
  public String[] getRelatedConcepts(String conceptURI) throws IOException {
    
    List<String> related = conceptMap.get(conceptURI).get(SKOSType.RELATED);
    
    if (related != null) return related.toArray(new String[0]);
    
    return null;
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getBroaderConcepts(java
   * .lang.String)
   */
  @Override
  public String[] getBroaderConcepts(String conceptURI) throws IOException {
    
    List<String> broader = conceptMap.get(conceptURI).get(SKOSType.BROADER);
    
    if (broader != null) {
      return broader.toArray(new String[0]);
    }
    
    return null;
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getNarrowerConcepts(java
   * .lang.String)
   */
  @Override
  public String[] getNarrowerConcepts(String conceptURI) throws IOException {
    
    List<String> narrower = conceptMap.get(conceptURI).get(SKOSType.NARROWER);
    
    if (narrower != null) {
      return narrower.toArray(new String[0]);
    }
    
    return null;
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getBroaderTransitiveConcepts
   * (java .lang.String)
   */
  @Override
  public String[] getBroaderTransitiveConcepts(String conceptURI)
      throws IOException {
    
    List<String> broader = conceptMap.get(conceptURI).get(
        SKOSType.BROADER_TRANSITIVE);
    
    if (broader != null) return broader.toArray(new String[0]);
    
    return null;
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getNarrowerTransitiveConcepts
   * (java .lang.String)
   */
  @Override
  public String[] getNarrowerTransitiveConcepts(String conceptURI)
      throws IOException {
    
    List<String> narrower = conceptMap.get(conceptURI).get(
        SKOSType.NARROWER_TRANSITIVE);
    
    if (narrower != null) return narrower.toArray(new String[0]);
    
    return null;
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getRelatedLabels(java.lang
   * .String)
   */
  @Override
  public String[] getRelatedLabels(String conceptURI) throws IOException {
    
    String[] relatedConcepts = getRelatedConcepts(conceptURI);
    
    if (relatedConcepts == null) return null;
    
    List<String> relatedLabels = new ArrayList<String>();
    
    for (String relatedConcept : relatedConcepts) {
      
      String[] prefLabels = getPrefLabels(relatedConcept);
      if (prefLabels != null) relatedLabels.addAll(Arrays.asList(prefLabels));
      String[] altLabels = getAltLabels(relatedConcept);
      if (altLabels != null) relatedLabels.addAll(Arrays.asList(altLabels));
      
    }
    
    return relatedLabels.toArray(new String[0]);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getBroaderLabels(java.lang
   * .String)
   */
  @Override
  public String[] getBroaderLabels(String conceptURI) throws IOException {
    
    String[] broaderConcepts = getBroaderConcepts(conceptURI);
    
    if (broaderConcepts == null) {
      return null;
    }
    
    List<String> broaderLabels = new ArrayList<String>();
    
    for (String broaderConcept : broaderConcepts) {
      
      String[] prefLabels = getPrefLabels(broaderConcept);
      if (prefLabels != null) {
        broaderLabels.addAll(Arrays.asList(prefLabels));
      }
      String[] altLabels = getAltLabels(broaderConcept);
      if (altLabels != null) {
        broaderLabels.addAll(Arrays.asList(altLabels));
      }
      
    }
    
    return broaderLabels.toArray(new String[0]);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getNarrowerLabels(java.
   * lang.String)
   */
  @Override
  public String[] getNarrowerLabels(String conceptURI) throws IOException {
    
    String[] narrowerConcepts = getNarrowerConcepts(conceptURI);
    
    if (narrowerConcepts == null) {
      return null;
    }
    
    List<String> narrowerLabels = new ArrayList<String>();
    
    for (String narrowerConcept : narrowerConcepts) {
      
      String[] prefLabels = getPrefLabels(narrowerConcept);
      if (prefLabels != null) {
        narrowerLabels.addAll(Arrays.asList(prefLabels));
      }
      String[] altLabels = getAltLabels(narrowerConcept);
      if (altLabels != null) {
        narrowerLabels.addAll(Arrays.asList(altLabels));
      }
      
    }
    
    return narrowerLabels.toArray(new String[0]);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getBroaderTransitiveLabels
   * (java.lang .String)
   */
  @Override
  public String[] getBroaderTransitiveLabels(String conceptURI)
      throws IOException {
    
    String[] broaderConcepts = getBroaderTransitiveConcepts(conceptURI);
    
    if (broaderConcepts == null) return null;
    
    List<String> broaderLabels = new ArrayList<String>();
    
    for (String broaderConcept : broaderConcepts) {
      
      String[] prefLabels = getPrefLabels(broaderConcept);
      if (prefLabels != null) broaderLabels.addAll(Arrays.asList(prefLabels));
      String[] altLabels = getAltLabels(broaderConcept);
      if (altLabels != null) broaderLabels.addAll(Arrays.asList(altLabels));
      
    }
    
    return broaderLabels.toArray(new String[0]);
    
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see
   * at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine#getNarrowerTransitiveLabels
   * (java. lang.String)
   */
  @Override
  public String[] getNarrowerTransitiveLabels(String conceptURI)
      throws IOException {
    
    String[] narrowerConcepts = getNarrowerTransitiveConcepts(conceptURI);
    
    if (narrowerConcepts == null) return null;
    
    List<String> narrowerLabels = new ArrayList<String>();
    
    for (String narrowerConcept : narrowerConcepts) {
      
      String[] prefLabels = getPrefLabels(narrowerConcept);
      if (prefLabels != null) narrowerLabels.addAll(Arrays.asList(prefLabels));
      String[] altLabels = getAltLabels(narrowerConcept);
      if (altLabels != null) narrowerLabels.addAll(Arrays.asList(altLabels));
      
    }
    
    return narrowerLabels.toArray(new String[0]);
    
  }
  
  /**
   * Returns the number of (whitespace separated) terms contained in a label
   * 
   * @param label
   * @return
   */
  private int countLabelTerms(String label) {
    
    return label.split(" ").length;
    
  }
  
}
