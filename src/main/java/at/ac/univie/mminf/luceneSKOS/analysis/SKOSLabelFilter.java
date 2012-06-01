package at.ac.univie.mminf.luceneSKOS.analysis;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine;

/**
 * A Lucene TokenFilter that supports label-based term expansion as described in
 * https://code.
 * google.com/p/lucene-skos/wiki/UseCases#UC2:_Label-based_term_expansion.
 * 
 * It takes labels (String values) as input and searches a given SKOS vocabulary
 * for matching concepts (based on their prefLabels). If a match is found, it
 * adds the concept's labels to the output token stream.
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * @author Martin Kysel <martin.kysel@univie.ac.at>
 * 
 */
public final class SKOSLabelFilter extends SKOSFilter {
  
  public static final int DEFAULT_BUFFER_SIZE = 1;
  
  /* the size of the buffer used for multi-term prediction */
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  
  /* a list serving as token buffer between consumed and consuming stream */
  private Queue<State> buffer = new LinkedList<State>();
  
  /**
   * Constructor for multi-term expansion support. Takes an input token stream,
   * the SKOS engine, and an integer indicating the maximum token length of the
   * preferred labels in the SKOS vocabulary.
   * 
   * @param in
   *          the consumed token stream
   * @param skosEngine
   *          the skos expansion engine
   * @param bufferSize
   *          the length of the longest pref-label to consider (needed for
   *          mult-term expansion)
   * @param types
   *          the skos types to expand to
   */
  public SKOSLabelFilter(TokenStream in, SKOSEngine skosEngine,
      Analyzer analyzer, int bufferSize, SKOSType... types) {
    super(in, skosEngine, analyzer, types);
    this.bufferSize = bufferSize;
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
    
    while (buffer.size() < bufferSize && input.incrementToken()) {
      buffer.add(input.captureState());
      
    }
    
    if (buffer.isEmpty()) {
      return false;
    }
    
    restoreState(buffer.peek());
    
    /* check whether there are expanded terms for a given token */
    if (addAliasesToStack()) {
      /* if yes, capture the state of all attributes */
      current = captureState();
    }
    
    buffer.remove();
    
    return true;
  }
  
  private boolean addAliasesToStack() throws IOException {
    for (int i = buffer.size(); i > 0; i--) {
      String inputTokens = bufferToString(i);
      
      if (addTermsToStack(inputTokens)) {
        break;
      }
      
    }
    
    if (termStack.isEmpty()) {
      return false;
    }
    
    return true;
  }
  
  /**
   * Converts the first x=noTokens states in the queue to a concatenated token
   * string separated by white spaces
   */
  private String bufferToString(int noTokens) {
    State entered = captureState();
    
    State[] bufferedStates = buffer.toArray(new State[0]);
    
    StringBuilder sb = new StringBuilder();
    sb.append(termAtt.toString());
    restoreState(bufferedStates[0]);
    for (int i = 1; i < noTokens; i++) {
      restoreState(bufferedStates[i]);
      sb.append(" " + termAtt.toString());
    }
    
    restoreState(entered);
    
    return sb.toString();
  }
  
  /**
   * Assumes that the given term is a textual token
   * 
   */
  public boolean addTermsToStack(String term) throws IOException {
    try {
      String[] conceptURIs = engine.getConcepts(term);
      
      for (String conceptURI : conceptURIs) {
        if (types.contains(SKOSType.PREF)) {
          String[] prefLabels = engine.getPrefLabels(conceptURI);
          pushLabelsToStack(prefLabels, SKOSType.PREF);
        }
        if (types.contains(SKOSType.ALT)) {
          String[] altLabels = engine.getAltLabels(conceptURI);
          pushLabelsToStack(altLabels, SKOSType.ALT);
        }
        if (types.contains(SKOSType.HIDDEN)) {
          String[] hiddenLabels = engine.getHiddenLabels(conceptURI);
          pushLabelsToStack(hiddenLabels, SKOSType.HIDDEN);
        }
        if (types.contains(SKOSType.BROADER)) {
          String[] broaderLabels = engine.getBroaderLabels(conceptURI);
          pushLabelsToStack(broaderLabels, SKOSType.BROADER);
        }
        if (types.contains(SKOSType.BROADERTRANSITIVE)) {
          String[] broaderTransitiveLabels = engine
              .getBroaderTransitiveLabels(conceptURI);
          pushLabelsToStack(broaderTransitiveLabels, SKOSType.BROADERTRANSITIVE);
        }
        if (types.contains(SKOSType.NARROWER)) {
          String[] narrowerLabels = engine.getNarrowerLabels(conceptURI);
          pushLabelsToStack(narrowerLabels, SKOSType.NARROWER);
        }
        if (types.contains(SKOSType.NARROWERTRANSITIVE)) {
          String[] narrowerTransitiveLabels = engine
              .getNarrowerTransitiveLabels(conceptURI);
          pushLabelsToStack(narrowerTransitiveLabels,
              SKOSType.NARROWERTRANSITIVE);
        }
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
  
  public int getBufferSize() {
    return this.bufferSize;
  }
}
