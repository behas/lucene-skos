package at.ac.univie.mminf.luceneSKOS.test;

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
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.BytesRef;

/**
 * Utils for displaying the results of the Lucene analysis process
 */
public class AnalyzerUtils {
  
  public static void displayTokens(Analyzer analyzer, String text)
      throws IOException {
    displayTokens(analyzer.tokenStream("contents", new StringReader(text)));
    
  }
  
  public static void displayTokens(TokenStream stream) throws IOException {
    
    CharTermAttribute term = stream.addAttribute(CharTermAttribute.class);
    while (stream.incrementToken()) {
      System.out.println("[" + term.toString() + "] ");
    }
    
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
      
      BytesRef pl = payload.getPayload();
      
      if (pl != null) {
        System.out.print("[" + term.toString() + ":" + offset.startOffset()
            + "->" + offset.endOffset() + ":" + type.type() + ":"
            + new String(pl.bytes) + "] ");
        
      } else {
        System.out.print("[" + term.toString() + ":" + offset.startOffset()
            + "->" + offset.endOffset() + ":" + type.type() + "] ");
        
      }
      
    }
    System.out.println();
  }
  
}
