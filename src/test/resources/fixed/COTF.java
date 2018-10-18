public class FieldInitRemoval {
    public static void main(String[] args) {
        final int l = args.length;
        boolean b = l > 0 && !(args.hashCode() > 1);
        if (!b) {
            System.out.println("ddd");
        }
    }
}