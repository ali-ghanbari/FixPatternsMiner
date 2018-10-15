public class CaseBreakerRet {
    public static void main(String[] args) {
        switch (args.length) {
            case 0:
                System.out.println("no args");
                break;
            case 1:
                System.out.println("one args");
                return args;
            case 2:
                System.out.println("two args");
                break;
            case 3:
                System.out.println("three args");
                break;
            default:
                System.out.println("four or more args");
        }
    }
}