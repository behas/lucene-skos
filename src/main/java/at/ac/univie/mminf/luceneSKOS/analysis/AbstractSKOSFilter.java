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
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;

import org.apache.lucene.util.CharsRefBuilder;
import at.ac.univie.mminf.luceneSKOS.analysis.engine.SKOSEngine;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSTypeAttribute.SKOSType;

/**
 * Basic methods for SKOS-specific TokenFilter implementations
 */
public abstract class AbstractSKOSFilter extends TokenFilter {

    // a stack holding the expanded terms for a token
    protected Stack<ExpandedTerm> termStack;
    // an engine delivering SKOS concepts
    protected SKOSEngine engine;
    // the skos types to expand to
    protected Set<SKOSType> types;
    // provides access to the the term attributes
    protected AttributeSource.State current;
    // the term text (propagated to the index)
    protected final CharTermAttribute termAtt;
    // the token position relative to the previous token (propagated)
    protected final OffsetAttribute offsettAtt;
    // the token position relative to the previous token (propagated)

    protected final PositionIncrementAttribute posIncrAtt;
    // the binary payload attached to the indexed term (propagated to the index)
    protected final PayloadAttribute payloadAtt;
    // the SKOS-specific attribute attached to a term
    protected final SKOSTypeAttribute skosAtt;
    // the analyzer to use when parsing
    protected final Analyzer analyzer;

    private List<SKOSTypeAttribute.SKOSType> defaultTypes = Arrays.asList(SKOSAnalyzer.DEFAULT_SKOS_TYPES);

    /**
     * Constructor
     *
     * @param input the TokenStream
     * @param engine the engine delivering skos concepts
     * @param analyzer the analyzer
     * @param types the skos types to expand to
     */
    public AbstractSKOSFilter(TokenStream input, SKOSEngine engine, Analyzer analyzer, List<SKOSType> types) {
        super(input);
        termStack = new Stack<>();
        this.engine = engine;
        this.analyzer = analyzer;
        this.types = new TreeSet<>(types != null && !types.isEmpty() ? types : defaultTypes);
        this.termAtt = addAttribute(CharTermAttribute.class);
        this.posIncrAtt = addAttribute(PositionIncrementAttribute.class);
        this.payloadAtt = addAttribute(PayloadAttribute.class);
        this.skosAtt = addAttribute(SKOSTypeAttribute.class);
        this.offsettAtt = addAttribute(OffsetAttribute.class);
    }

    /**
     * Advances the stream to the next token.
     *
     * To be implemented by the concrete sub-classes
     */
    @Override
    public abstract boolean incrementToken() throws IOException;

    /**
     * Replaces the current term (attributes) with term (attributes) from the stack
     *
     * @throws IOException if analyzer failed
     */
    protected void processTermOnStack() throws IOException {
        ExpandedTerm expandedTerm = termStack.pop();
        String term = expandedTerm.getTerm();
        SKOSType termType = expandedTerm.getTermType();
        String sTerm;
        try {
            CharsRefBuilder builder = new CharsRefBuilder();
            sTerm = analyze(analyzer, term, builder).toString();
        } catch (IllegalArgumentException e) {
            // skip this term
            return;
        }
        // copies the values of all attribute implementations from this state into
        // the implementations of the target stream
        restoreState(current);
        // adds the expanded term to the term buffer
        termAtt.setEmpty().append(sTerm);
        // set position increment to zero to put multiple terms into the same position
        posIncrAtt.setPositionIncrement(0);
        // set offset of the original expression (usefull for highlighting)
        if (expandedTerm.getStart() >= 0 && expandedTerm.getEnd() >= 0)
            offsettAtt.setOffset(expandedTerm.getStart(), expandedTerm.getEnd());
        // sets the type of the expanded term (pref, alt, broader, narrower, etc.)
        skosAtt.setSkosType(termType);
        // converts the SKOS Attribute to a payload, which is propagated to the index
        byte[] bytes = PayloadHelper.encodeInt(skosAtt.getSkosType().ordinal());
        payloadAtt.setPayload(new BytesRef(bytes));
    }

    public static CharsRef analyze(Analyzer analyzer, String text, CharsRefBuilder buffer)
            throws IOException {
        TokenStream ts = analyzer.tokenStream("", new StringReader(text));
        CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        while (ts.incrementToken()) {
            int length = termAtt.length();
            if (length == 0) {
                throw new IllegalArgumentException("term: " + text + " analyzed to a zero-length token");
            }
            if (buffer.length() > 0) {
                buffer.append(' ');
            }
            buffer.append(termAtt.buffer(), 0, length);
        }
        ts.end();
        ts.close();
        if (buffer.length() == 0) {
            throw new IllegalArgumentException("term: " + text + " was completely eliminated by analyzer");
        }
        return buffer.get();
    }

    protected void pushLabelsToStack(ExpandedTerm origin, Collection<String> labels, SKOSType type) {
        if (labels != null) {
            for (String label : labels) {
                termStack.push(new ExpandedTerm(label, type, origin.getStart(), origin.getEnd()));
            }
        }
    }

    /**
     * Helper class for capturing terms and term types
     */
    protected static class ExpandedTerm {

        private final String term;
        private final SKOSType termType;

        private final int start;
        private final int end;

        protected ExpandedTerm(String term, SKOSType termType) {
            this.term = term;
            this.termType = termType;
            this.start = -1;
            this.end = -1;

        }

        protected ExpandedTerm(String term, SKOSType termType, int start, int end) {
            this.term = term;
            this.termType = termType;
            this.start = start;
            this.end = end;

        }

        protected String getTerm() {
            return this.term;
        }

        protected SKOSType getTermType() {
            return this.termType;
        }

        protected int getStart() {
            return start;
        }

        protected int getEnd() {
            return end;
        }

    }
}
