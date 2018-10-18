public class FieldInitRemoval {
    private int i;
    private int j;

    public FieldInitRemoval() {
        this.i = 10;
    }

    public void swap() {
        this.i = 20;
        this.j = 10;
    }
}