package org.ala.bhl.solr;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
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
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class SynonymyHelper {

	public static String embellishQueryWithSynonyms(String q) {
		try {

			if (q.startsWith("\"") && q.endsWith("\"")) {
				q = q.substring(1, q.length() - 2);
			}

			NamedList<Double> synonyms = extractSynonyms(q, true);
			q = addQueryTerms(q, synonyms);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return q;
	}

	public static NamedList<Double> extractSynonyms(String term, boolean includeCommonNames) throws Exception {

		String uri = String.format("http://bie.ala.org.au/search.json?q=%s", URLEncoder.encode(term, "utf-8"));

		System.err.println("Finding taxa synonyms: " + uri);

		JsonNode root = webServiceCallJson(uri);

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

	public static void addLookupSynonymsFromGuid(String guid, List<String> tokens) throws IOException {
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

	public static JsonNode webServiceCallJson(String uri) throws IOException {
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

	public static void appendUniqueTokens(String terms, List<String> tokens) {
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

	public static String addQueryTerms(String orig, NamedList<Double> synonyms) {
		StringBuilder b = new StringBuilder(orig.trim());
		for (Map.Entry<String, Double> entry : synonyms) {
			b.append(" \"").append(entry.getKey()).append("\"^").append(entry.getValue());
		}

		System.err.println("addQueryTerms: " + b.toString());
		return b.toString();
	}

}
