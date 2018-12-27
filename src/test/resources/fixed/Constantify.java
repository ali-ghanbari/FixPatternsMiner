public class Constantify {
    public static int main(String[] args) {
        return args.length > 10 * args.hashCode() ? 10 : 9000;
    }
}