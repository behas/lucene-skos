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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;

import at.ac.univie.mminf.luceneSKOS.analysis.engine.SKOSEngine;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSTypeAttribute.SKOSType;

/**
 * A Lucene TokenFilter that supports label-based term expansion as described in
 * https://code.google.com/p/lucene-skos/wiki/UseCases#UC2:_Label-based_term_expansion.
 *
 * It takes labels (String values) as input and searches a given SKOS vocabulary
 * for matching concepts (based on their prefLabels). If a match is found, it
 * adds the concept's labels to the output token stream.
 */
public final class SKOSLabelFilter extends AbstractSKOSFilter {

    public static final int DEFAULT_BUFFER_SIZE = 1;
    /* the size of the buffer used for multi-term prediction */
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    /* a list serving as token buffer between consumed and consuming stream */
    private Queue<State> buffer = new LinkedList<>();

    /**
     * Constructor for multi-term expansion support. Takes an input token
     * stream, the SKOS engine, and an integer indicating the maximum token
     * length of the preferred labels in the SKOS vocabulary.
     *
     * @param input the consumed token stream
     * @param engine the skos expansion engine
     * @param analyzer the analyzer
     * @param bufferSize the length of the longest pref-label to consider
     * (needed for mult-term expansion)
     * @param types the skos types to expand to
     */
    public SKOSLabelFilter(TokenStream input, SKOSEngine engine,
            Analyzer analyzer, int bufferSize, List<SKOSType> types) {
        super(input, engine, analyzer, types);
        this.bufferSize = bufferSize;
    }

    /**
     * Advances the stream to the next token
     */
    @Override
    public boolean incrementToken() throws IOException {
        // there are expanded terms for the given token
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
        // check whether there are expanded terms for a given token
        if (addAliasesToStack()) {
            // if yes, capture the state of all attributes
            current = captureState();
        }
        buffer.remove();
        return true;
    }

    private boolean addAliasesToStack() throws IOException {
        for (int i = buffer.size(); i > 0; i--) {
            ExpandedTerm inputTokens = bufferToTerm(i);
            if (addTermsToStack(inputTokens)) {
                break;
            }
        }
        return !termStack.isEmpty();
    }

    /**
     * Converts the first x=noTokens states in the queue to a concatenated token
     * string separated by white spaces
     * @param noTokens the number of tokens
     * @return the concatenated token string
     */
    private ExpandedTerm bufferToTerm(int noTokens) {
        State entered = captureState();
        State[] bufferedStates = buffer.toArray(new State[buffer.size()]);
        StringBuilder builder = new StringBuilder();
        builder.append(termAtt.toString());
        restoreState(bufferedStates[0]);
        int start = offsettAtt.startOffset();
        for (int i = 1; i < noTokens; i++) {
            restoreState(bufferedStates[i]);
            builder.append(" ").append(termAtt.toString());
        }
        int end = offsettAtt.endOffset();
        restoreState(entered);
        return new ExpandedTerm(builder.toString(), null, start, end);
    }

    /**
     * Add terms to stack
     * Assumes that the given term is a textual token
     *
     * @param term the given term
     * @return true if term stack is not empty
     */
    public boolean addTermsToStack(ExpandedTerm term) throws IOException {
        Collection<String> conceptURIs = engine.getConcepts(term.getTerm());
        for (String conceptURI : conceptURIs) {
            if (types.contains(SKOSType.PREF)) {
                pushLabelsToStack(term, engine.getPrefLabels(conceptURI), SKOSType.PREF);
            }
            if (types.contains(SKOSType.ALT)) {
                pushLabelsToStack(term, engine.getAltLabels(conceptURI), SKOSType.ALT);
            }
            if (types.contains(SKOSType.HIDDEN)) {
                pushLabelsToStack(term, engine.getHiddenLabels(conceptURI), SKOSType.HIDDEN);
            }
            if (types.contains(SKOSType.BROADER)) {
                pushLabelsToStack(term, engine.getBroaderLabels(conceptURI), SKOSType.BROADER);
            }
            if (types.contains(SKOSType.BROADERTRANSITIVE)) {
                pushLabelsToStack(term, engine.getBroaderTransitiveLabels(conceptURI), SKOSType.BROADERTRANSITIVE);
            }
            if (types.contains(SKOSType.NARROWER)) {
                pushLabelsToStack(term, engine.getNarrowerLabels(conceptURI), SKOSType.NARROWER);
            }
            if (types.contains(SKOSType.NARROWERTRANSITIVE)) {
                pushLabelsToStack(term, engine.getNarrowerTransitiveLabels(conceptURI), SKOSType.NARROWERTRANSITIVE);
            }
            if (types.contains(SKOSType.RELATED)) {
                pushLabelsToStack(term, engine.getRelatedLabels(conceptURI), SKOSType.RELATED);
            }

        }
        return !termStack.isEmpty();
    }

}
