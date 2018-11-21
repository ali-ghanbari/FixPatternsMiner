public class SimpleMethCallGuard {
    public void nothing() {

    }

    public static void main(String[] args) {
        final SimpleMethCallGuard l = args.length == 0 ? new SimpleMethCallGuard() : null;

        l.nothing();
    }
}