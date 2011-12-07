package org.ala.bhl.solr;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.RequestHandlerBase;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public abstract class QueryTermEmbellisherHandler extends RequestHandlerBase {

    @Override
    public String getSource() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSourceId() {
        // TODO Auto-generated method stub
        return null;
    }
    
    protected JsonNode webServiceCallJson(String uri) throws IOException {               
        HttpClient httpclient = new DefaultHttpClient();
        HttpGet httpget = new HttpGet(uri);
        httpget.setHeader("Accept", "application/json");
        HttpResponse response = httpclient.execute(httpget);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream instream = entity.getContent();
            @SuppressWarnings("unchecked")
            List<String> lines = IOUtils.readLines(instream);
            JsonNode root = new ObjectMapper().readValue(StringUtils.join(lines, "\n"), JsonNode.class);
            return root; 
        }        
        return null;
    }

    protected void appendUniqueTokens(String terms, List<String> tokens) {
        // First split by comma
        String[] bits = terms.toLowerCase().split(",");
        for (String bit : bits) {
            String[] innerBits = bit.split(" and | or ");
            for (String innerBit : innerBits) {
                String trimmed = innerBit.trim().toLowerCase();
                if (!tokens.contains(trimmed) && !trimmed.equals("null")) {
                    tokens.add(trimmed);
                }
            }
        }
    }
    
    
    
    protected String addQueryTerms(String orig, NamedList<Double> synonyms) {
        StringBuilder b = new StringBuilder(orig.trim());
        System.err.println(String.format("*** Original Query: '%s'", b.toString()));
        
        for (Map.Entry<String, Double> entry : synonyms) {
            b.append(" \"").append(entry.getKey()).append("\"^").append(entry.getValue());
        }
        
        System.err.println(String.format("*** Synonymized Query: '%s'", b.toString()));
        
        return b.toString();
    }
    

}
