package at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes;

import org.apache.lucene.util.Attribute;

/**
 * This class represents SKOS-specific meta-information that is assigned to
 * tokens during the analysis phase.
 * 
 * Note: when tokens are posted to the index as terms, attribute information is
 * lost unless it is encoded in the terms' payload.
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public interface SKOSAttribute extends Attribute {
  
  /**
   * An enumeration of supported SKOS concept types
   * 
   * @author haslhofer
   * 
   */
  public static enum SKOSType {
    
    PREF, ALT, HIDDEN, BROADER, NARROWER;
    
  }
  
  /**
   * Sets the SKOS type
   * 
   * @param sl
   */
  public void setSKOSType(SKOSType sl);
  
  /**
   * Returns the SKOS type
   * 
   * @return
   */
  public SKOSType getSKOSType();
  
}
