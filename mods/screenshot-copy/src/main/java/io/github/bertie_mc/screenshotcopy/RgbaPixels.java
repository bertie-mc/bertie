package io.github.bertie_mc.screenshotcopy;

final class RgbaPixels {
    private static final int CHANNELS = 4;

    private RgbaPixels() {
    }

    static byte[] fromAbgr(int[] pixels) {
        byte[] rgba = new byte[pixels.length * CHANNELS];
        for (int index = 0; index < pixels.length; index++) {
            int pixel = pixels[index];
            int offset = index * CHANNELS;
            rgba[offset] = (byte) pixel;
            rgba[offset + 1] = (byte) (pixel >>> 8);
            rgba[offset + 2] = (byte) (pixel >>> 16);
            rgba[offset + 3] = (byte) (pixel >>> 24);
        }
        return rgba;
    }
}
