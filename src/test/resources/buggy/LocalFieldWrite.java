public class LocalFieldWrite {
    private int i;

    public static void main(String[] args) {
        int length = args.length;
        final FieldLocalWrite flw = new FieldLocalWrite();
        length = 10 * args.length;
    }
}