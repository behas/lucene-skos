package at.ac.univie.mminf.luceneSKOS.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
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
public class SKOSAnalyzer extends Analyzer {
  
  /**
   * The supported expansion types
   * 
   */
  public enum ExpansionType {
    URI, LABEL
  }
  
  /**
   * A SKOS Engine instance
   */
  private SKOSEngine skosEngine;
  
  /**
   * The expansion type to be applied
   */
  private ExpansionType expansionType = ExpansionType.LABEL;
  
  /**
   * The size of the buffer used for multi-term prediction
   */
  private int bufferSize = 1;
  
  /**
   * Instantiates the SKOSAnalyzer for a given skosFile and expansionType
   * 
   * @param skosFile
   *          the SKOS file to be used
   * @param expansionType
   *          URI or LABEL expansion
   * @throws IOException
   *           if the skosFile cannot be loaded
   */
  public SKOSAnalyzer(String skosFile, ExpansionType expansionType)
      throws IOException {
    
    this.skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile);
    
    this.expansionType = expansionType;
  }
  
  /**
   * Instantiates the SKOSAnalyzer for a given skosFile and expansionType
   * 
   * @param skosFile
   *          the SKOS file to be used
   * @param expansionType
   *          URI or LABEL expansion
   * @param bufferSize
   *          the length of the longest pref-label to consider (needed for
   *          mult-term expansion)
   * @throws IOException
   *           if the skosFile cannot be loaded
   */
  public SKOSAnalyzer(String skosFile, ExpansionType expansionType, int bufferSize)
      throws IOException {
    
    skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile);
    
    this.expansionType = expansionType;
    
    this.bufferSize = bufferSize;
  }
  
  /**
   * Instantiates the SKOSAnalyzer with a given SKOS engine
   * 
   * @param skosEngine
   * @param expansionType
   */
  public SKOSAnalyzer(SKOSEngine skosEngine, ExpansionType expansionType) {
    
    this.skosEngine = skosEngine;
    
    this.expansionType = expansionType;
    
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  protected TokenStreamComponents createComponents(String fileName,
      Reader reader) {
    TokenStreamComponents result = null;
    
    if (expansionType.equals(ExpansionType.URI)) {
      
      Tokenizer kwTokenizer = new KeywordTokenizer(reader);
      
      TokenFilter skosURIFilter = new SKOSURIFilter(kwTokenizer, skosEngine);
      
      TokenFilter lwFilter = new LowerCaseFilter(Version.LUCENE_40,
          skosURIFilter);
      
      result = new TokenStreamComponents(kwTokenizer, lwFilter);
    } else {
      
      Tokenizer stdTokenizer = new StandardTokenizer(Version.LUCENE_40, reader);
      
      TokenFilter stdFilter = new StandardFilter(Version.LUCENE_40,
          stdTokenizer);
      
      // TODO: improve usage of stop filter
      TokenFilter stopFilter = new StopFilter(Version.LUCENE_40, stdFilter,
          StopAnalyzer.ENGLISH_STOP_WORDS_SET);
      
      TokenFilter skosLabelFilter = new SKOSLabelFilter(stopFilter, skosEngine, bufferSize);
      
      TokenFilter lwFilter = new LowerCaseFilter(Version.LUCENE_40,
          skosLabelFilter);
      
      result = new TokenStreamComponents(stdTokenizer, lwFilter);
    }
    
    return result;
  }
  
}