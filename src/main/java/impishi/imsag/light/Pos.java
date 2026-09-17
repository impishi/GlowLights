package impishi.imsag.light;

public final class Pos {

    private Pos() {
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    public static int x(long key) {
        int v = (int) ((key >> 38) & 0x3FFFFFF);
        return (v & 0x2000000) != 0 ? v | ~0x3FFFFFF : v;
    }

    public static int y(long key) {
        int v = (int) ((key >> 26) & 0xFFF);
        return (v & 0x800) != 0 ? v | ~0xFFF : v;
    }

    public static int z(long key) {
        int v = (int) (key & 0x3FFFFFF);
        return (v & 0x2000000) != 0 ? v | ~0x3FFFFFF : v;
    }
}
