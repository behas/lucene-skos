package at.ac.univie.mminf.luceneSKOS.analysis;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
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
  
  /* the skos types to expand to */
  protected Set<SKOSType> types;
  
  /* provides access to the the term attributes */
  protected AttributeSource.State current;
  
  /* the term text (propagated to the index) */
  protected final CharTermAttribute termAtt;
  
  /* the token position relative to the previous token (propagated) */
  protected final PositionIncrementAttribute posIncrAtt;
  
  /* the binary payload attached to the indexed term (propagated to the index) */
  protected final PayloadAttribute payloadAtt;
  
  /* the SKOS-specific attribute attached to a term */
  protected final SKOSTypeAttribute skosAtt;
  
  /* the analyzer to use when parsing */
  protected final Analyzer analyzer;
  
  /**
   * Constructor
   * 
   * @param in
   *          the TokenStream
   * @param engine
   *          the engine delivering skos concepts
   * @param type
   *          the skos types to expand to
   */
  public SKOSFilter(TokenStream in, SKOSEngine engine, Analyzer analyzer,
      SKOSType... types) {
    super(in);
    termStack = new Stack<ExpandedTerm>();
    this.engine = engine;
    this.analyzer = analyzer;
    
    if (types != null) this.types = new TreeSet<SKOSType>(Arrays.asList(types));
    else this.types = new TreeSet<SKOSType>(Arrays.asList(new SKOSType[] {
        SKOSType.PREF, SKOSType.ALT}));
    
    this.termAtt = addAttribute(CharTermAttribute.class);
    this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    this.payloadAtt = addAttribute(PayloadAttribute.class);
    this.skosAtt = addAttribute(SKOSTypeAttribute.class);
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
   * 
   * @throws IOException
   */
  protected void processTermOnStack() throws IOException {
    ExpandedTerm expandedTerm = termStack.pop();
    
    String term = expandedTerm.getTerm();
    
    SKOSType termType = expandedTerm.getTermType();
    
    String sTerm = "";
    
    try {
      sTerm = analyze(analyzer, term, new CharsRef()).toString();
    } catch (IllegalArgumentException e) {
      // skip this term
      return;
    }
    
    /*
     * copies the values of all attribute implementations from this state into
     * the implementations of the target stream
     */
    restoreState(current);
    
    /*
     * Adds the expanded term to the term buffer
     */
    termAtt.setEmpty().append(sTerm);
    
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
    byte[] bytes = PayloadHelper.encodeInt(skosAtt.getSKOSType().ordinal());
    payloadAtt.setPayload(new BytesRef(bytes));
  }
  
  /* Snipped from Solr's SynonymMap */
  public static CharsRef analyze(Analyzer analyzer, String text, CharsRef reuse)
      throws IOException {
    TokenStream ts = analyzer.tokenStream("", new StringReader(text));
    CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
    // PositionIncrementAttribute posIncAtt =
    // ts.addAttribute(PositionIncrementAttribute.class);
    ts.reset();
    reuse.length = 0;
    while (ts.incrementToken()) {
      int length = termAtt.length();
      if (length == 0) {
        throw new IllegalArgumentException("term: " + text
            + " analyzed to a zero-length token");
      }
      // if (posIncAtt.getPositionIncrement() != 1) {
      // throw new IllegalArgumentException("term: " + text +
      // " analyzed to a token with posinc != 1");
      // }
      reuse.grow(reuse.length + length + 1); /* current + word + separator */
      int end = reuse.offset + reuse.length;
      if (reuse.length > 0) {
        reuse.chars[end++] = 32; // space
        reuse.length++;
      }
      System.arraycopy(termAtt.buffer(), 0, reuse.chars, end, length);
      reuse.length += length;
    }
    ts.end();
    ts.close();
    if (reuse.length == 0) {
      throw new IllegalArgumentException("term: " + text
          + " was completely eliminated by analyzer");
    }
    return reuse;
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
  }
}
