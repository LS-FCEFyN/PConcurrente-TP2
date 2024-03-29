package petrinet;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a Petri Net.
 *
 * <p>
 * A Petri Net is a mathematical modeling language for the description of
 * distributed systems.
 * It is characterized by Places, Transitions, and Arcs. This class provides
 * methods to interact with
 * and manipulate the Petri Net.
 * </p>
 *
 * @author Loretta
 * @since 2023-06-28
 */

public class PetriNet {
    private List<Place> places;
    private List<Transition> transitions;
    private List<Arc> arcs;

    /**
     * Constructs a PetriNet with the specified list of Places, Transitions, and
     * Arcs.
     *
     * @param places      The list of Places in the Petri Net.
     * @param transitions The list of Transitions in the Petri Net.
     * @param arcs        The list of Arcs in the Petri Net.
     */
    public PetriNet(List<Place> places, List<Transition> transitions, List<Arc> arcs) {
        this.places = places;
        this.transitions = transitions;
        this.arcs = arcs;
    }

    /**
     * Prints a textual representation of the Petri Net to the standard output.
     *
     * <p>
     * This method is primarily used for verifying that the parser correctly
     * parsed the Petri Net from a .pflow file. It prints out the details of the
     * Places, Transitions, and Arcs in the Petri Net, which can be compared with
     * the data in the .pflow file to check the accuracy of the parsing.
     * </p>
     *
     * <p>
     * The output includes the IDs and token counts for each Place, the ID for
     * each Transition, and the ID, associated Place and Transition, input status,
     * and weight for each Arc.
     * </p>
     */
    public void printPetriNetAscii() {
        System.out.println("Petri Net:");
        System.out.println("----------");

        System.out.println("Places:");
        for (Place place : places) {
            System.out.printf("ID: %s, Tokens: %d\n", place.getId(), place.getTokens());
        }

        System.out.println("\nTransitions:");
        for (Transition transition : transitions) {
            System.out.printf("ID: %s\n", transition.getId());
        }

        System.out.println("\nArcs:");
        for (Arc arc : arcs) {
            System.out.printf("ID: %s, Place: %s, Transition: %s, Is Input: %s, Weight: %d\n",
                    arc.getId(), arc.getPlace().getId(), arc.getTransition().getId(),
                    arc.isInput() ? "true" : "false", arc.getWeight());
        }
    }

    /**
     * Returns the list of Places in the Petri Net.
     *
     * @return List<Place> The list of Places in the Petri Net.
     */
    public List<Place> getPlaces() {
        return places;
    }

    /**
     * Returns the list of Transitions in the Petri Net.
     *
     * @return List<Transition> The list of Transitions in the Petri Net.
     */
    public List<Transition> getTransitions() {
        return transitions;
    }

    /**
     * Returns a list of all input Arcs.
     *
     * @return List<Arc> All input Arcs.
     */
    public List<Arc> getAllInputArcs() {
        return arcs.stream()
                .filter(Arc::isInput)
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of all output Arcs.
     *
     * @return List<Arc> All output Arcs.
     */
    public List<Arc> getAllOutputArcs() {
        return arcs.stream()
                .filter(arc -> !arc.isInput())
                .collect(Collectors.toList());
    }
}