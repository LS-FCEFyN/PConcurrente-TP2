package petri;

public class Arc {
    private final String id;
    private final Place place;
    private final Transition transition;
    private final boolean isInput;
    private final int weight;

    public Arc(String id, Place place, Transition transition, boolean isInput, int weight) {
        this.id = id;
        this.place = place;
        this.transition = transition;
        this.isInput = isInput;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public Place getPlace() {
        return place;
    }

    public Transition getTransition() {
        return transition;
    }

    public boolean isInput() {
        return isInput;
    }

    public int getWeight() {
        return weight;
    }
}