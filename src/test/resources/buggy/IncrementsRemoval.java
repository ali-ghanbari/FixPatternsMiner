public class IncrementsRemoval {
    private int field;

    public IncrementsRemoval id(final IncrementsRemoval ir) {
        return ir.clone();
    }

    public static void main(String[] args) {
        final IncrementsRemoval l = new IncrementsRemoval();
        int k = l.field++;
        int t = k++;
    }
}