package ipaddrcounter;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * This class implements a BitSet using AtomicLongArray.
 * Each long equals to 64 bits, so to represent N bits we need N/64 longs.
 */
public class AtomicBitSet {
    private final AtomicLongArray bits;
    private static final int ADDRESS_BITS_PER_LONG = 6; // 2^6 = 64
    private static final int BITS_IN_LONG = Long.SIZE;

    public AtomicBitSet(long sizeInBits) {
        int length = (int) ((sizeInBits + BITS_IN_LONG - 1) / BITS_IN_LONG);
        this.bits = new AtomicLongArray(length);
    }

    public void setBit(long bitIndex) {
        int longIndex = longArrayIndex(bitIndex);
        // bitOffset = bitIndex % 64
        int bitOffset = (int) (bitIndex & (BITS_IN_LONG - 1));
        long mask = 1L << bitOffset;
        bits.getAndUpdate(longIndex, val -> val | mask);
    }

    public long cardinality() {
        long count = 0;
        for (int i = 0; i < bits.length(); i++) {
            count += Long.bitCount(bits.get(i));
        }
        return count;
    }

    /**
     * Get index in AtomicLongArray: bitIndex / 64
     */
    private int longArrayIndex(long bitIndex) {
        return (int) (bitIndex >>> ADDRESS_BITS_PER_LONG);
    }
}
