package at.ac.univie.mminf.luceneSKOS.index;

import org.apache.lucene.index.Payload;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttributeImpl;

/**
 * Encodes a given SKOSAttribute as term payload simply by converting the
 * SKOSAttribute enums to and from string.
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public class SKOSTypePayload extends Payload {
  
  public SKOSTypePayload(SKOSTypeAttribute skosAtt) {
    super();
    int attr = skosAtt.getSKOSType().ordinal();
    byte[] pl = new byte[] {(byte) attr};
    super.setData(pl);
  }
  
  public SKOSTypeAttribute getSKOSAttribute() {
    if (super.data.length == 0) {
      System.err.println("Error no SKOS Attribute available");
      return null;
    }
    
    byte[] payload = super.getData();
    return getSKOSAttribute(payload);
  }
  
  public static SKOSTypeAttribute getSKOSAttribute(byte[] payload) {
    int attr = payload[0];
    SKOSType skosType = SKOSType.fromInteger(attr);
    
    if (skosType == null) {
      return null;
    }
    
    SKOSTypeAttribute skosAttribute = new SKOSTypeAttributeImpl();
    skosAttribute.setSKOSType(skosType);
    return skosAttribute;
  }
}
