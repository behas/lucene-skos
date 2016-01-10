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

import at.ac.univie.mminf.luceneSKOS.skos.engine.SKOSEngine;
import at.ac.univie.mminf.luceneSKOS.skos.engine.SKOSEngineFactory;
import at.ac.univie.mminf.luceneSKOS.tokenattributes.SKOSTypeAttribute;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.miscellaneous.RemoveDuplicatesTokenFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;

import java.io.IOException;
import java.io.Reader;

/**
 * An analyzer for expanding fields that contain either (i) URI references to
 * SKOS concepts OR (ii) SKOS concept prefLabels as values.
 */
public class SKOSAnalyzer extends StopwordAnalyzerBase {

  /**
   * The supported expansion types
   */
  public enum ExpansionType {
    URI, LABEL
  }

  /**
   * Default expansion type
   */
  public static final ExpansionType DEFAULT_EXPANSION_TYPE = ExpansionType.LABEL;
  private ExpansionType expansionType = DEFAULT_EXPANSION_TYPE;
  /**
   * Default skos types to expand to
   */
  public static final SKOSTypeAttribute.SKOSType[] DEFAULT_SKOS_TYPES = new SKOSTypeAttribute.SKOSType[]{
      SKOSTypeAttribute.SKOSType.PREF, SKOSTypeAttribute.SKOSType.ALT, SKOSTypeAttribute.SKOSType.BROADER,
      SKOSTypeAttribute.SKOSType.BROADERTRANSITIVE, SKOSTypeAttribute.SKOSType.NARROWER,
      SKOSTypeAttribute.SKOSType.NARROWERTRANSITIVE};
  private SKOSTypeAttribute.SKOSType[] types = DEFAULT_SKOS_TYPES;
  /**
   * A SKOS Engine instance
   */
  private SKOSEngine skosEngine;
  /**
   * The size of the buffer used for multi-term prediction
   */
  private int bufferSize = SKOSLabelFilter.DEFAULT_BUFFER_SIZE;
  /**
   * Default maximum allowed token length
   */
  public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;
  private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;
  /**
   * An unmodifiable set containing some common English words that are usually
   * not useful for searching.
   */
  public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

  public SKOSAnalyzer(CharArraySet stopWords, SKOSEngine skosEngine, ExpansionType expansionType) {
    super(stopWords);
    this.skosEngine = skosEngine;
    this.expansionType = expansionType;
  }

  public SKOSAnalyzer(SKOSEngine skosEngine, ExpansionType expansionType) {
    this(STOP_WORDS_SET, skosEngine, expansionType);
  }

  public SKOSAnalyzer(Reader stopwords, SKOSEngine skosEngine, ExpansionType expansionType) throws IOException {
    this(loadStopwordSet(stopwords), skosEngine, expansionType);
  }

  public SKOSAnalyzer(CharArraySet stopWords,
                      String indexPath, String skosFile,
                      ExpansionType expansionType, int bufferSize, String... languages)
      throws IOException {
    super(stopWords);
    this.skosEngine = SKOSEngineFactory.getSKOSEngine(indexPath, skosFile, languages);
    this.expansionType = expansionType;
    this.bufferSize = bufferSize;
  }

  public SKOSAnalyzer(String indexPath, String skosFile,
                      ExpansionType expansionType, int bufferSize, String... languages)
      throws IOException {
    this(STOP_WORDS_SET, indexPath, skosFile, expansionType, bufferSize, languages);
  }

  public SKOSAnalyzer(String indexPath, String skosFile,
                      ExpansionType expansionType, int bufferSize) throws IOException {
    this(indexPath, skosFile, expansionType, bufferSize, (String[]) null);
  }

  public SKOSAnalyzer(String indexPath, String skosFile,
                      ExpansionType expansionType) throws IOException {
    this(indexPath, skosFile, expansionType, SKOSLabelFilter.DEFAULT_BUFFER_SIZE);
  }

  public SKOSAnalyzer(Reader stopwords,
                      String indexPath, String skosFile,
                      ExpansionType expansionType, int bufferSize, String... languages)
      throws IOException {
    this(loadStopwordSet(stopwords), indexPath, skosFile,
        expansionType, bufferSize, languages);
  }

  public SKOSTypeAttribute.SKOSType[] getTypes() {
    return types;
  }

  public void setTypes(SKOSTypeAttribute.SKOSType... types) {
    this.types = types;
  }

  /**
   * Set maximum allowed token length. If a token is seen that exceeds this
   * length then it is discarded. This setting only takes effect the next time
   * tokenStream or tokenStream is called.
   *
   * @param length the length
   */
  public void setMaxTokenLength(int length) {
    maxTokenLength = length;
  }

  /**
   * Get maximum token length
   *
   * @return the maximum token length
   * @see #setMaxTokenLength
   */
  public int getMaxTokenLength() {
    return maxTokenLength;
  }

  @Override
  protected TokenStreamComponents createComponents(String fileName) {
    if (expansionType.equals(ExpansionType.URI)) {
      final KeywordTokenizer src = new KeywordTokenizer();
      TokenStream tok = new SKOSURIFilter(src, skosEngine, new StandardAnalyzer(), types);
      tok = new LowerCaseFilter(tok);
      return new TokenStreamComponents(src, tok);
    } else {
      final StandardTokenizer src = new StandardTokenizer();
      src.setMaxTokenLength(maxTokenLength);
      TokenStream tok = new StandardFilter(src);
      // prior to this we get the classic behavior, standardfilter does it for us.
      tok = new SKOSLabelFilter(tok, skosEngine, new StandardAnalyzer(), bufferSize, types);
      tok = new LowerCaseFilter(tok);
      tok = new StopFilter(tok, stopwords);
      tok = new RemoveDuplicatesTokenFilter(tok);
      return new TokenStreamComponents(src, tok) {
        @Override
        protected void setReader(final Reader reader) throws IOException {
          src.setMaxTokenLength(maxTokenLength);
          super.setReader(reader);
        }
      };
    }
  }
}
