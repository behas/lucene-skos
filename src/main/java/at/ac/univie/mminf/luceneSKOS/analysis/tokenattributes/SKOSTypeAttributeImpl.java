package at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes;

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

import org.apache.lucene.util.AttributeImpl;

/**
 * The SKOSType of a Token. See also {@link SKOSType}.
 */
public class SKOSTypeAttributeImpl extends AttributeImpl implements
    SKOSTypeAttribute, Cloneable {
  
  private static final long serialVersionUID = 1L;
  
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
