package at.ac.univie.mminf.luceneSKOS.analysis;

import java.io.IOException;
import java.util.Stack;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttribute;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.index.SKOSPayload;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine;

/**
 * A SKOS-specific TokenFilter implementation
 * 
 * @author haslhofer
 * 
 */
public abstract class SKOSFilter extends TokenFilter {
  
  /* a stack holding the expanded terms for a token */
  protected Stack<ExpandedTerm> termStack;
  
  /* an engine delivering SKOS concepts */
  protected SKOSEngine engine;
  
  /* provides access to the the term attributes */
  protected AttributeSource.State current;
  
  /* the term text (propagated to the index) */
  protected final CharTermAttribute termAtt;
  
  /* the token position relative to the previous token (propagated) */
  protected final PositionIncrementAttribute posIncrAtt;
  
  /* the binary payload attached to the indexed term (propagated to the index) */
  protected final PayloadAttribute payloadAtt;
  
  /* the SKOS-specific attribute attached to a term */
  protected final SKOSAttribute skosAtt;
  
  /**
   * Constructor
   * 
   * @param in
   *          the TokenStream
   * @param engine
   *          the engine delivering skos concepts
   */
  public SKOSFilter(TokenStream in, SKOSEngine engine) {
    super(in);
    termStack = new Stack<ExpandedTerm>();
    this.engine = engine;
    this.termAtt = addAttribute(CharTermAttribute.class);
    this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    this.payloadAtt = addAttribute(PayloadAttribute.class);
    this.skosAtt = addAttribute(SKOSAttribute.class);
    
  }
  
  /**
   * Advances the stream to the next token.
   * 
   * To be implemented by the concrete sub-classes
   */
  @Override
  public abstract boolean incrementToken() throws IOException;
  
  /**
   * Replaces the current term (attributes) with term (attributes) from the
   * stack
   */
  protected void processTermOnStack() {
    
    ExpandedTerm expandedTerm = termStack.pop();
    
    String term = expandedTerm.getTerm();
    
    SKOSType termType = expandedTerm.getTermType();
    
    /*
     * copies the values of all attribute implementations from this state into
     * the implementations of the target stream
     */
    restoreState(current);
    
    /*
     * Adds the expanded term to the term buffer
     */
    termAtt.setEmpty().append(term);
    
    /*
     * set position increment to zero to put multiple terms into the same
     * position
     */
    posIncrAtt.setPositionIncrement(0);
    
    /*
     * sets the type of the expanded term (pref, alt, broader, narrower, etc.)
     */
    skosAtt.setSKOSType(termType);
    
    /*
     * converts the SKOS Attribute to a payload, which is propagated to the
     * index
     */
    payloadAtt.setPayload(new SKOSPayload(skosAtt));
    
  }
  
  /**
   * Pushes a given set of labels onto the stack
   * 
   * @param labels
   * @param type
   */
  protected void pushLabelsToStack(String[] labels, SKOSType type) {
    
    if (labels != null) {
      for (String label : labels) {
        termStack.push(new ExpandedTerm(label, type));
      }
    }
    
  }
  
  /**
   * Helper class for capturing terms and term types
   * 
   * @author haslhofer
   * 
   */
  protected static class ExpandedTerm {
    
    private String term;
    
    private SKOSType termType;
    
    protected ExpandedTerm(String term, SKOSType termType) {
      this.term = term;
      this.termType = termType;
    }
    
    protected String getTerm() {
      return this.term;
    }
    
    protected SKOSType getTermType() {
      return this.termType;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((term == null) ? 0 : term.hashCode());
      result = prime * result + ((termType == null) ? 0 : termType.hashCode());
      return result;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ExpandedTerm other = (ExpandedTerm) obj;
      if (term == null) {
        if (other.term != null) return false;
      } else if (!term.equals(other.term)) return false;
      if (termType != other.termType) return false;
      return true;
    }
    
  }
  
}
