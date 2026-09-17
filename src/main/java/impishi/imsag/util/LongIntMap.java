package impishi.imsag.util;

import java.util.Arrays;

public final class LongIntMap {
    private static final long EMPTY = Long.MIN_VALUE;

    private long[] keys;
    private int[] values;
    private int mask;
    private int size;
    private int threshold;

    public LongIntMap() {
        this(64);
    }

    public LongIntMap(int cap) {
        int capacity = 16;
        while (capacity < cap) {
            capacity <<= 1;
        }

        keys = new long[capacity];
        values = new int[capacity];
        Arrays.fill(keys, EMPTY);
        mask = capacity - 1;
        threshold = capacity - (capacity >> 2);
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

        if (keys[i] != EMPTY) {
            values[i] = value;
            return;
        }

        keys[i] = key;
        values[i] = value;

        if (++size >= threshold) {
            resize();
        }
    }

    public void mergeMax(long key, int value) {
        int i = probe(key);

        if (keys[i] == EMPTY) {
            keys[i] = key;
            values[i] = value;

            if (++size >= threshold) {
                resize();
            }
            return;
        }

        if ((value & 0xF) > (values[i] & 0xF)) {
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
        int i = hash(key) & mask;

        while (keys[i] != EMPTY && keys[i] != key) {
            i = (i + 1) & mask;
        }

        return i;
    }

    private static int hash(long key) {
        key *= 0x9E3779B97F4A7C15L;
        key ^= key >>> 32;
        key *= 0x9E3779B97F4A7C15L;
        key ^= key >>> 32;
        return (int) key;
    }

    private void resize() {
        long[] oldKeys = keys;
        int[] oldValues = values;

        int newCapacity = oldKeys.length << 1;
        long[] newKeys = new long[newCapacity];
        int[] newValues = new int[newCapacity];
        int newMask = newCapacity - 1;

        Arrays.fill(newKeys, EMPTY);

        for (int i = 0; i < oldKeys.length; i++) {
            long key = oldKeys[i];

            if (key == EMPTY) {
                continue;
            }

            int index = hash(key) & newMask;

            while (newKeys[index] != EMPTY) {
                index = (index + 1) & newMask;
            }

            newKeys[index] = key;
            newValues[index] = oldValues[i];
        }

        keys = newKeys;
        values = newValues;
        mask = newMask;
        threshold = newCapacity - (newCapacity >> 2);
    }
}
