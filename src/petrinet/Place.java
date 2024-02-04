package petrinet;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a Place in a Petri Net.
 *
 * <p>
 * A Place in a Petri Net is a location where tokens can reside. Each Place has
 * an ID and a certain number of tokens.
 * </p>
 *
 * @author Loretta
 * @since 2023-06-28
 */
public class Place {
    private String id;
    private AtomicInteger tokens;

    /**
     * Constructs a Place with the specified ID and number of tokens.
     *
     * @param id The ID of the Place.
     * @param tokens The number of tokens in the Place.
     */
    public Place(String id, int tokens) {
        this.id = id;
        this.tokens = new AtomicInteger(tokens);
    }

    /**
     * Returns the ID of the Place.
     *
     * @return String The ID of the Place.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the number of tokens in the Place.
     *
     * @return int The number of tokens in the Place.
     */
    public int getTokens() {
        return tokens.get();
    }

    /**
     * Removes an x number of tokens form the Place.
     */
    public void removeTokens(int tokensToRemove) {
        if (tokens.get() - tokensToRemove >= 0) {
            tokens.addAndGet(-tokensToRemove);
        }
    }


    /**
     * Adds an x number of tokens to the Place.
     */
    public void addTokens(int tokensToAdd) {
        tokens.addAndGet(tokensToAdd);
    }

    @Override
    public String toString() {
        return "ID: " + getId() + ", Tokens: " + getTokens();
    }

}
