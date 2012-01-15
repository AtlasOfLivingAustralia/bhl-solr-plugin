/*******************************************************************************
 * Copyright (C) 2011 Atlas of Living Australia
 * All Rights Reserved.
 *   
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *   
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ******************************************************************************/
package org.ala.bhl.solr;

import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocList;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.SolrPluginUtils;
import org.codehaus.jackson.JsonNode;

public class NearestNamedPlacesHandler extends QueryTermEmbellisherHandler {
    
    private Pattern _latLongMatcher = Pattern.compile("^\\s*(-?\\d+[.]\\d+)\\s*,\\s*(\\d+[.]\\d+)\\s*$");
    
    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        // TODO Auto-generated method stub
        SolrParams params = req.getParams();
        SolrIndexSearcher searcher = req.getSearcher();
       
        String q = params.get(CommonParams.Q);
        
        NamedList<Double> synonyms = extractNearestNamedPlaces(q);
        
        String fl = params.get(CommonParams.FL);
        int flags = 0;
        if (fl != null) {
            flags |= SolrPluginUtils.setReturnFields(fl, rsp);
        }

        int matchOffset = params.getInt(CommonParams.START, 0);
        int numberOfResults = params.getInt(CommonParams.ROWS, 1);
        
        Query query = QueryParsing.parseQuery(addQueryTerms(q,  synonyms), params.get(CommonParams.DF), params, req.getSchema());
        DocList match = searcher.getDocList(query, null, null, matchOffset, numberOfResults, flags);

        rsp.add("match", match);
        rsp.add("synonyms", synonyms);

    }

    @Override
    public String getDescription() {
        return "A request handler that injects the nearest named places to either a point, or other named place";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }
    
    public NamedList<Double> extractNearestNamedPlaces(String q) throws Exception {
        
        Matcher m = _latLongMatcher.matcher(q);
        if (m.find()) {
            double lat = Double.parseDouble(m.group(1));
            double lon = Double.parseDouble(m.group(2));
            
            System.err.println(String.format("Lat/long: %f, %f", lat, lon));
        } else {        
          String url = String.format("http://spatial-dev.ala.org.au/layers-service/search?q=%s", URLEncoder.encode(q, "utf-8"));        
          JsonNode root = webServiceCallJson(url);
          
          if (root.isArray()) {
              for (int i = 0; i < root.size(); ++i) {
                  JsonNode node = root.get(i);
                  System.err.println(node);
              }
          }
        }
        
        
        
        NamedList<Double> results = new NamedList<Double>();
        results.add("queanbeyan", 1.0);
        return results;
    }

}
