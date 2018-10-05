public class FieldName {
    private int f1;
    private int f2;

    private static int F1 = 10;
    private static int F2 = 11;

    private FieldName(int a, int b) {
        this.f1 = a;
        this.f2 = b;
    }

    public static void main(String[] args) {
        final FieldName fn = new FieldName(args.length, 10);
        System.out.printf("%d args", F2);
    }
}