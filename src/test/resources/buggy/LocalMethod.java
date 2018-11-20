public class LocalMethod {
    private int g(int a, int b) {
        return a;
    }

    public static void main(String[] args) {
        final LocalMethod lm = new LocalMethod();
        final int length = args.length;
    }

    private static int f(int a, int b) {
        return Math.max(a, b);
    }
}