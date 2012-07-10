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

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine;

/**
 * A Lucene TokenFilter that supports URI-based term expansion as described in
 * https://code.
 * google.com/p/lucene-skos/wiki/UseCases#UC1:_URI-based_term_expansion
 * 
 * It takes references to SKOS concepts (URIs) as input and searches a given
 * SKOS vocabulary for matching concepts. If a match is found, it adds the
 * concept's labels to the output token stream.
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public final class SKOSURIFilter extends AbstractSKOSFilter {
  
  /**
   * Constructor.
   * 
   * @param input
   * @param skosEngine
   * @param types
   */
  public SKOSURIFilter(TokenStream input, SKOSEngine skosEngine,
      Analyzer analyzer, SKOSType... types) {
    super(input, skosEngine, analyzer, types);
  }
  
  /**
   * Advances the stream to the next token
   */
  @Override
  public boolean incrementToken() throws IOException {
    
    /* there are expanded terms for the given token */
    if (termStack.size() > 0) {
      processTermOnStack();
      return true;
    }
    
    /* no more tokens on the consumed stream -> end of stream */
    if (!input.incrementToken()) {
      return false;
    }
    
    /* check whether there are expanded terms for a given token */
    if (addTermsToStack(termAtt.toString())) {
      
      /* if yes, capture the state of all attributes */
      current = captureState();
    }
    
    return true;
  }
  
  /**
   * Assumes that the given term is a concept URI
   */
  public boolean addTermsToStack(String term) throws IOException {
    try {
      if (types.contains(SKOSType.PREF)) {
        String[] prefLabels = engine.getPrefLabels(term);
        pushLabelsToStack(prefLabels, SKOSType.PREF);
      }
      if (types.contains(SKOSType.ALT)) {
        String[] altLabels = engine.getAltLabels(term);
        pushLabelsToStack(altLabels, SKOSType.ALT);
      }
      if (types.contains(SKOSType.BROADER)) {
        String[] broaderLabels = engine.getBroaderLabels(term);
        pushLabelsToStack(broaderLabels, SKOSType.BROADER);
      }
      if (types.contains(SKOSType.BROADERTRANSITIVE)) {
        String[] broaderTransitiveLabels = engine
            .getBroaderTransitiveLabels(term);
        pushLabelsToStack(broaderTransitiveLabels, SKOSType.BROADERTRANSITIVE);
      }
      if (types.contains(SKOSType.NARROWER)) {
        String[] narrowerLabels = engine.getNarrowerLabels(term);
        pushLabelsToStack(narrowerLabels, SKOSType.NARROWER);
      }
      if (types.contains(SKOSType.NARROWERTRANSITIVE)) {
        String[] narrowerTransitiveLabels = engine
            .getNarrowerTransitiveLabels(term);
        pushLabelsToStack(narrowerTransitiveLabels, SKOSType.NARROWERTRANSITIVE);
      }
    } catch (Exception e) {
      System.err
          .println("Error when accessing SKOS Engine.\n" + e.getMessage());
    }
    
    if (termStack.isEmpty()) {
      return false;
    }
    
    return true;
  }
}
