public class RetDerefGuard {
    public static void main(String[] args) {
        String[] c = args.clone();
        if (c == null) {
            return;
        }
        System.out.println(c.length);
    }
}