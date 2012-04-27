package at.ac.univie.mminf.luceneSKOS.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttribute.SKOSType;
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
public final class SKOSURIFilter extends SKOSFilter {
  
  /**
   * Constructor.
   * 
   * @param in
   * @param skosEngine
   */
  public SKOSURIFilter(TokenStream in, SKOSEngine skosEngine) {
    super(in, skosEngine);
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
    if (!input.incrementToken()) return false;
    
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
  // @Override
  public boolean addTermsToStack(String term) throws IOException {
    
    try {
      
      String[] prefLabels = engine.getPrefLabels(term);
      pushLabelsToStack(prefLabels, SKOSType.PREF);
      
      String[] altLabels = engine.getAltLabels(term);
      pushLabelsToStack(altLabels, SKOSType.ALT);
      
      String[] broaderLabels = engine.getBroaderLabels(term);
      pushLabelsToStack(broaderLabels, SKOSType.BROADER);
      
      String[] narrowerLabels = engine.getNarrowerLabels(term);
      pushLabelsToStack(narrowerLabels, SKOSType.NARROWER);
      
      String[] broaderTransitiveLabels = engine
          .getBroaderTransitiveLabels(term);
      pushLabelsToStack(broaderTransitiveLabels, SKOSType.BROADER_TRANSITIVE);
      
      String[] narrowerTransitiveLabels = engine
          .getNarrowerTransitiveLabels(term);
      pushLabelsToStack(narrowerTransitiveLabels, SKOSType.NARROWER_TRANSITIVE);
      
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
