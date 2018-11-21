public class VoidMethCallRemoval {
    public void nothing(final Object o1, final Object o2) {
        return null;
    }

    public static void main(String[] args) {
        final VoidMethCallRemoval l = new VoidMethCallRemoval();
    }
}