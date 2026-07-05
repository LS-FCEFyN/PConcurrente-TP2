package monitor;

/**
 * A monitor capable of firing transitions of a Petri net under mutual
 * exclusion, optionally applying a firing policy to arbitrate conflicts.
 */
public interface MonitorInterface {
    /**
     * Attempts to fire the given transition (or, if negative, lets the
     * implementation choose one) in a thread-safe manner.
     *
     * @param transition the index of the transition to fire, or a negative
     *                   value to let the implementation select one
     * @return {@code true} if a transition was successfully fired,
     *         {@code false} otherwise
     */
    boolean fireTransition(int transition);
}