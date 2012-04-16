package at.ac.univie.mminf.luceneSKOS.util;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.Payload;

/**
 * Utils for displaying the results of the Lucene analysis process
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * 
 */
public class AnalyzerUtils {
  
  public static void displayTokens(Analyzer analyzer, String text)
      throws IOException {
    displayTokens(analyzer.tokenStream("contents", new StringReader(text)));
    
  }
  
  public static void displayTokens(TokenStream stream) throws IOException {
    
    CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
    while (stream.incrementToken())
      System.out.println("[" + term.toString() + "] ");
    
  }
  
  public static void displayTokensWithPositions(Analyzer analyzer, String text)
      throws IOException {
    
    TokenStream stream = analyzer.tokenStream("contents",
        new StringReader(text));
    
    CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
    PositionIncrementAttribute posIncr = stream
        .addAttribute(PositionIncrementAttribute.class);
    
    int position = 0;
    while (stream.incrementToken()) {
      
      int increment = posIncr.getPositionIncrement();
      if (increment > 0) {
        position = position + increment;
        System.out.println();
        System.out.print(position + ":");
      }
      
      System.out.print("[" + term.toString() + "] ");
      
    }
    System.out.println();
    
  }
  
  public static void displayTokensWithFullDetails(Analyzer analyzer, String text)
      throws IOException {
    
    TokenStream stream = analyzer.tokenStream("contents",
        new StringReader(text));
    
    CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
    PositionIncrementAttribute posIncr = stream
        .addAttribute(PositionIncrementAttribute.class);
    OffsetAttribute offset = stream.addAttribute(OffsetAttribute.class);
    TypeAttribute type = stream.addAttribute(TypeAttribute.class);
    PayloadAttribute payload = stream.addAttribute(PayloadAttribute.class);
    
    int position = 0;
    while (stream.incrementToken()) {
      
      int increment = posIncr.getPositionIncrement();
      if (increment > 0) {
        position = position + increment;
        System.out.println();
        System.out.print(position + ":");
      }
      
      Payload pl = payload.getPayload();
      
      if (pl != null) {
        System.out.print("[" + term.toString() + ":" + offset.startOffset() + "->"
            + offset.endOffset() + ":" + type.type() + ":"
            + new String(pl.getData()) + "] ");
        
      } else {
        System.out.print("[" + term.toString() + ":" + offset.startOffset() + "->"
            + offset.endOffset() + ":" + type.type() + "] ");
        
      }
      
    }
    System.out.println();
  }
  
}
