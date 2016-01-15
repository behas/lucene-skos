package at.ac.univie.mminf.luceneSKOS.analysis;

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
 */
public interface SKOSTypeAttribute extends Attribute {

    /**
     * An enumeration of supported SKOS concept types
     */
    enum SKOSType {

        PREF, ALT, HIDDEN, BROADER, NARROWER, BROADERTRANSITIVE, NARROWERTRANSITIVE, RELATED;
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
     * @param skosType the SKOS type
     */
    void setSkosType(SKOSType skosType);
}
