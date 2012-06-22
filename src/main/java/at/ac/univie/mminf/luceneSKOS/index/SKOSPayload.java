package at.ac.univie.mminf.luceneSKOS.index;

import org.apache.lucene.index.Payload;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttribute;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttributeImpl;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttribute.SKOSType;


/**
 * Encodes a given SKOSAttribute as term payload simply by converting
 * the SKOSAttribute enums to and from string. 
 *
 * TODO: improve efficiency; only one byte is needed!
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 *
 */
public class SKOSPayload extends Payload {

	
	private static final long serialVersionUID = 2036694880487284226L;


	public SKOSPayload(SKOSAttribute skosAtt) {
		
		super();
		
		String attr = skosAtt.getSKOSType().toString();
		
		byte[] pl = attr.getBytes();
		
		super.setData(pl);
		
		
	}
	
	
	public SKOSAttribute getSKOSAttribute() {
		
		if(super.data.length == 0) {
			System.err.println("Error no SKOS Attribute available");
			return null;
		}
		
		byte[] payload = super.getData();
		
		return getSKOSAttribute(payload);
		
		
	}

	
	public static SKOSAttribute getSKOSAttribute(byte[] payload) {
		
		String attr = new String(payload).trim();
		
		SKOSType skosType = null;
		
		if(attr.equals(SKOSType.ALT.toString())) {
			skosType = SKOSType.ALT;
		} else if (attr.equals(SKOSType.HIDDEN.toString())) {
			skosType = SKOSType.HIDDEN;
		} else if (attr.equals(SKOSType.BROADER.toString())) {
			skosType = SKOSType.BROADER;
		} else if (attr.equals(SKOSType.NARROWER.toString())) {
			skosType = SKOSType.NARROWER;
		}
		
		if(skosType == null) {
			return null;
		}
		
		SKOSAttribute skosAttribute = new SKOSAttributeImpl();
		skosAttribute.setSKOSType(skosType);
		
		return skosAttribute;

		
	}
	
	
	
	
}
