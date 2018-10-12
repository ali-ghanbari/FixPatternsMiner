public class LocalFieldRead {
    private int i;

    public static void main(String[] args) {
        int length = args.length;
        final FieldLocalWrite flw = new FieldLocalWrite();
        flw.i = 10 * args.length;
    }
}