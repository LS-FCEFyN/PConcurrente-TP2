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

/**
 * Parses a PIPE-exported Petri net XML file (PNML-like format) into a
 * {@link ConcurrentMatrixPetriNet}.
 *
 * <p>Extracts places, transitions, and arcs from the XML document, builds
 * the pre- and post-condition (incidence) matrices from the arcs, and
 * captures each transition's timed/immediate status and firing rate, as
 * well as each place's initial marking.
 */
public class Parser {

        /**
         * Parses the Petri net XML file at {@code filePath} and builds the
         * corresponding {@link ConcurrentMatrixPetriNet}.
         *
         * <p>Places and transitions are sorted by the numeric suffix of their
         * XML identifiers (e.g. {@code "p3"} sorts as {@code 3}) so that
         * matrix row/column indices correspond to a predictable, human
         * readable ordering. Arcs are then used to populate the pre- and
         * post-weight matrices, depending on whether each arc is an input
         * (place to transition) or output (transition to place) arc.
         *
         * @param filePath path to the XML file describing the Petri net
         * @return the constructed Petri net, ready for analysis and simulation
         * @throws ParserConfigurationException if a document builder cannot
         *                                       be created
         * @throws SAXException                 if the XML file is malformed
         * @throws IOException                  if the file cannot be read
         */
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

        /**
         * Loads the XML document at {@code filePath} and normalizes it
         * (merging adjacent text nodes and removing empty ones), which makes
         * subsequent element/text extraction more reliable.
         *
         * @param filePath path to the XML file to load
         * @return the parsed and normalized DOM document
         * @throws ParserConfigurationException if a document builder cannot
         *                                       be created
         * @throws SAXException                 if the XML file is malformed
         * @throws IOException                  if the file cannot be read
         */
        private static Document loadAndNormalizeXml(String filePath)
                        throws ParserConfigurationException, SAXException, IOException {
                Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(filePath);
                doc.getDocumentElement().normalize();
                return doc;
        }

        /**
         * Extracts every {@code <place>} element from the document into a
         * {@link Place} object, preserving their XML id and initial marking.
         *
         * @param doc the parsed Petri net document
         * @return the places found in the document, in document order
         */
        private static List<Place> parsePlaces(Document doc) {
                return getElements(doc, "place").stream()
                                .map(e -> new Place(e.getAttribute("id"), parseInitialMarking(e)))
                                .collect(Collectors.toList());
        }

        /**
         * Reads the initial number of tokens of a {@code <place>} element from
         * its nested {@code <initialMarking><value>} text, which is expected
         * in the PIPE-exported {@code "Default,N"} comma-separated format.
         *
         * @param placeElement the {@code <place>} XML element
         * @return the initial token count, or {@code 0} if not specified
         */
        private static int parseInitialMarking(Element placeElement) {
                return Optional.ofNullable(getChildElement(placeElement, "initialMarking"))
                                .map(e -> getChildElement(e, "value"))
                                .map(Element::getTextContent)
                                .map(v -> v.split(","))
                                .filter(v -> v.length > 1)
                                .map(v -> Integer.parseInt(v[1]))
                                .orElse(0);
        }

        /**
         * Extracts every {@code <transition>} element from the document into a
         * {@link Transition} object, including whether it is timed and, if
         * so, its firing rate.
         *
         * @param doc the parsed Petri net document
         * @return the transitions found in the document, in document order
         */
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

        /**
         * Reads whether a {@code <transition>} element is marked as timed
         * from its nested {@code <timed><value>} text.
         *
         * @param transitionElement the {@code <transition>} XML element
         * @return {@code true} if the transition is timed, {@code false}
         *         otherwise (including when not specified)
         */
        private static boolean parseTimed(Element transitionElement) {
                return Optional.ofNullable(getChildElement(transitionElement, "timed"))
                                .map(e -> getChildElement(e, "value"))
                                .map(Element::getTextContent)
                                .map(Boolean::parseBoolean)
                                .orElse(false);
        }

