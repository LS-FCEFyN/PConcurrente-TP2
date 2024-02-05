package utils;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import petrinet.Arc;
import petrinet.Place;
import petrinet.Transition;

public class CustomComparator implements Comparator<Object> {
    @Override
    public int compare(Object o1, Object o2) {
        // Ensure that o1 and o2 are instances of Arc, Place, or Transition
        if (!(o1 instanceof Arc || o1 instanceof Place || o1 instanceof Transition) ||
            !(o2 instanceof Arc || o2 instanceof Place || o2 instanceof Transition)) {
            throw new IllegalArgumentException("Objects must be of type Arc, Place, or Transition.");
        }

        String id1 = null;
        try {
            id1 = (String) o1.getClass().getMethod("getId").invoke(o1);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }
        String id2 = null;
        try {
            id2 = (String) o2.getClass().getMethod("getId").invoke(o2);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        }

        String[] parts1 = id1.split("(?<=\\D)(?=\\d)");
        String[] parts2 = id2.split("(?<=\\D)(?=\\d)");

        int numericComparison = Integer.compare(Integer.parseInt(parts1[1]), Integer.parseInt(parts2[1]));

        return numericComparison != 0 ? numericComparison : parts1[0].compareTo(parts2[0]);
    }
}
