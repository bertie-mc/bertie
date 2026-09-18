using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Text;

// Native pixel-art source. Every character is exactly one texture pixel.
// No generated illustration, scaling, antialiasing, or image sampling is used.
public static class PocketWatchTexture
{
    private static readonly string[] Case = {
        "......DDDD......",
        "......DGHD......",
        "......D..D......",
        "......DGGD......",
        "....DDHGGGDD....",
        "...DHGGvvvGGD...",
        "..DHGvvvvvvvGD..",
        "..DGvvvvvvvvGD..",
        ".DGvvvvvvvvvvGD.",
        ".DGvvvvvvvvvvGD.",
        ".DGvvvvvvvvvvGD.",
        "..DGvvvvvvvvGD..",
        "..DSGvvvvvvGSD..",
        "...DSGGGGGGSD...",
        "....DDSSSSDD....",
        "......DDDD......"
    };

    private static readonly Dictionary<char, Color> Ink = new Dictionary<char, Color> {
        { '.', Color.FromArgb(0, 0, 0, 0) },
        { 'D', Color.FromArgb(255, 53, 32, 25) },
        { 'S', Color.FromArgb(255, 155, 93, 35) },
        { 'G', Color.FromArgb(255, 231, 172, 55) },
        { 'H', Color.FromArgb(255, 255, 226, 137) },
        { 'v', Color.FromArgb(255, 48, 18, 76) },
        { 'p', Color.FromArgb(255, 95, 32, 148) },
        { 'b', Color.FromArgb(255, 153, 58, 218) },
        { 'l', Color.FromArgb(255, 206, 117, 245) },
        { 'w', Color.FromArgb(255, 241, 187, 255) }
    };

    // The same grid defines the fixed clock hand: three diagonal pixels and its pivot.
    private static readonly Point[] Hand = {
        new Point(7, 9), new Point(8, 8), new Point(9, 7)
    };

