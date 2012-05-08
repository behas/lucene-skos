package at.ac.univie.mminf.luceneSKOS.index;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.Payload;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttributeImpl;

/**
 * Encodes a given SKOSAttribute as term payload simply by converting the
 * SKOSAttribute enums to and from int.
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public class SKOSTypePayload extends Payload {
  
  public SKOSTypePayload(SKOSTypeAttribute skosAtt) {
    super();
    int payload = skosAtt.getSKOSType().ordinal();
    byte[] bytes = PayloadHelper.encodeInt(payload);
    super.setData(bytes);
  }
  
  public SKOSTypeAttribute getSKOSTypeAttribute() {
    if (super.data.length == 0) {
      System.err.println("Error no SKOS Attribute available");
      return null;
    }
    
    byte[] bytes = super.getData();
    return getSKOSTypeAttribute(bytes);
  }
  
  public static SKOSTypeAttribute getSKOSTypeAttribute(byte[] bytes) {
    int payload = PayloadHelper.decodeInt(bytes, 0);
    SKOSType skosType = SKOSType.fromInteger(payload);
    
    if (skosType == null) {
      return null;
    }
    
    return new SKOSTypeAttributeImpl(skosType);
  }
}
