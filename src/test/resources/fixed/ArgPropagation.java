public class ArgPropagation {
    private int sum(final int a, final int b, int c) {
        return a + b + c;
    }

    private int sum(final int a, final int b) {
        return a + b;
    }

    private int sum(final int a) {
        return a;
    }

    public static void main(String[] args) {
        System.out.printf("%d args", 0);
    }
}