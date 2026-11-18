import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;
import javax.imageio.ImageIO;

public class CutoutCleaner {
    public static void makeCleanTransparent(String inPath, String outPath) throws Exception {
        BufferedImage src = ImageIO.read(new File(inPath));
        if (src == null) return;
        int w = src.getWidth();
        int h = src.getHeight();
        
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        boolean[][] isBg = new boolean[w][h];
        
        // Flood fill from all 4 borders to identify background
        Queue<int[]> queue = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            queue.add(new int[]{x, 0});
            queue.add(new int[]{x, h - 1});
            isBg[x][0] = true;
            isBg[x][h - 1] = true;
        }
        for (int y = 0; y < h; y++) {
            queue.add(new int[]{0, y});
            queue.add(new int[]{w - 1, y});
            isBg[0][y] = true;
            isBg[w - 1][y] = true;
        }
        
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        
        while (!queue.isEmpty()) {
            int[] pt = queue.poll();
            int cx = pt[0];
            int cy = pt[1];
            
            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];
                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !isBg[nx][ny]) {
                    int rgb = src.getRGB(nx, ny);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    // White or very bright pixel connected to boundary
                    if (r > 230 && g > 230 && b > 230) {
                        isBg[nx][ny] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }
        
        // Populate dst with smooth alpha anti-aliasing on edge pixels
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (isBg[x][y]) {
                    dst.setRGB(x, y, 0x00000000); // fully transparent
                } else {
                    int rgb = src.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    
                    // Check if adjacent to background for alpha feathering
                    boolean nearBg = false;
                    for (int i = 0; i < 4; i++) {
                        int nx = x + dx[i];
                        int ny = y + dy[i];
                        if (nx >= 0 && nx < w && ny >= 0 && ny < h && isBg[nx][ny]) {
                            nearBg = true;
                            break;
                        }
                    }
                    
                    if (nearBg && r > 210 && g > 210 && b > 210) {
                        int avg = (r + g + b) / 3;
                        int alpha = Math.max(0, Math.min(255, 255 - (avg - 210) * 8));
                        dst.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
                    } else {
                        dst.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
                    }
                }
            }
        }
        
        ImageIO.write(dst, "PNG", new File(outPath));
        System.out.println("Cleaned and saved: " + outPath);
    }
    
    public static void main(String[] args) throws Exception {
        String input = "C:/Users/manic/.gemini/antigravity-ide/brain/dee2f635-73e0-4064-affd-e3cdece19c90/stylized_walker_phone_1788197159715.jpg";
        makeCleanTransparent(input, "WebContent/images/character_phone.png");
    }
}
