package ipaddrcounter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


public class IPv4CounterTest {
    public static void main(String[] args) throws IOException {
        runAllTests();
    }

    public static void runAllTests() throws IOException {
        testEmptyFile();
        testAllUnique();
        testWithDuplicates();
        testAllSame();
        testEdgeValues();
        testInvalidLines();
        testMaxUnsignedIP();
        testRandomLarge(100_000_000);

        System.out.println("All tests passed");
    }

    private static void testEmptyFile() throws IOException {
        runTest(Collections.emptyList(), 0, "Empty file");
    }

    private static void testAllUnique() throws IOException {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            lines.add(iToIp(i));
        }
        runTest(lines, 1000, "All unique IPs");
    }

    private static void testWithDuplicates() throws IOException {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String ip = iToIp(i);
            lines.add(ip);
            if (i % 10 == 0) lines.add(ip); // Add some duplicates
        }
        runTest(lines, 1000, "With duplicates");
    }

    private static void testAllSame() throws IOException {
        String ip = "1.2.3.4";
        List<String> lines = Collections.nCopies(5000, ip);
        runTest(lines, 1, "All IPs same");
    }

    private static void testEdgeValues() throws IOException {
        List<String> lines = List.of(
                "0.0.0.0",        // smallest IP
                "127.255.255.255",// max in class A
                "128.0.0.0",      // min where sign bit flips
                "255.255.255.255" // largest IP (unsigned)
        );
        runTest(lines, 4, "Edge IP values");
    }

    private static void testInvalidLines() throws IOException {
        List<String> lines = List.of(
                "300.1.2.3",      // invalid part
                "abcd",           // garbage
                "1.2.3",          // too short
                "1.2.3.4.5",      // too long
                "....",           // invalid
                " ",              // empty
                "1.2.3.4",        // valid
                "1.2.3.4"         // duplicate valid
        );
        runTest(lines, 1, "Mixed valid and invalid lines");
    }

    private static void testMaxUnsignedIP() throws IOException {
        List<String> lines = List.of(
                "255.255.255.255", // max unsigned int (2^32-1)
                "128.0.0.0",       // 2^31
                "0.0.0.0"          // 0
        );
        runTest(lines, 3, "Unsigned overflow edge cases");
    }

    // === Test Helpers ===

    private static void runTest(List<String> lines, int expectedUnique, String testName) throws IOException {
        Path tempFile = Files.createTempFile("iptest_", ".txt");
        try {
            Files.write(tempFile, lines);
            testRunner(tempFile, expectedUnique, testName);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void testRandomLarge(int count) throws IOException {
        Path path = Files.createTempFile("large", ".txt");

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        BufferedWriter writer = Files.newBufferedWriter(path);

        long startIp = rand.nextLong(0, (1L << 32) - count);

        for (int i = 0; i < count; i++) {
            long ip = startIp + i;
            String ipStr = iToIp(ip);
            writer.write(ipStr);
            writer.newLine();

            if (rand.nextInt(20) == 0) {
                writer.write(ipStr);
                writer.newLine();
            }
        }

        writer.close();
        System.out.println("Finished generating");

        testRunner(path, count, "Random large input with duplicates");

        Files.deleteIfExists(path);
    }

    private static void testRunner(Path filePath, int expectedUniqueCount, String testName) {
        UniqueIPv4Counter counter = new UniqueIPv4Counter();
        BatchedFileReader reader = new BatchedFileReader(1000);
        reader.readAndExecute(filePath.toString(), counter::add);
        long result = counter.finishAndCount();

        // Debug info for large tests. Test a few specific IPs to see if conversion works
        if (expectedUniqueCount > 10000 && result != expectedUniqueCount) {
            System.err.println("DEBUG: " + testName);
            System.err.println("Expected: " + expectedUniqueCount + ", Got: " + result);
            System.err.println("Difference: " + (expectedUniqueCount - result));

            testSpecificIPs();
        }

        assertEquals(result, expectedUniqueCount, testName);
    }

    private static void testSpecificIPs() {
        System.err.println("Testing specific IP conversions:");
        long[] testIPs = {0L, 1L, 127L, 128L, (1L << 31) - 1, 1L << 31, (1L << 32) - 1};

        for (long ip : testIPs) {
            String ipStr = iToIp(ip);
            System.err.println("IP " + ip + " -> " + ipStr);

            UniqueIPv4Counter testCounter = new UniqueIPv4Counter();
            testCounter.add(List.of(ipStr));
            long count = testCounter.finishAndCount();
            System.err.println("  Counter result: " + count);
        }
    }

    private static void assertEquals(long actual, long expected, String testName) {
        if (actual != expected) {
            throw new AssertionError("Test failed: " + testName + ". Expected " + expected + ", got " + actual);
        } else {
            System.out.println("Test passed: " + testName);
        }
    }

    private static String iToIp(long ip) {
        return String.format(
                "%d.%d.%d.%d",
                (ip >> 24) & 0xFF,
                (ip >> 16) & 0xFF,
                (ip >> 8) & 0xFF,
                ip & 0xFF
        );
    }
}