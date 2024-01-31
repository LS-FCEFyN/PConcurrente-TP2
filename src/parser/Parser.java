package parser;

import petrinet.Arc;
import petrinet.PetriNet;
import petrinet.Place;
import petrinet.Transition;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Parser {
    public static PetriNet parseXmlFile(String filePath)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(filePath);

        // Normalize the XML structure
        doc.getDocumentElement().normalize();

        // Initialize lists for places, transitions, and arcs
        List<Place> places = new ArrayList<>();
        List<Transition> transitions = new ArrayList<>();
        List<Arc> arcs = new ArrayList<>();

        // Parse places
        NodeList placeNodes = doc.getElementsByTagName("place");
        for (int i = 0; i < placeNodes.getLength(); i++) {
            Node node = placeNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String id = element.getAttribute("id");

                // Get the 'initialMarking' element
                Node initialMarking = element.getElementsByTagName(
                    "initialMarking").item(0);
                if (initialMarking.getNodeType() == Node.ELEMENT_NODE) {
                    Element initialMarkingElement = (Element) initialMarking;

                    // Now get the 'value' element from 'initialMarking'
                    String value = initialMarkingElement.getElementsByTagName(
                        "value").item(0).getTextContent();
                    String[] values = value.split(",");
                    int tokens;
                    if (values.length > 1) {
                        tokens = Integer.parseInt(values[1]);
                    } else {
                        tokens = 0;
                    }
                    places.add(new Place(id, tokens));
                }
            }
        }

        // Parse transitions
        NodeList transitionNodes = doc.getElementsByTagName("transition");
        for (int i = 0; i < transitionNodes.getLength(); i++) {
            Node node = transitionNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String id = element.getAttribute("id");
                transitions.add(new Transition(id));
            }
        }

        // Parse arcs
        NodeList arcNodes = doc.getElementsByTagName("arc");
        for (int i = 0; i < arcNodes.getLength(); i++) {
            Node node = arcNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                String id = element.getAttribute("id");
                String sourceId = element.getAttribute("source");
                String destinationId = element.getAttribute("target");
                String value = element.getElementsByTagName(
                    "value").item(0).getTextContent();
                String[] values = value.split(",");
                int weight = Integer.parseInt(values[1]);

                Place place = null;
                Transition transition = null;
                boolean isInput = false;

                for (Place p : places) {
                    if (p.getId().equals(sourceId) || p.getId().equals(destinationId)) {
                        place = p;
                        break;
                    }
                }

                for (Transition t : transitions) {
                    if (t.getId().equals(sourceId) || t.getId().equals(destinationId)) {
                        transition = t;
                        break;
                    }
                }

                if (place != null && transition != null) {
                    if (place.getId().equals(sourceId)) {
                        isInput = true;
                    }
                    arcs.add(new Arc(id, place, transition, isInput, weight));
                }
            }
        }
        return new PetriNet(places, transitions, arcs);
    }
}
