public class MethodName {
    private static int wrong() {
        return 10;
    }

    private static int correct() {
        return 20;
    }

    public static void main(String[] args) {
        System.out.printf("20 = %d", correct());
    }
}