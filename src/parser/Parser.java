package parser;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import petri.Arc;
import petri.ConcurrentMatrixPetriNet;
import petri.Place;
import petri.Transition;

public class Parser {

        public static ConcurrentMatrixPetriNet parse(String filePath)
                        throws ParserConfigurationException, SAXException, IOException {
                Document doc = loadAndNormalizeXml(filePath);

                List<Place> places = parsePlaces(doc).stream()
                                .sorted(Comparator.comparingInt(p -> extractNumericSuffix(p.getId())))
                                .collect(Collectors.toList());

                List<Transition> transitions = parseTransitions(doc).stream()
                                .sorted(Comparator.comparingInt(t -> extractNumericSuffix(t.getId())))
                                .collect(Collectors.toList());

                List<Arc> arcs = parseArcs(doc, places, transitions);

                Map<String, Integer> placeIndex = IntStream.range(0, places.size())
                                .boxed()
                                .collect(Collectors.toMap(i -> places.get(i).getId(), i -> i));

                Map<String, Integer> transitionIndex = IntStream.range(0, transitions.size())
                                .boxed()
                                .collect(Collectors.toMap(i -> transitions.get(i).getId(), i -> i));

                int numPlaces = places.size();
                int numTransitions = transitions.size();
                int[][] preMatrix = new int[numPlaces][numTransitions];
                int[][] postMatrix = new int[numPlaces][numTransitions];
                boolean[] timed = new boolean[numTransitions];
                int[] rates = new int[numTransitions];

                for (int i = 0; i < numTransitions; i++) {
                        Transition t = transitions.get(i);
                        timed[i] = t.isTimed();
                        rates[i] = t.getFiringRate();
                }

                arcs.forEach(arc -> {
                        int pIdx = placeIndex.get(arc.getPlace().getId());
                        int tIdx = transitionIndex.get(arc.getTransition().getId());
                        (arc.isInput() ? preMatrix : postMatrix)[pIdx][tIdx] = arc.getWeight();
                });

                return new ConcurrentMatrixPetriNet(
                                preMatrix,
                                postMatrix,
                                timed,
                                rates,
                                new AtomicIntegerArray(places.stream().mapToInt(Place::getTokens).toArray()));
        }

        private static Document loadAndNormalizeXml(String filePath)
                        throws ParserConfigurationException, SAXException, IOException {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filePath);
                doc.getDocumentElement().normalize();
                return doc;
        }

        private static List<Place> parsePlaces(Document doc) {
                return getElements(doc, "place").stream()
                                .map(e -> new Place(e.getAttribute("id"), parseInitialMarking(e)))
                                .collect(Collectors.toList());
        }

        private static int parseInitialMarking(Element placeElement) {
                return Optional.ofNullable(getChildElement(placeElement, "initialMarking"))
                                .map(e -> getChildElement(e, "value"))
                                .map(Element::getTextContent)
                                .map(v -> v.split(","))
                                .filter(v -> v.length > 1)
                                .map(v -> Integer.parseInt(v[1]))
                                .orElse(0);
        }

        private static List<Transition> parseTransitions(Document doc) {
                return getElements(doc, "transition").stream()
                                .map(e -> {
                                        Transition t = new Transition(e.getAttribute("id"));

                                        boolean isTimed = parseTimed(e);
                                        t.setIsTimed(isTimed);

                                        if (isTimed) {
                                                int rate = parseRate(e);
                                                t.setFiringRate(rate);
                                        }

                                        return t;
                                })
                                .collect(Collectors.toList());
        }

        private static boolean parseTimed(Element transitionElement) {
                return Optional.ofNullable(getChildElement(transitionElement, "timed"))
                                .map(e -> getChildElement(e, "value"))
                                .map(Element::getTextContent)
                                .map(Boolean::parseBoolean)
                                .orElse(false);
        }

        private static int parseRate(Element transitionElement) {
                return Optional.ofNullable(getChildElement(transitionElement, "rate"))
                                .map(e -> getChildElement(e, "value"))
                                .map(Element::getTextContent)
                                .map(v -> (int) Math.round(Double.parseDouble(v)))
                                .orElse(0);
        }

        private static List<Arc> parseArcs(Document doc, List<Place> places, List<Transition> transitions) {
                Map<String, Place> placeMap = places.stream()
                                .collect(Collectors.toMap(Place::getId, Function.identity()));
                Map<String, Transition> transitionMap = transitions.stream()
                                .collect(Collectors.toMap(Transition::getId, Function.identity()));

                return getElements(doc, "arc").stream()
                                .flatMap(e -> createArc(e, placeMap, transitionMap))
                                .collect(Collectors.toList());
        }

        private static Stream<Arc> createArc(Element element, Map<String, Place> places,
                        Map<String, Transition> transitions) {
                String sourceId = element.getAttribute("source");
                String targetId = element.getAttribute("target");
                int weight = parseArcWeight(element);

                Place place = places.get(sourceId);
                Transition transition = transitions.get(targetId);

                if (place != null && transition != null) {
                        return Stream.of(new Arc(element.getAttribute("id"), place, transition, true, weight));
                }

                place = places.get(targetId);
                transition = transitions.get(sourceId);

                return (place != null && transition != null)
                                ? Stream.of(new Arc(element.getAttribute("id"), place, transition, false, weight))
                                : Stream.empty();
        }

        private static int parseArcWeight(Element arcElement) {
                return Optional.ofNullable(getChildElement(arcElement, "value"))
                                .map(Element::getTextContent)
                                .map(v -> v.split(",")[1])
                                .map(Integer::parseInt)
                                .orElse(1);
        }

        private static List<Element> getElements(Document doc, String tagName) {
                NodeList nodes = doc.getElementsByTagName(tagName);
                return IntStream.range(0, nodes.getLength())
                                .mapToObj(nodes::item)
                                .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                                .map(n -> (Element) n)
                                .collect(Collectors.toList());
        }

        private static Element getChildElement(Element parent, String tagName) {
                NodeList children = parent.getElementsByTagName(tagName);
                return children.getLength() > 0 ? (Element) children.item(0) : null;
        }

        private static int extractNumericSuffix(String id) {
                return Integer.parseInt(id.substring(1));
        }
}