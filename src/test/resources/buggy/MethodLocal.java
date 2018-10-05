public class MethodLocal {
    public static void main(String[] args) {
        final int length = args.length;
        System.out.printf("%d args%n", args.hashCode());
    }
}