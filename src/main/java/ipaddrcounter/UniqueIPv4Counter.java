package ipaddrcounter;

import ipaddrcounter.exception.UniqueIPV4CounterException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

/**
 * IPv4 addresses are represented as 32-bit integers. So, the total number of possible IPv4 addresses is 2^32.
 * The required max size of the BitSet to store all the unique IPv4 addresses is (2^32/8) bytes = 512 MB.
 */
public class UniqueIPv4Counter {
    // Either custom Backpressure ThreadPool (e.g., with CallerRunsPolicy and bounded pool) to avoid OOM
    // or workStealingPool to use threads efficiently
    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final Phaser activeTaskCounter = new Phaser(1);
    private static final long TOTAL_NUM_IPV4 = 1L << 32;
    private final AtomicBitSet bitSet = new AtomicBitSet(TOTAL_NUM_IPV4);

    public void add(List<String> ipsBatch) {
        activeTaskCounter.register();
        executorService.submit(() -> {
            try {
                ipsBatch.forEach(this::processIp);
            } finally {
                activeTaskCounter.arriveAndDeregister();
            }
        });
    }

    private void processIp(String ip) {
        try {
            long index = convertToBitSetIndex(ip.trim());
            bitSet.setBit(index);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid IP: " + e.getMessage());
        }
    }


    public long finishAndCount() {
        activeTaskCounter.arriveAndAwaitAdvance();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Warning: Executor did not terminate within timeout");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            throw new UniqueIPV4CounterException("Executor interrupted", e);
        }

        return bitSet.cardinality();
    }

    public long convertToBitSetIndex(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IP format, expected 4 octets: " + ip);
        }

        long a = parseAndValidateOctet(parts[0], ip);
        long b = parseAndValidateOctet(parts[1], ip);
        long c = parseAndValidateOctet(parts[2], ip);
        long d = parseAndValidateOctet(parts[3], ip);

        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    private int parseAndValidateOctet(String octetStr, String fullIp) {
        try {
            int octet = Integer.parseInt(octetStr);
            if (octet < 0 || octet > 255) {
                throw new IllegalArgumentException("Invalid IP octet range: " + fullIp);
            }
            return octet;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid IP octet format: " + fullIp);
        }
    }
}
