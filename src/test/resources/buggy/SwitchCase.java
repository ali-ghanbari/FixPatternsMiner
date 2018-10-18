public class SwitchCase {
    public static void main(String[] args) {
        switch(args.length) {
            case 1:
                System.out.println("one");
                break;
            case 2:
                System.out.println("two");
                break;
            case 3:
                System.out.println("three");
                break;
            default:
                System.out.println("?");
                break;
        }
    }
}