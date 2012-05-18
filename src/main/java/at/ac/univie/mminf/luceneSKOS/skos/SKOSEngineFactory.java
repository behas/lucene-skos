package at.ac.univie.mminf.luceneSKOS.skos;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.util.Version;

import at.ac.univie.mminf.luceneSKOS.skos.impl.SKOSEngineImpl;

/**
 * This factory instantiates the various kinds of SKOSEngine implementations
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 */
public class SKOSEngineFactory {
  
  /**
   * Sets up a SKOS Engine from a local rdf file (serialized in any rdf
   * serialization format) or a remote rdf serialization identified by a URI and
   * reachable via HTTP.
   * 
   * @param filenameOrURI
   *          the skos file
   * @return a new SKOSEngine instance
   * @throws IOException
   */
  public static SKOSEngine getSKOSEngine(final Version version,
      String filenameOrURI) throws IOException {
    return new SKOSEngineImpl(version, filenameOrURI);
  }
  
  /**
   * Sets up a SKOS Engine from a given InputStream. The inputstream must
   * deliver data in a valid RDF serialization format.
   * 
   * @param inputStream
   *          the input stream
   * @param lang
   *          the serialization format (N3, RDF/XML, TURTLE)
   * @return a new SKOSEngine instance
   * @throws IOException
   */
  public static SKOSEngine getSKOSEngine(final Version version,
      InputStream inputStream, String lang) throws IOException {
    return new SKOSEngineImpl(version, inputStream, lang);
  }
  
  /**
   * Sets up a SKOS Engine from a given rdf file (serialized in any rdf
   * serialization format) and considers only those concept labels that are
   * defined in the language parameter
   * 
   * @param filenameOrURI
   *          the skos file
   * @param languages
   *          the languages to be considered
   * @return SKOSEngine
   * @throws IOException
   */
  public static SKOSEngine getSKOSEngine(final Version version,
      String filenameOrURI, String... languages) throws IOException {
    return new SKOSEngineImpl(version, filenameOrURI, languages);
  }
}