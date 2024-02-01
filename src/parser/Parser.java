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
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Parser {
    public static PetriNet parseXmlFile(String filePath)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(filePath);

        // Normalize the XML structure
        doc.getDocumentElement().normalize();

        // Parse places
        List<Place> places = IntStream.range(0, doc.getElementsByTagName("place").getLength())
        .mapToObj(i -> doc.getElementsByTagName("place").item(i))
        .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
        .map(node -> (Element) node)
        .map(element -> {
            String id = element.getAttribute("id");
            String value = ((Element) element.getElementsByTagName("initialMarking").item(0))
                    .getElementsByTagName("value").item(0).getTextContent();
            String[] values = value.split(",");
            int tokens = values.length > 1 ? Integer.parseInt(values[1]) : 0;
            return new Place(id, tokens);
        })
        .collect(Collectors.toList());

        // Parse transitions
        List<Transition> transitions = IntStream.range(0, doc.getElementsByTagName("transition").getLength())
                .mapToObj(i -> doc.getElementsByTagName("transition").item(i))
                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                .map(node -> (Element) node)
                .map(element -> {
                    String id = element.getAttribute("id");
                    return new Transition(id);
                })
                .collect(Collectors.toList());

        // Parse arcs
        List<Arc> arcs = IntStream.range(0, doc.getElementsByTagName("arc").getLength())
                .mapToObj(i -> doc.getElementsByTagName("arc").item(i))
                .filter(node -> node.getNodeType() == Node.ELEMENT_NODE)
                .map(node -> (Element) node)
                .flatMap(element -> {
                    String id = element.getAttribute("id");
                    String sourceId = element.getAttribute("source");
                    String destinationId = element.getAttribute("target");
                    String value = element.getElementsByTagName("value").item(0).getTextContent();
                    String[] values = value.split(",");
                    int weight = Integer.parseInt(values[1]);

                    Optional<Place> placeOpt = places.stream()
                            .filter(p -> p.getId().equals(sourceId) || p.getId().equals(destinationId))
                            .findAny();
                    Optional<Transition> transitionOpt = transitions.stream()
                            .filter(t -> t.getId().equals(sourceId) || t.getId().equals(destinationId))
                            .findAny();

                    if (placeOpt.isPresent() && transitionOpt.isPresent()) {
                        Place place = placeOpt.get();
                        Transition transition = transitionOpt.get();
                        boolean isInput = place.getId().equals(sourceId);
                        return Stream.of(new Arc(id, place, transition, isInput, weight));
                    } else {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());

        return new PetriNet(places, transitions, arcs);
    }
}