package impishi.imsag.util;

import java.util.Arrays;

public final class LongIntMap {

    private static final long EMPTY = Long.MIN_VALUE;

    private long[] keys;
    private int[] values;
    private int mask;
    private int size;

    public LongIntMap() {
        this(64);
    }

    public LongIntMap(int cap) {
        int c = Integer.highestOneBit(Math.max(16, cap - 1)) << 1;
        keys = new long[c];
        values = new int[c];
        Arrays.fill(keys, EMPTY);
        mask = c - 1;
    }

    public int get(long key) {
        int i = probe(key);
        return keys[i] == EMPTY ? -1 : values[i];
    }

    public boolean contains(long key) {
        return keys[probe(key)] != EMPTY;
    }

    public void put(long key, int value) {
        int i = probe(key);
        if (keys[i] == EMPTY) {
            keys[i] = key;
            values[i] = value;
            if (++size * 4 > keys.length * 3) resize();
        } else {
            values[i] = value;
        }
    }

    public void mergeMax(long key, int value) {
        int i = probe(key);
        if (keys[i] == EMPTY) {
            keys[i] = key;
            values[i] = value;
            if (++size * 4 > keys.length * 3) resize();
        } else if ((value & 0xF) > (values[i] & 0xF)) {
            values[i] = value;
        }
    }

    public void clear() {
        Arrays.fill(keys, EMPTY);
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int capacity() {
        return keys.length;
    }

    public long keyAt(int i) {
        return keys[i];
    }

    public int valueAt(int i) {
        return values[i];
    }

    public boolean isPresent(int i) {
        return keys[i] != EMPTY;
    }

    private int probe(long key) {
        int i = mix(key) & mask;
        while (keys[i] != EMPTY && keys[i] != key) {
            i = (i + 1) & mask;
        }
        return i;
    }

    private static int mix(long key) {
        long h = key * 0x9E3779B97F4A7C15L;
        return (int) (h ^ (h >>> 32));
    }

    private void resize() {
        long[] oldKeys = keys;
        int[] oldValues = values;
        int cap = keys.length << 1;
        keys = new long[cap];
        values = new int[cap];
        Arrays.fill(keys, EMPTY);
        mask = cap - 1;
        size = 0;
        for (int i = 0; i < oldKeys.length; i++) {
            if (oldKeys[i] != EMPTY) put(oldKeys[i], oldValues[i]);
        }
    }
}
