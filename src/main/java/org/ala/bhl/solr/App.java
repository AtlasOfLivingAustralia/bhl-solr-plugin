package org.ala.bhl.solr;

public class App {

    public static void main(String[] args) throws Exception {
        NearestNamedPlacesHandler h = new NearestNamedPlacesHandler();
        // h.extractNearestNamedPlaces("forest hill");
        h.extractNearestNamedPlaces("-36.80070114,148.2006989");
    }

}
