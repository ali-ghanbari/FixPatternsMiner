public class SimpleMethCallRemoval {
    public void nothing(final Object o1, final Object o2) {

    }

    public static void main(String[] args) {
        final SimpleMethCallRemoval l = new SimpleMethCallRemoval();
        l.nothing(new SimpleMethCallRemoval(), SimpleMethCallRemoval.class.hashCode());
    }
}