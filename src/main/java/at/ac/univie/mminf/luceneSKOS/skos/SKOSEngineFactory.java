package at.ac.univie.mminf.luceneSKOS.skos;

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
import java.io.InputStream;

import at.ac.univie.mminf.luceneSKOS.skos.impl.SKOSEngineImpl;

/**
 * This factory instantiates the various kinds of SKOSEngine implementations
 */
public class SKOSEngineFactory {

    /**
     * Sets up a SKOS Engine from a local rdf file (serialized in any rdf serialization format) or a remote rdf serialization identified by a URI and reachable via HTTP.
     * 
     * @param filenameOrURI the skos file
     * @return a new SKOSEngine instance
     * @throws IOException
     */
    public static SKOSEngine getSKOSEngine(String filenameOrURI) throws IOException {
        return new SKOSEngineImpl(filenameOrURI);
    }

    /**
     * Sets up a SKOS Engine from a given InputStream. The inputstream must deliver data in a valid RDF serialization format.
     * 
     * @param inputStream the input stream
     * @param lang the serialization format (N3, RDF/XML, TURTLE)
     * @return a new SKOSEngine instance
     * @throws IOException
     */
    public static SKOSEngine getSKOSEngine(InputStream inputStream, String lang) throws IOException {
        return new SKOSEngineImpl(inputStream, lang);
    }

    /**
     * Sets up a SKOS Engine from a given rdf file (serialized in any rdf serialization format) and considers only those concept labels that are defined in the language parameter
     * 
     * @param filenameOrURI the skos file
     * @param languages the languages to be considered
     * @return SKOSEngine
     * @throws IOException
     */
    public static SKOSEngine getSKOSEngine(String filenameOrURI, String... languages) throws IOException {
        return new SKOSEngineImpl(filenameOrURI, languages);
    }
}