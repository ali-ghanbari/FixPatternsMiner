public class MethodLocal {
    private int g(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        final LocalMethod lm = new LocalMethod();
        final int length = args.length;
        System.out.printf("%d args%n", length);
    }

    private static int f(int a, int b) {
        return Math.max(a, b);
    }
}