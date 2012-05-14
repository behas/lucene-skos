package at.ac.univie.mminf.luceneSKOS.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.util.Version;
import org.apache.solr.core.SolrResourceLoader;

import at.ac.univie.mminf.luceneSKOS.analysis.SKOSAnalyzer.ExpansionType;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSLabelFilter;
import at.ac.univie.mminf.luceneSKOS.analysis.SKOSURIFilter;
import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngine;
import at.ac.univie.mminf.luceneSKOS.skos.SKOSEngineFactory;

/**
 * A factory for plugging SKOS filters into Apache Solr
 * 
 * @author Bernhard Haslhofer <bernhard.haslhofer@univie.ac.at>
 * @author Martin Kysel <martin.kysel@univie.ac.at>
 * 
 */
public class SKOSFilterFactory extends TokenFilterFactory implements
    ResourceLoaderAware {
  
  private String skosFile;
  
  private ExpansionType expansionType;
  
  private int bufferSize;
  
  private SKOSType[] type;
  
  private SKOSEngine skosEngine;
  
  @Override
  public void inform(ResourceLoader loader) {
    SolrResourceLoader solrLoader = (SolrResourceLoader) loader;
    
    skosFile = args.get("skosFile");
    
    String expansionTypeString = args.get("expansionType");
    
    String bufferSizeString = args.get("bufferSize");
    
    String languageString = args.get("language");
    
    String typeString = args.get("type");
    
    System.out.println("Passed argument: " + skosFile + " Type: "
        + expansionTypeString + " bufferSize: "
        + (bufferSizeString != null ? bufferSizeString : "Default")
        + " language: " + (languageString != null ? languageString : "All")
        + " type: " + (typeString != null ? typeString : "Default"));
    
    if (skosFile == null || expansionTypeString == null) throw new IllegalArgumentException(
        "Mandatory parameters 'skosFile=FILENAME' or 'expansionType=[URI|LABEL]' missing");
    
    try {
      if (skosFile.endsWith(".n3") || skosFile.endsWith(".rdf")
          || skosFile.endsWith(".ttl")) skosEngine = SKOSEngineFactory
          .getSKOSEngine(solrLoader.getConfigDir() + skosFile,
              languageString != null ? languageString.split(" ") : null);
      else throw new IOException(
          "Allowed file suffixes are: .n3 (N3), .rdf (RDF/XML), .ttl (TURTLE)");
      
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
    
    if (bufferSizeString != null) {
      int bs = Integer.parseInt(bufferSizeString);
      if (bs > 0) bufferSize = bs;
      else throw new IllegalArgumentException(
          "The property 'bufferSize' must be a positive (smallish) integer");
    }
    
    if (typeString != null) {
      List<SKOSType> sts = new ArrayList<SKOSType>();
      for (String s : typeString.split(" ")) {
        SKOSType st = SKOSType.valueOf(s.toUpperCase());
        if (st != null) sts.add(st);
      }
      type = sts.toArray(new SKOSType[0]);
    }
  }
  
  @Override
  public TokenStream create(TokenStream input) {
    
    if (expansionType.equals(ExpansionType.LABEL)) {
      return new SKOSLabelFilter(input, skosEngine, new StandardAnalyzer(
          Version.LUCENE_40), bufferSize, type);
      
    } else {
      return new SKOSURIFilter(input, skosEngine, new StandardAnalyzer(
          Version.LUCENE_40));
    }
    
  }
}
