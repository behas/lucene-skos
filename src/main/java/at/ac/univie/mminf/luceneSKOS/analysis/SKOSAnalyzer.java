package at.ac.univie.mminf.luceneSKOS.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngineFactory;

/**
 * An analyzer for expanding fields that contain either (i) URI references to
 * SKOS concepts OR (ii) SKOS concept prefLabels as values.
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * @author Martin Kysel <martin.kysel@univie.ac.at>
 * 
 */
public class SKOSAnalyzer extends StopwordAnalyzerBase {
  
  /** The supported expansion types */
  public enum ExpansionType {
    URI, LABEL
  }
  
  /** Default expansion type */
  public static final ExpansionType DEFAULT_EXPANSION_TYPE = ExpansionType.LABEL;
  
  private ExpansionType expansionType = DEFAULT_EXPANSION_TYPE;
  
  /** A SKOS Engine instance */
  private SKOSEngine skosEngine;
  
  /** The size of the buffer used for multi-term prediction */
  private int bufferSize = SKOSLabelFilter.DEFAULT_BUFFER_SIZE;
  
  /** Default maximum allowed token length */
  public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;
  
  private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;
  
  /**
   * An unmodifiable set containing some common English words that are usually
   * not useful for searching.
   */
  public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
  
  public SKOSAnalyzer(Version matchVersion, CharArraySet stopWords,
      SKOSEngine skosEngine, ExpansionType expansionType) {
    super(matchVersion, stopWords);
    this.skosEngine = skosEngine;
    this.expansionType = expansionType;
  }
  
  public SKOSAnalyzer(Version matchVersion, SKOSEngine skosEngine,
      ExpansionType expansionType) {
    this(matchVersion, STOP_WORDS_SET, skosEngine, expansionType);
  }
  
  public SKOSAnalyzer(Version matchVersion, Reader stopwords,
      SKOSEngine skosEngine, ExpansionType expansionType) throws IOException {
    this(matchVersion, loadStopwordSet(stopwords, matchVersion), skosEngine,
        expansionType);
  }
  
  public SKOSAnalyzer(Version matchVersion, CharArraySet stopWords,
      String skosFile, ExpansionType expansionType, int bufferSize,
      String... languages) throws IOException {
    super(matchVersion, stopWords);
    this.skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile, languages);
    this.expansionType = expansionType;
    this.bufferSize = bufferSize;
  }
  
  public SKOSAnalyzer(Version matchVersion, String skosFile,
      ExpansionType expansionType, int bufferSize, String... languages)
      throws IOException {
    this(matchVersion, STOP_WORDS_SET, skosFile, expansionType, bufferSize,
        languages);
  }
  
  public SKOSAnalyzer(Version matchVersion, String skosFile,
      ExpansionType expansionType, int bufferSize) throws IOException {
    this(matchVersion, skosFile, expansionType, bufferSize, (String[]) null);
  }
  
  public SKOSAnalyzer(Version matchVersion, String skosFile,
      ExpansionType expansionType) throws IOException {
    this(matchVersion, skosFile, expansionType,
        SKOSLabelFilter.DEFAULT_BUFFER_SIZE);
  }
  
  public SKOSAnalyzer(Version matchVersion, Reader stopwords, String skosFile,
      ExpansionType expansionType, int bufferSize, String... languages)
      throws IOException {
    this(matchVersion, loadStopwordSet(stopwords, matchVersion), skosFile,
        expansionType, bufferSize, languages);
  }
  
  /**
   * Set maximum allowed token length. If a token is seen that exceeds this
   * length then it is discarded. This setting only takes effect the next time
   * tokenStream or tokenStream is called.
   */
  public void setMaxTokenLength(int length) {
    maxTokenLength = length;
  }
  
  /**
   * @see #setMaxTokenLength
   */
  public int getMaxTokenLength() {
    return maxTokenLength;
  }
  
  @Override
  protected TokenStreamComponents createComponents(String fileName,
      Reader reader) {
    if (expansionType.equals(ExpansionType.URI)) {
      final KeywordTokenizer src = new KeywordTokenizer(reader);
      TokenStream tok = new SKOSURIFilter(src, skosEngine);
      tok = new LowerCaseFilter(Version.LUCENE_40, tok);
      return new TokenStreamComponents(src, tok);
    } else {
      final StandardTokenizer src = new StandardTokenizer(Version.LUCENE_40,
          reader);
      src.setMaxTokenLength(maxTokenLength);
      TokenStream tok = new StandardFilter(Version.LUCENE_40, src);
      tok = new StopFilter(Version.LUCENE_40, stdFilter,
              StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      tok = new SKOSLabelFilter(tok, skosEngine, bufferSize);
      tok = new LowerCaseFilter(Version.LUCENE_40, tok);
      tok = new StopFilter(Version.LUCENE_40, tok, stopwords);
      return new TokenStreamComponents(src, tok) {
        @Override
        protected void reset(final Reader reader) throws IOException {
          src.setMaxTokenLength(maxTokenLength);
          super.reset(reader);
        }
      };
    }
  }
}