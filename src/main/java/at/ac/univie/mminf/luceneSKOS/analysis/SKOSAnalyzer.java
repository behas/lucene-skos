package at.ac.univie.mminf.luceneSKOS.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
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
	 * Instantiates the SKOSAnalyzer for a given skosFile and expansionType
	 * 
	 * @param skosFile
	 *            the SKOS file to be used
	 * @param expansionType
	 *            URI or LABEL expansion
	 * @throws IOException
	 *             if the skosFile cannot be loaded
	 */
	public SKOSAnalyzer(String skosFile, ExpansionType expansionType) throws IOException {

		this.skosEngine = SKOSEngineFactory.getSKOSEngine(skosFile);

		this.expansionType = expansionType;
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
	public TokenStream tokenStream(String fieldName, Reader reader) {

		TokenStream result = null;

		if (expansionType.equals(ExpansionType.URI)) {

			Tokenizer kwTokenizer = new KeywordTokenizer(reader);

			TokenFilter skosURIFilter = new SKOSURIFilter(kwTokenizer, skosEngine);

			result = new LowerCaseFilter(skosURIFilter);

		} else {

			Tokenizer stdTokenizer = new StandardTokenizer(Version.LUCENE_30, reader);

			TokenFilter stdFilter = new StandardFilter(stdTokenizer);

			// TODO: improve usage of stop filter
			TokenFilter stopFilter = new StopFilter(true, stdFilter,
					StopAnalyzer.ENGLISH_STOP_WORDS_SET);

			TokenFilter skosLabelFilter;

			skosLabelFilter = new SKOSLabelFilter(stopFilter, skosEngine);

			result = new LowerCaseFilter(skosLabelFilter);

		}

		return result;

	}

	// TODO: implement reusableTokenStream(...) for performance improvement

}