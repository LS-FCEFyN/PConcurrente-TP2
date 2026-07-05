package petri;

/**
 * An immutable representation of a place in a Petri net, as parsed from the
 * net's XML definition, holding its identifier and initial token count.
 */
public class Place {
    private final String id;
    private final int tokens;

    /**
     * Creates a new place.
     *
     * @param id     the place's XML identifier
     * @param tokens the place's initial number of tokens
     */
    public Place(String id, int tokens) {
        this.id = id;
        this.tokens = tokens;
    }

    /**
     * @return the place's XML identifier
     */
    public String getId() {
        return id;
    }

    /**
     * @return the place's initial number of tokens
     */
    public int getTokens() {
        return tokens;
    }
}