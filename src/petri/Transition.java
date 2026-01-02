package petri;

public class Transition {
    private String id;
    private int firingRate;
    private boolean isTimed;

    public Transition(String id) {
        this.id = id;
        this.firingRate = 0;
        this.isTimed = false;
    }

    public String getId() {
        return id;
    }

    public void setFiringRate(int firingRate) {
        this.firingRate = firingRate;
    }

    public int getFiringRate() {
        return firingRate;
    }

    public void setIsTimed(boolean isTimed) {
        this.isTimed = isTimed;
    }

    public boolean isTimed() {
        return isTimed;
    }
}