        /**
         * Reads a timed transition's firing rate from its nested
         * {@code <rate><value>} text, rounding to the nearest integer.
         *
         * @param transitionElement the {@code <transition>} XML element
         * @return the firing rate, or {@code 0} if not specified
         */
        private static int parseRate(Element transitionElement) {
                return Optional.ofNullable(getChildElement(transitionElement, "rate"))
                                .map(e -> getChildElement(e, "value"))
                                .map(Element::getTextContent)
                                .map(v -> (int) Math.round(Double.parseDouble(v)))
                                .orElse(0);
        }

        /**
         * Extracts every {@code <arc>} element from the document into an
         * {@link Arc} object, resolving each arc's source/target XML ids
         * against the already-parsed places and transitions.
         *
         * @param doc         the parsed Petri net document
         * @param places      the places already extracted from {@code doc}
         * @param transitions the transitions already extracted from {@code doc}
         * @return the arcs found in the document that could be resolved to a
         *         known place and transition
         */
        private static List<Arc> parseArcs(Document doc, List<Place> places, List<Transition> transitions) {
                Map<String, Place> placeMap = places.stream()
                                .collect(Collectors.toMap(Place::getId, Function.identity()));
                Map<String, Transition> transitionMap = transitions.stream()
                                .collect(Collectors.toMap(Transition::getId, Function.identity()));

                return getElements(doc, "arc").stream()
                                .flatMap(e -> createArc(e, placeMap, transitionMap))
                                .collect(Collectors.toList());
        }

        /**
         * Builds an {@link Arc} from a single {@code <arc>} XML element,
         * determining its direction by checking whether the {@code source}
         * attribute resolves to a place (input arc, place to transition) or
         * the {@code target} attribute resolves to a place (output arc,
         * transition to place).
         *
         * @param element     the {@code <arc>} XML element
         * @param places      known places, indexed by XML id
         * @param transitions known transitions, indexed by XML id
         * @return a single-element stream containing the resolved arc, or an
         *         empty stream if the arc's endpoints could not be resolved
         *         to a known place and transition
         */
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

        /**
         * Reads an arc's weight from its nested {@code <value>} text,
         * expected in the PIPE-exported {@code "Default,N"} comma-separated
         * format.
         *
         * @param arcElement the {@code <arc>} XML element
         * @return the arc's weight, or {@code 1} if not specified
         */
        private static int parseArcWeight(Element arcElement) {
                return Optional.ofNullable(getChildElement(arcElement, "value"))
                                .map(Element::getTextContent)
                                .map(v -> v.split(",")[1])
                                .map(Integer::parseInt)
                                .orElse(1);
        }

        /**
         * Returns every element in {@code doc} with the given tag name, as a
         * plain {@link List} rather than a {@link NodeList}.
         *
         * @param doc     the document to search
         * @param tagName the tag name to match
         * @return the matching elements, in document order
         */
        private static List<Element> getElements(Document doc, String tagName) {
                NodeList nodes = doc.getElementsByTagName(tagName);
                return IntStream.range(0, nodes.getLength())
                                .mapToObj(nodes::item)
                                .filter(n -> n.getNodeType() == Node.ELEMENT_NODE)
                                .map(n -> (Element) n)
                                .collect(Collectors.toList());
        }

        /**
         * Returns the first direct or nested descendant of {@code parent}
         * with the given tag name.
         *
         * @param parent  the element to search within
         * @param tagName the tag name to match
         * @return the first matching child element, or {@code null} if none
         *         exists
         */
        private static Element getChildElement(Element parent, String tagName) {
                NodeList children = parent.getElementsByTagName(tagName);
                return children.getLength() > 0 ? (Element) children.item(0) : null;
        }

        /**
         * Extracts the trailing numeric part of a PIPE-style identifier
         * (e.g. {@code "p12"} to {@code 12}), used to sort places and
         * transitions into a stable, human-readable order.
         *
         * @param id the identifier, expected to be a single letter prefix
         *           followed by digits
         * @return the numeric suffix of {@code id}
         */
        private static int extractNumericSuffix(String id) {
                return Integer.parseInt(id.substring(1));
        }
}