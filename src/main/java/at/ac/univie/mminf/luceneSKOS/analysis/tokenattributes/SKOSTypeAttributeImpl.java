package at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes;

import org.apache.lucene.util.AttributeImpl;

/**
 * The SKOSType of a Token. See also {@link SKOSType}.
 */
public class SKOSTypeAttributeImpl extends AttributeImpl implements
    SKOSTypeAttribute, Cloneable {
  private SKOSType skosType;
  
  /**
   * Initialize this attribute with no SKOSType.
   */
  public SKOSTypeAttributeImpl() {
    super();
  }
  
  /**
   * Initialize this attribute with the given SKOSType.
   */
  public SKOSTypeAttributeImpl(SKOSType skosType) {
    super();
    this.skosType = skosType;
  }
  
  /**
   * Returns this Token's SKOSType.
   */
  @Override
  public SKOSType getSkosType() {
    return skosType;
  }
  
  /**
   * Sets this Token's SKOSType.
   */
  @Override
  public void setSkosType(SKOSType skosType) {
    this.skosType = skosType;
  }
  
  @Override
  public void clear() {
    skosType = null;
  }
  
  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    
    if (other instanceof SKOSTypeAttribute) {
      final SKOSTypeAttributeImpl otherImpl = (SKOSTypeAttributeImpl) other;
      return (this.skosType == null ? otherImpl.skosType == null
          : this.skosType.equals(otherImpl.skosType));
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return (skosType == null) ? 0 : skosType.hashCode();
  }
  
  @Override
  public void copyTo(AttributeImpl target) {
    SKOSTypeAttribute type = (SKOSTypeAttribute) target;
    type.setSkosType(skosType);
  }
}