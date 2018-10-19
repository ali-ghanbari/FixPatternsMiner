public class Preconditions {
    private int addLens(final String s1, final String s2) {
        if (s1 == null || s2 == null) {
            return 0;
        }
        return s1.length() + s2.length();
    }
}