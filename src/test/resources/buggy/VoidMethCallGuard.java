public class VoidMethCallGuard {
    public void nothing() {

    }

    public static void main(String[] args) {
        final VoidMethCallGuard l = args.length == 0 ? new VoidMethCallGuard() : null;

        l.nothing();
    }
}