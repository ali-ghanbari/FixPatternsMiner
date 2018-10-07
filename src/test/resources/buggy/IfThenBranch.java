public class IfThenBranch {

    private static int fibo(int n) {
        return n;
    }

    private static int fact(int n) {
        return n;
    }

    public static void main(String[] args) {
        final String outStr;
        int f;
        if (args.length > 10) {
            f = fact(args.length);
            outStr = "factorial";
        } else {
            f = fibo(args.length);
            outStr = "fibonacci";
        }
        System.out.printf("%s of %d is %d", outStr, args.length, f);
    }
}