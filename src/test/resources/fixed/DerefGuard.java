public class DerefGuard {
    public static void main(String[] args) {
        System.out.println(args == null ? 0 : Math.abs(args.length + 19));
    }
}