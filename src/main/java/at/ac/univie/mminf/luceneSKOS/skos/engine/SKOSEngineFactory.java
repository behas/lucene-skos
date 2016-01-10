package at.ac.univie.mminf.luceneSKOS.skos.engine;

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

import at.ac.univie.mminf.luceneSKOS.skos.engine.jena.SKOSEngineImpl;

import java.io.IOException;
import java.io.InputStream;

/**
 * This factory instantiates the various kinds of SKOSEngine implementations
 */
public class SKOSEngineFactory {

  /**
   * Sets up a SKOS Engine from a local rdf file (serialized in any rdf
   * serialization format) or a remote rdf serialization identified by a URI
   * and reachable via HTTP.
   *
   * @param indexPath     the index path
   * @param filenameOrURI the skos file
   * @return a new SKOSEngine instance
   * @throws IOException if SKOS engine can not be instantiated
   */
  public static SKOSEngine getSKOSEngine(String indexPath, String filenameOrURI) throws IOException {
    return new SKOSEngineImpl(indexPath, filenameOrURI);
  }

  /**
   * Sets up a SKOS Engine from a given InputStream. The inputstream must
   * deliver data in a valid RDF serialization format.
   *
   * @param inputStream the input stream
   * @param lang        the serialization format (N3, RDF/XML, TURTLE)
   * @return a new SKOSEngine instance
   * @throws IOException if SKOS engine can not be instantiated
   */
  public static SKOSEngine getSKOSEngine(InputStream inputStream, String lang) throws IOException {
    return new SKOSEngineImpl(inputStream, lang);
  }

  /**
   * Sets up a SKOS Engine from a given rdf file (serialized in any RDF
   * serialization format) and considers only those concept labels that are
   * defined in the language parameter
   *
   * @param indexPath     the index path
   * @param filenameOrURI the skos file
   * @param languages     the languages to be considered
   * @return SKOSEngine
   * @throws IOException if SKOS engine can not be instantiated
   */
  public static SKOSEngine getSKOSEngine(String indexPath, String filenameOrURI, String... languages) throws IOException {
    return new SKOSEngineImpl(indexPath, filenameOrURI, languages);
  }


  /**
   * Sets up a SKOS Engine from a given InputStream. The inputstream must
   * deliver data in a valid RDF serialization format.
   *
   * @param inputStream the input stream
   * @param format      the serialization format (N3, RDF/XML, TURTLE)
   * @param languages   the languages to be considered
   * @return a new SKOSEngine instance
   * @throws IOException if SKOS engine can not be instantiated
   */
  public static SKOSEngine getSKOSEngine(InputStream inputStream, String format, String... languages) throws IOException {
    return new SKOSEngineImpl(inputStream, format, languages);
  }
}