import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MakePng {
    public static void process(String inPath, String outPath) throws Exception {
        BufferedImage src = ImageIO.read(new File(inPath));
        if (src == null) return;
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        
        int bgR = (src.getRGB(0, 0) >> 16) & 0xFF;
        int bgG = (src.getRGB(0, 0) >> 8) & 0xFF;
        int bgB = src.getRGB(0, 0) & 0xFF;
        
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                int diff = Math.max(Math.abs(r - bgR), Math.max(Math.abs(g - bgG), Math.abs(b - bgB)));
                if ((r > 240 && g > 240 && b > 240) || diff < 20) {
                    dst.setRGB(x, y, 0x00000000);
                } else if (r > 225 && g > 225 && b > 225) {
                    int avg = (r + g + b) / 3;
                    int alpha = Math.max(0, Math.min(255, 255 - (avg - 225) * 15));
                    dst.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
                } else {
                    dst.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
                }
            }
        }
        ImageIO.write(dst, "PNG", new File(outPath));
        System.out.println("Created transparent PNG: " + outPath);
    }
    
    public static void main(String[] args) throws Exception {
        process("WebContent/images/character_phone.jpg", "WebContent/images/character_phone.png");
        process("WebContent/images/character_laptop.jpg", "WebContent/images/character_laptop.png");
        process("WebContent/images/landing_hero_3d.jpg", "WebContent/images/landing_hero_3d.png");
    }
}
