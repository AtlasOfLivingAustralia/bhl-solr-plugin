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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
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

public class TaxonSynonymyHandler extends QueryTermEmbellisherHandler {

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        SolrParams params = req.getParams();
        SolrIndexSearcher searcher = req.getSearcher();

        String q = params.get(CommonParams.Q);

        NamedList<Double> synonyms = extractSynonyms(q, true);

        String fl = params.get(CommonParams.FL);
        int flags = 0;
        if (fl != null) {
            flags |= SolrPluginUtils.setReturnFields(fl, rsp);
        }

        int matchOffset = params.getInt(CommonParams.START, 0);
        int numberOfResults = params.getInt(CommonParams.ROWS, 1);

        Query query = QueryParsing.parseQuery(addQueryTerms(q, synonyms), params.get(CommonParams.DF), params, req.getSchema());
        DocList match = searcher.getDocList(query, null, null, matchOffset, numberOfResults, flags);

        rsp.add("match", match);
        rsp.add("synonyms", synonyms);
    }

    public NamedList<Double> extractSynonyms(String term, boolean includeCommonNames) throws Exception {

        JsonNode root = webServiceCallJson(String.format("http://bie.ala.org.au/search.json?q=%s", URLEncoder.encode(term, "utf-8")));

        JsonNode resultsNode = root.path("searchResults").path("results");
        List<String> results = new ArrayList<String>();
        if (resultsNode.isArray()) {
            for (int i = 0; i < resultsNode.size(); ++i) {
                JsonNode node = resultsNode.get(i);
                if (node.has("parentId")) {
                    String name = node.findValue("nameComplete").asText();
                    appendUniqueTokens(name, results);
                    if (node.has("guid")) {
                        String taxonGuid = node.findValue("guid").asText();
                        addLookupSynonymsFromGuid(taxonGuid, results);
                    }

                    if (includeCommonNames) {
                        String commonNames = node.findValue("commonName").asText();
                        appendUniqueTokens(commonNames, results);
                    }
                }
            }

            NamedList<Double> list = new NamedList<Double>();
            for (String result : results) {
                list.add(result, 1.0);
            }

            return list;
        }

        return null;
    }

    private void addLookupSynonymsFromGuid(String guid, List<String> tokens) throws IOException {
        if (StringUtils.isEmpty(guid)) {
            return;
        }
        JsonNode resultsNode = webServiceCallJson(String.format("http://bie.ala.org.au/species/synonymsForGuid/%s", guid));
        if (resultsNode.isArray()) {
            for (int i = 0; i < resultsNode.size(); ++i) {
                JsonNode node = resultsNode.get(i);
                appendUniqueTokens(node.findValue("name").asText(), tokens);
            }
        }

    }

    @Override
    public String getDescription() {
        return "Request Handler that includs synonyms of taxa names in the search";
    }

    @Override
    public String getVersion() {
        return "0.1";
    }

}
