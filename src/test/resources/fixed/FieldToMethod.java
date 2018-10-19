public class FieldToMethod {
    private int f;

    private int getF() {
        return f;
    }

    public static void main(String[] args) {
        final FieldToMethod f2m = new FieldToMethod();
        System.out.println(f2m.f);
    }
}