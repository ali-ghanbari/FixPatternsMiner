public class ArgList {
    private int sum(final int[] arr) {
        int a = 0;
        for (int n : arr) {
            a += n;
        }
        return a;
    }

    public static void main(String[] args) {
        System.out.printf("%d args", sum(args, 1));
    }
}