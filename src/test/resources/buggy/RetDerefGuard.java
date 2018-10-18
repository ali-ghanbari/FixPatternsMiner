public class RetDerefGuard {
    public static void main(String[] args) {
        String[] c = args.clone();
        System.out.println(c.length);
    }
}