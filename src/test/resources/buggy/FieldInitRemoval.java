public class FieldInitRemoval {
    private int i;
    private int j;

    public FieldInitRemoval() {
        this.i = 10;
        this.j = 20;
    }

    public void swap() {
        this.i = 20;
        this.j = 10;
    }
}