    public static string Draw(string texturePath, string previewDirectory)
    {
        foreach (var row in Case) if (row.Length != 16) throw new Exception("Case row must contain exactly 16 pixels");
        var frames = new List<Bitmap>();
        using (var strip = new Bitmap(16, 256, PixelFormat.Format32bppArgb))
        {
            for (int frame = 0; frame < 16; frame++)
            {
                var sprite = new Bitmap(16, 16, PixelFormat.Format32bppArgb);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
                {
                    char pixel = Case[y][x];
                    if (pixel == 'v')
                    {
                        // Move the spiral through discrete palette entries on this fixed grid.
                        double dx = x - 7.5, dy = y - 9;
                        double radius = Math.Sqrt(dx * dx + dy * dy);
                        double phase = Math.Atan2(dy, dx) - radius * 1.15 - frame * Math.PI / 8;
                        double turn = (phase / (2 * Math.PI) % 1 + 1) % 1;
                        pixel = "vvppbllwlbpv"[(int)(turn * 12)];
                    }
                    sprite.SetPixel(x, y, Ink[pixel]);
                }
                foreach (var point in Hand) sprite.SetPixel(point.X, point.Y, Ink['H']);
                frames.Add(sprite);
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
                {
                    var pixel = sprite.GetPixel(x, y);
                    strip.SetPixel(x, frame * 16 + y, pixel);
                    if (pixel.A != 0 && pixel.A != 255) throw new Exception("Partial alpha is forbidden");
                    if (Case[y][x] != 'v' && pixel.ToArgb() != frames[0].GetPixel(x, y).ToArgb())
                        throw new Exception("The case moved");
                }
                foreach (var point in Hand) if (sprite.GetPixel(point.X, point.Y).ToArgb() != Ink['H'].ToArgb())
                    throw new Exception("The clock hand moved");
            }
            Directory.CreateDirectory(previewDirectory);
            strip.Save(texturePath, ImageFormat.Png);
            frames[0].Save(Path.Combine(previewDirectory, "pocket-watch-native-16.png"), ImageFormat.Png);
            using (var preview = new Bitmap(256, 256, PixelFormat.Format32bppArgb))
            {
                // Exact integer enlargement: a native pixel is one flat 16x16 square.
                for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++)
                    preview.SetPixel(x, y, frames[0].GetPixel(x / 16, y / 16));
                preview.Save(Path.Combine(previewDirectory, "pocket-watch-native-enlarged.png"), ImageFormat.Png);
            }
            using (var proof = new Bitmap(257, 257, PixelFormat.Format32bppArgb))
            {
                for (int y = 0; y <= 256; y++) for (int x = 0; x <= 256; x++)
                {
                    var pixel = x % 16 == 0 || y % 16 == 0
                        ? Color.FromArgb(255, 82, 82, 82)
                        : frames[0].GetPixel(x / 16, y / 16);
                    if (pixel.A == 0) pixel = Color.FromArgb(255, 30, 30, 30);
                    proof.SetPixel(x, y, pixel);
                }
                proof.Save(Path.Combine(previewDirectory, "pocket-watch-native-grid-proof.png"), ImageFormat.Png);
            }
            WriteGif(frames, Path.Combine(previewDirectory, "pocket-watch-native-animation.gif"));
        }
        foreach (var frame in frames) frame.Dispose();
        return "Native 16x16 pixel source exported: fixed case, 3-pixel diagonal hand, 16 purple animation frames.";
    }

    private static void WriteGif(List<Bitmap> frames, string destination)
    {
        var palette = new List<int> { 0 };
        foreach (var frame in frames) for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++)
        {
            var c = frame.GetPixel(x, y);
            if (c.A == 0) continue;
            int rgb = c.ToArgb() & 0xFFFFFF;
            if (!palette.Contains(rgb)) palette.Add(rgb);
        }
        using (var writer = new BinaryWriter(File.Create(destination)))
        {
            writer.Write(Encoding.ASCII.GetBytes("GIF89a"));
            writer.Write((ushort)256); writer.Write((ushort)256);
            writer.Write(new byte[] { 0xF7, 0, 0 });
            for (int i = 0; i < 256; i++)
            {
                int rgb = i < palette.Count ? palette[i] : 0;
                writer.Write(new byte[] { (byte)(rgb >> 16), (byte)(rgb >> 8), (byte)rgb });
            }
            writer.Write(new byte[] { 0x21, 0xFF, 11 });
            writer.Write(Encoding.ASCII.GetBytes("NETSCAPE2.0"));
            writer.Write(new byte[] { 3, 1, 0, 0, 0 });
            foreach (var frame in frames)
            {
                writer.Write(new byte[] { 0x21, 0xF9, 4, 9, 10, 0, 0, 0, 0x2C });
                writer.Write((ushort)0); writer.Write((ushort)0);
                writer.Write((ushort)256); writer.Write((ushort)256); writer.Write((byte)0);
                writer.Write((byte)8);
                var packed = new List<byte>(); uint bits = 0; int bitCount = 0;
                Action<int> code = value => { bits |= (uint)value << bitCount; bitCount += 9; while (bitCount >= 8) { packed.Add((byte)bits); bits >>= 8; bitCount -= 8; } };
                code(256); int sinceClear = 0;
                for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++)
                {
                    if (sinceClear == 120) { code(256); sinceClear = 0; }
                    var c = frame.GetPixel(x / 16, y / 16);
                    code(c.A == 0 ? 0 : palette.IndexOf(c.ToArgb() & 0xFFFFFF)); sinceClear++;
                }
                code(257); if (bitCount > 0) packed.Add((byte)bits);
                var data = packed.ToArray();
                for (int offset = 0; offset < data.Length; offset += 255)
                {
                    int count = Math.Min(255, data.Length - offset);
                    writer.Write((byte)count); writer.Write(data, offset, count);
                }
                writer.Write((byte)0);
            }
            writer.Write((byte)0x3B);
        }
    }
}
