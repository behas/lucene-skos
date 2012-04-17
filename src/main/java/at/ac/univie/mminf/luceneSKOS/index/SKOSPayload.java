package at.ac.univie.mminf.luceneSKOS.index;

import org.apache.lucene.index.Payload;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttribute;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttributeImpl;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttribute.SKOSType;

/**
 * Encodes a given SKOSAttribute as term payload simply by converting the
 * SKOSAttribute enums to and from string.
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public class SKOSPayload extends Payload {
  
  public SKOSPayload(SKOSAttribute skosAtt) {
    
    super();
    
    int attr = skosAtt.getSKOSType().ordinal();
    
    byte[] pl = new byte[] {(byte) attr};
    
    super.setData(pl);
    
  }
  
  public SKOSAttribute getSKOSAttribute() {
    
    if (super.data.length == 0) {
      System.err.println("Error no SKOS Attribute available");
      return null;
    }
    
    byte[] payload = super.getData();
    
    return getSKOSAttribute(payload);
    
  }
  
  public static SKOSAttribute getSKOSAttribute(byte[] payload) {
    
    int attr = payload[0];
    
    SKOSType skosType = SKOSType.fromInteger(attr);
    
    if (skosType == null) {
      return null;
    }
    
    SKOSAttribute skosAttribute = new SKOSAttributeImpl();
    skosAttribute.setSKOSType(skosType);
    
    return skosAttribute;
    
  }
  
}
