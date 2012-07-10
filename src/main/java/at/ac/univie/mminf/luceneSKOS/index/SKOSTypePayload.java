package at.ac.univie.mminf.luceneSKOS.index;

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
  
  private static final long serialVersionUID = 1L;
  
  public SKOSTypePayload(SKOSTypeAttribute skosAtt) {
    super();
    int payload = skosAtt.getSkosType().ordinal();
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
