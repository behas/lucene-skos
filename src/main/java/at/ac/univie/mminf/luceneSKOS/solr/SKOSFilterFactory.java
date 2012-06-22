package at.ac.univie.mminf.luceneSKOS.solr;

import java.io.IOException;
import java.io.InputStream;

import org.apache.lucene.analysis.TokenStream;
import org.apache.solr.analysis.BaseTokenFilterFactory;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.util.plugin.ResourceLoaderAware;

import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer.ExpansionType;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSLabelFilter;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSURIFilter;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngineFactory;

/**
 * A factory for plugging SKOS filters into Apache Solr
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * @author Martin Kysel <martin.kysel@univie.ac.at>
 * 
 */
public class SKOSFilterFactory extends BaseTokenFilterFactory implements
    ResourceLoaderAware {
  
  private String skosFile;
  
  private ExpansionType expansionType;
  
  private SKOSEngine skosEngine;
  
  public void inform(ResourceLoader loader) {
    
    skosFile = args.get("skosFile");
    
    System.out.println("Passed argument: " + skosFile);
    
    String expansionTypeString = args.get("expansionType");
    
    if (skosFile == null || expansionTypeString == null) {
      throw new IllegalArgumentException(
          "Mandatory parameters 'skosFile=FILENAME' or 'expansionType=[URI|LABEL]' missing");
    }
    
    try {
      
      InputStream ins = loader.openResource(skosFile);
      
      if (skosFile.endsWith(".n3")) {
        skosEngine = SKOSEngineFactory.getSKOSEngine(ins, "N3");
      } else if (skosFile.endsWith(".rdf")) {
        skosEngine = SKOSEngineFactory.getSKOSEngine(ins, "RDF/XML");
      } else if (skosFile.endsWith(".ttl")) {
        skosEngine = SKOSEngineFactory.getSKOSEngine(ins, "TURTLE");
      } else {
        throw new IOException(
            "Allowed file suffixes are: .n3 (N3), .rdf (RDF/XML), .ttl (TURTLE)");
      }
      
    } catch (IOException e) {
      throw new RuntimeException("Could not instantiate SKOS engine", e);
    }
    
    if (expansionTypeString.equalsIgnoreCase(ExpansionType.URI.toString())) {
      expansionType = ExpansionType.URI;
    } else if (expansionTypeString.equalsIgnoreCase(ExpansionType.LABEL
        .toString())) {
      expansionType = ExpansionType.LABEL;
    } else {
      throw new IllegalArgumentException(
          "The property 'expansionType' must be either URI or LABEL");
    }
    
  }
  
  @Override
  public TokenStream create(TokenStream input) {
    
    if (expansionType.equals(ExpansionType.LABEL)) {
      return new SKOSLabelFilter(input, skosEngine);
      
    } else {
      return new SKOSURIFilter(input, skosEngine);
    }
    
  }
  
}
