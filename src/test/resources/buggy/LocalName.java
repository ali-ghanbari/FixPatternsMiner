public class LocalName {
    public static void main(String[] args) {
        final int wrong = args.hashCode();
        final int correct = args.length;
        System.out.printf("%d args", wrong);
    }
}