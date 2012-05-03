package at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes;

import org.apache.lucene.util.AttributeImpl;

/**
 * Implementation class for SKOS-specific attributes
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public class SKOSAttributeImpl extends AttributeImpl implements SKOSAttribute {
  
  private SKOSType st;
  
  @Override
  public void setSKOSType(SKOSType st) {
    this.st = st;
  }
  
  @Override
  public SKOSType getSKOSType() {
    return this.st;
  }
  
  @Override
  public void clear() {
    st = null;
  }
  
  @Override
  public void copyTo(AttributeImpl other) {
    ((SKOSAttribute) other).setSKOSType(st);
  }
  
  @Override
  public String toString() {
    return st.toString();
  }
}
