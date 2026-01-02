package petri;

public class Place {
    private final String id;
    private final int tokens;

    public Place(String id, int tokens) {
        this.id = id;
        this.tokens = tokens;
    }

    public String getId() {
        return id;
    }

    public int getTokens() {
        return tokens;
    }
}