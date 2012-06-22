package at.ac.univie.mminf.luceneSKOS.analysis;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.lucene.analysis.TokenStream;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSAttribute.SKOSType;
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
public class SKOSLabelFilter extends SKOSFilter {

	/* the size of the buffer used for multi-term prediction */
	private int bufferSize = 1;

	/* a list serving as token buffer between consumed and consuming stram */
	private Queue<State> buffer = new LinkedList<State>();

	/**
	 * Constructor for multi-term expansion support. Takes an input token
	 * stream, the SKOS engine, and an integer indicating the maximum token
	 * length of the preferred labels in the SKOS vocabulary.
	 * 
	 * @param in
	 *            the consumed token stream
	 * @param skosEngine
	 *            the skos expansion engine
	 * @param bufferSize
	 *            the length of the longest pref-label in the SKOS thesaurus
	 *            (needed for mult-term expansion)
	 */
	public SKOSLabelFilter(TokenStream in, SKOSEngine skosEngine) {

		super(in, skosEngine);

		int maxPrefLabelTerms = skosEngine.getMaxPrefLabelTerms();
		
		if (maxPrefLabelTerms > 0) {
			bufferSize = maxPrefLabelTerms;
		}

	}


	/**
	 * Advances the stream to the next token
	 */
	public boolean incrementToken() throws IOException {

		/* there are expanded terms for the given token */
		if (termStack.size() > 0) {
			processTermOnStack();
			return true;
		}

		while (buffer.size() < bufferSize && input.incrementToken()) {
			buffer.add(input.captureState());

		}

		if(buffer.isEmpty()) {
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
	 * Converts the first x=noTokens states in the queue to a concated token
	 * string separated by white spaces
	 * 
	 * @param noTokens
	 * @return
	 */
	private String bufferToString(int noTokens) {

		State entered = captureState();

		State[] bufferedStates = buffer.toArray(new State[0]);

		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < noTokens; i++) {

			restoreState(bufferedStates[i]);

			sb.append(termAtt.term());
			sb.append(" ");
		}

		String result = sb.toString();

		if (result.endsWith(" ")) {
			result = result.substring(0, result.length() - 1);
		}

		restoreState(entered);

		return result;

	}


	/**
	 * Assumes that the given term is a textual token
	 * 
	 */
	// @Override
	public boolean addTermsToStack(String term) throws IOException {

		try {

			String[] conceptURIs = engine.getConcepts(term);

			for (String conceptURI : conceptURIs) {

				String[] altLabels = engine.getAltLabels(conceptURI);
				pushLabelsToStack(altLabels, SKOSType.ALT);

				String[] broaderLabels = engine.getBroaderLabels(conceptURI);
				pushLabelsToStack(broaderLabels, SKOSType.BROADER);

				String[] narrowerLabels = engine.getNarrowerLabels(conceptURI);
				pushLabelsToStack(narrowerLabels, SKOSType.NARROWER);

			}

		} catch (Exception e) {
			System.err.println("Error when accessing SKOS Engine.\n" + e.getMessage());
		}

		if (termStack.isEmpty()) {
			return false;
		}

		return true;
	}
	
	/**
	 * used for testing
	 */
	public int getBufferSize(){
		return this.bufferSize;
	}
}
