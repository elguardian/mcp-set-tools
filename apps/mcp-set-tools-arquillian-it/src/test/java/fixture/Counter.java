package fixture;

/** Simple fixture class instrumented by the agent under test. */
public class Counter {

    private int count = 0;

    public void increment() {
        count++;
    }

    public int getCount() {
        return count;
    }
}
