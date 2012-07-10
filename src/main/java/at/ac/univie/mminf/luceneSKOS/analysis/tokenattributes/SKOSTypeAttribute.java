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
public interface SKOSTypeAttribute extends Attribute {
  
  /**
   * An enumeration of supported SKOS concept types
   * 
   * @author haslhofer
   * 
   */
  public static enum SKOSType {
    
    PREF, ALT, HIDDEN, BROADER, NARROWER, BROADERTRANSITIVE, NARROWERTRANSITIVE, RELATED;
    
    /**
     * Returns the SKOSType given the ordinal.
     */
    public static SKOSType fromInteger(int ordinal) {
      switch (ordinal) {
        case 0:
          return PREF;
        case 1:
          return ALT;
        case 2:
          return HIDDEN;
        case 3:
          return BROADER;
        case 4:
          return NARROWER;
        case 5:
          return BROADERTRANSITIVE;
        case 6:
          return NARROWERTRANSITIVE;
        case 7:
          return RELATED;
        default:
          return RELATED;
      }
    }
  }
  
  /**
   * Returns the SKOS type
   * 
   * @return SKOSType
   */
  SKOSType getSkosType();
  
  /**
   * Sets this Token's SKOSType.
   * 
   * @param skosType
   */
  void setSkosType(SKOSType skosType);
}
