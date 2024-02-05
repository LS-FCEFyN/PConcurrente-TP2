package utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import petrinet.Arc;
import petrinet.Place;
import petrinet.Transition;

/**
 * Custom comparator for objects that are instances of Arc, Place, or Transition. This comparator
 * ensures that input arcs come before output arcs and compares other objects based on their ID
 * parts: alphanumeric and numeric.
 *
 * @author Loretta
 * @since 2024-02-05
 */
public class CustomComparator implements Comparator<Object> {

    /**
     * Compares two objects based on their type and ID.
     *
     * @param o1 the first object to compare
     * @param o2 the second object to compare
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     *         equal to, or greater than the second
     * @throws IllegalArgumentException if either of the objects is not an instance of Arc, Place,
     *         or Transition
     */
    @Override
    public int compare(Object o1, Object o2) {
        // Ensure that o1 and o2 are instances of Arc, Place, or Transition
        if (!(o1 instanceof Arc || o1 instanceof Place || o1 instanceof Transition)
                || !(o2 instanceof Arc || o2 instanceof Place || o2 instanceof Transition)) {
            throw new IllegalArgumentException(
                    "Objects must be of type Arc, Place, or Transition.");
        }

        if (o1 instanceof Arc && o2 instanceof Arc) {
            return compareArcs((Arc) o1, (Arc) o2);
        } else {
            String id1 = getId(o1);
            String id2 = getId(o2);

            String[] parts1 = id1.split("(?<=\\D)(?=\\d)");
            String[] parts2 = id2.split("(?<=\\D)(?=\\d)");

            int numericComparison =
                    Integer.compare(Integer.parseInt(parts1[1]), Integer.parseInt(parts2[1]));

            return numericComparison != 0 ? numericComparison : parts1[0].compareTo(parts2[0]);
        }
    }

    /**
     * Retrieves the ID of an object by invoking its getId method via reflection.
     *
     * @param obj the object whose ID is to be retrieved
     * @return the ID of the object or null if an exception occurs during reflection
     */
    private String getId(Object obj) {
        try {
            return (String) obj.getClass().getMethod("getId").invoke(obj);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Compares two Arcs based on whether they are input or output arcs and then by comparing their
     * associated place and transition IDs.
     *
     * @param arc1 the first Arc to compare
     * @param arc2 the second Arc to compare
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     *         equal to, or greater than the second
     */
    private int compareArcs(Arc arc1, Arc arc2) {
        // Make input arcs come before output arcs
        int inputComparison = Boolean.compare(!arc1.isInput(), !arc2.isInput());
        if (inputComparison != 0) {
            return inputComparison;
        }

        // If both arcs are input or output, compare them based on place and transition IDs
        String placeId1 = arc1.getPlace().getId();
        String transitionId1 = arc1.getTransition().getId();

        String placeId2 = arc2.getPlace().getId();
        String transitionId2 = arc2.getTransition().getId();

        // Extract numeric part of IDs and parse as integers
        int numPlace1 = Integer.parseInt(placeId1.replaceAll("\\D+", ""));
        int numPlace2 = Integer.parseInt(placeId2.replaceAll("\\D+", ""));

        int numTransition1 = Integer.parseInt(transitionId1.replaceAll("\\D+", ""));
        int numTransition2 = Integer.parseInt(transitionId2.replaceAll("\\D+", ""));

        // First compare places, then transitions if places are equal
        if (numPlace1 == numPlace2) {
            return Integer.compare(numTransition1, numTransition2);
        } else {
            return Integer.compare(numPlace1, numPlace2);
        }
    }
}