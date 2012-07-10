package at.ac.univie.mminf.luceneSKOS.util;

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

import jena.schemagen;

/**
 * Uses the JENA schema generator to convert the used OWL ontologies/vocabs to
 * corresponding Java classes.
 */
public class SchemaGenerator {
  
  /*
   * Commonly used options include:
   *    -i <input> the source document as a file or URL.
   *    -n <name> the name of the created Java class.
   *    -a <uri> the namespace URI of the source document.
   *    -o <file> the file to write the generated class into.
   *    -o <dir> the directory in which the generated Java class is created.
   *    By default, output goes to stdout.
   *    -e <encoding> the encoding of the input document (N3, RDF/XML, etc).
   *    -c <config> a filename or URL for an RDF document containing configuration parameters.
   *    
   *    see http://jena.sourceforge.net/how-to/schemagen.html
   */
  public static void main(String[] args) {
    // SKOS Vocabulary
    args = new String[] { 
        "-i", "src/main/resources/skos.rdf"
      , "--package", "at.ac.univie.mminf.luceneSKOS.skos"
      , "-n", "SKOS"
      , "--ontology", "true"
      , "--owl", "true"
      , "-e", "RDF/XML"
      , "--inference", "true"
      , "-o", "src/main/java/at/ac/univie/mminf/luceneSKOS/skos" // note: for some reason uris are not allowed here...
    };
    System.out.println("publish SKOS vocabulary ...");
    schemagen.main(args);
    
    System.out.println("vocabularies published.");
  }
  
}
