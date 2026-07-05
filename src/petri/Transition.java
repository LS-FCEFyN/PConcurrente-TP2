package petri;

/**
 * A mutable representation of a transition in a Petri net, as parsed from
 * the net's XML definition, holding its identifier, timed/immediate status,
 * and firing rate.
 */
public class Transition {
    private String id;
    private int firingRate;
    private boolean isTimed;

    /**
     * Creates a new, initially immediate transition with a firing rate of 0.
     *
     * @param id the transition's XML identifier
     */
    public Transition(String id) {
        this.id = id;
        this.firingRate = 0;
        this.isTimed = false;
    }

    /**
     * @return the transition's XML identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the transition's firing rate.
     *
     * @param firingRate the new firing rate
     */
    public void setFiringRate(int firingRate) {
        this.firingRate = firingRate;
    }

    /**
     * @return the transition's firing rate (only meaningful when the
     *         transition {@link #isTimed()})
     */
    public int getFiringRate() {
        return firingRate;
    }

    /**
     * Sets whether the transition is timed.
     *
     * @param isTimed {@code true} to mark the transition as timed,
     *                {@code false} for immediate
     */
    public void setIsTimed(boolean isTimed) {
        this.isTimed = isTimed;
    }

    /**
     * @return {@code true} if the transition is timed, {@code false} if it
     *         is immediate
     */
    public boolean isTimed() {
        return isTimed;
    }
}