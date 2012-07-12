package at.ac.univie.mminf.luceneSKOS.queryparser.flexible.standard;

/**
 * Copyright 2012 Flavio Martins 
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

import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.flexible.core.processors.QueryNodeProcessorPipeline;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.processors.AnalyzerQueryNodeProcessor;

import at.ac.univie.mminf.luceneSKOS.analysis.tokenattributes.SKOSTypeAttribute.SKOSType;
import at.ac.univie.mminf.luceneSKOS.queryparser.flexible.standard.processors.SKOSQueryNodeProcessor;

public class SKOSStandardQueryParser extends StandardQueryParser {
  
  private Map<SKOSType,Float> boosts = new HashMap<SKOSType,Float>() {
    private static final long serialVersionUID = 1L;
    {
      put(SKOSType.PREF, 0f);
      put(SKOSType.ALT, 0f);
      put(SKOSType.HIDDEN, 0f);
      put(SKOSType.BROADER, 0f);
      put(SKOSType.NARROWER, 0f);
      put(SKOSType.BROADERTRANSITIVE, 0f);
      put(SKOSType.NARROWERTRANSITIVE, 0f);
      put(SKOSType.RELATED, 0f);
    }
  };
  
  public SKOSStandardQueryParser(Analyzer analyzer) {
    super();
    QueryNodeProcessorPipeline qnpp = ((QueryNodeProcessorPipeline) getQueryNodeProcessor());
    
    int i = 0;
    for (i = 0; i < qnpp.size(); i++) {
      if (qnpp.get(i) instanceof AnalyzerQueryNodeProcessor) {
        break;
      }
    }
    SKOSQueryNodeProcessor qnp = new SKOSQueryNodeProcessor(analyzer);
    qnpp.add(i, qnp);
    
    // Set boost map
    qnp.setBoosts(boosts);
  }
  
  public void setBoosts(Map<SKOSType,Float> boosts) {
    this.boosts = boosts;
  }
  
  public Map<SKOSType,Float> getBoosts() {
    return boosts;
  }
  
  public void setBoost(SKOSType skosType, float boost) {
    boosts.put(skosType, boost);
  }
  
  public float getBoost(String type) {
    if (boosts == null) {
      return 1;
    }
    
    Float boost = boosts.get(type);
    
    if (boost != null) {
      return boost;
    }
    
    return 1;
  }
  
}
