public class SimpleIfThen {

    private static int fact(int n) {
        return n;
    }

    public static void main(String[] args) {
        String outStr = "";
        int f = 0;
        if (args.length > 10) {
            f = fact(args.length);
            outStr = "factorial";
        }
        System.out.printf("%s of %d is %d", outStr, args.length, f);
    }
}