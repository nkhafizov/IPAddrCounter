package ipaddrcounter;

public class Main {
    public static void main(String[] args) {
        var uniqueIPv4Counter = new UniqueIPv4Counter();
        if (args.length == 0) {
            System.err.println("Usage: java Main <path-to-ip-file>");
            System.exit(1);
        }
        String filePath = args[0];
        var fileReader = new BatchedFileReader();
        fileReader.readAndExecute(filePath, uniqueIPv4Counter::add);
        System.out.println(uniqueIPv4Counter.finishAndCount());
    }
}