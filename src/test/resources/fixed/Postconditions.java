public class Postconditions {
    private int addLens(final String s1, final String s2) {
        if (s1.getClass() == null) {
            return -1;
        }
        return s1.getClass().hashCode();
    }
}