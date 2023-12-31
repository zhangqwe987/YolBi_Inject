package cn.yapeteam.yolbi.shader;

import cn.yapeteam.yolbi.YolBi;
import cn.yapeteam.yolbi.event.Listener;
import cn.yapeteam.yolbi.event.impl.game.EventLoop;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minecraft.client.renderer.GlStateManager.disableBlend;
import static net.minecraft.client.renderer.GlStateManager.enableTexture2D;
import static org.lwjgl.opengl.GL11.*;

public abstract class Shader {
    public int identifier = -1;
    public float width, height;
    public final int level;
    public final boolean antiAlias, multithreading;
    public int texture;

    public Shader(int level, boolean antiAlias, boolean multithreading) {
        this.level = level;
        this.antiAlias = antiAlias;
        this.multithreading = multithreading;
        this.texture = GL11.glGenTextures();
    }

    public abstract int dispose(int relativeX, int relativeY, float screenWidth, float screenHeight, int pixel);

    @Nullable
    public abstract Object[] params();

    public void setWidth(float width) {
        this.width = width * level;
    }

    public void setHeight(float height) {
        this.height = height * level;
    }

    public float getRealWidth() {
        return width / level;
    }

    public float getRealHeight() {
        return height / level;
    }

    private int lastIdentifier = -1;
    private boolean disposing = false;

    public void render(float x, float y, int color) {
        identifier = Arrays.deepHashCode(params());
        if (identifier != lastIdentifier && !disposing) {
            lastIdentifier = identifier;
            disposing = true;
            compile();
        }
        drawImage(texture, x, y, getRealWidth(), getRealHeight(), color);
    }

    private void compile() {
        int width = (int) this.width, height = (int) this.height;
        Runnable connect = () -> {
            int size = width * height;
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
            for (int i = 0; i < size; i++) {
                int x = i % width, y = i / width;
                image.setRGB(x, y, dispose(x, y, sr.getScaledWidth(), sr.getScaledHeight(), 0));
            }
            image = antiAlias ? new GaussianFilter(level / 2f).filter(image, null) : image;
            BufferedImage finalImage = image;
            Runnable task = () -> TextureUtil.uploadTextureImageAllocate(
                    texture,
                    finalImage,
                    true, false
            );
            if (multithreading)
                tasks.add(task);
            else task.run();
            disposing = false;
        };
        if (multithreading)
            new Thread(connect).start();
        else connect.run();
    }

    private final static CopyOnWriteArrayList<Runnable> tasks = new CopyOnWriteArrayList<>();

    static {
        YolBi.instance.getEventManager().register(Shader.class);
    }

    public static void drawImage(ResourceLocation image, int x, int y, int width, int height) {
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(image);
            glEnable(GL11.GL_BLEND);
            Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, width, height, width, height);
            glDisable(GL11.GL_BLEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void drawImage(int image, float x, float y, float width, float height, int color) {
        GlStateManager.bindTexture(image);
        Color c = new Color(color);
        GlStateManager.color(c.getRed() / 255f, c.getGreen() / 255f, c.getGreen() / 255f);
        glPushMatrix();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01f);
        glEnable(GL11.GL_TEXTURE_2D);
        glDisable(GL_CULL_FACE);
        glEnable(GL11.GL_ALPHA_TEST);
        GlStateManager.enableBlend();
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); // top left
        GL11.glVertex2f(x, y);

        GL11.glTexCoord2f(0, 1); // bottom left
        GL11.glVertex2f(x, y + height);

        GL11.glTexCoord2f(1, 1); // bottom right
        GL11.glVertex2f(x + width, y + height);

        GL11.glTexCoord2f(1, 0); // top right
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();
        enableTexture2D();
        disableBlend();
        GlStateManager.resetColor();

        glEnable(GL_CULL_FACE);
        glPopMatrix();
    }

    @Listener
    private static void onLoop(EventLoop e) {
        for (Runnable task : tasks) {
            task.run();
            tasks.remove(task);
        }
    }

    @AllArgsConstructor
    public static class vec2 {
        public double x, y;
    }

    @AllArgsConstructor
    public static class vec3 {
        public double x, y, z;
    }

    @AllArgsConstructor
    public static class vec4 {
        public double x, y, z, w;
    }

    public static vec2 vec2(double x, double y) {
        return new vec2(x, y);
    }

    public static vec3 vec3(double x, double y, double z) {
        return new vec3(x, y, z);
    }

    public static vec3 vec3(vec2 v, double z) {
        return new vec3(v.x, v.y, z);
    }

    public static vec4 vec4(double x, double y, double z, double w) {
        return new vec4(x, y, z, w);
    }

    public static vec4 vec4(vec3 v, double w) {
        return new vec4(v.x, v.y, v.z, w);
    }

    private static double atan2(double y, double x) {
        double ax = x >= 0.0 ? x : -x;
        double ay = y >= 0.0 ? y : -y;
        double a = Math.min(ax, ay) / Math.max(ax, ay);
        double s = a * a;
        double r = ((-0.0464964749 * s + 0.15931422) * s - 0.327622764) * s * a + a;
        if (ay > ax) {
            r = 1.57079637 - r;
        }

        if (x < 0.0) {
            r = 3.14159274 - r;
        }

        return y >= 0.0 ? r : -r;
    }

    public static int mod(int a, int b) {
        return a % b;
    }

    public static double mod(double a, double b) {
        return a % b;
    }

    public static vec2 mod(vec2 a, double b) {
        return vec2(mod(a.x, b), mod(a.y, b));
    }

    public static double fma(double a, double b, double c) {
        return a * b + c;
    }

    // 计算向量的点积
    public static double dot(vec2 v1, vec2 v2) {
        return atan2(v1.x * v2.x + v1.y * v2.y, v1.x * v2.y - v1.y * v2.x);
    }

    public static double dot(vec3 v1, vec3 v2) {
        return fma(v1.x, v2.x, fma(v1.y, v2.y, v1.z * v2.z));
    }

    public static double dot(vec4 v1, vec4 v2) {
        return fma(v1.x, v2.x, fma(v1.y, v2.y, fma(v1.z, v2.z, v1.w * v2.w)));
    }

    // 线性插值
    public static double mix(double v1, double v2, double a) {
        return v1 * (1 - a) + v2 * a;
    }

    public static vec2 mix(vec2 v1, vec2 v2, double a) {
        return vec2(mix(v1.x, v2.x, a), mix(v1.y, v2.y, a));
    }

    public static vec3 mix(vec3 v1, vec3 v2, double a) {
        return vec3(mix(v1.x, v2.x, a), mix(v1.y, v2.y, a), mix(v1.z, v2.z, a));
    }

    public static vec4 mix(vec4 v1, vec4 v2, double a) {
        return vec4(mix(v1.x, v2.x, a), mix(v1.y, v2.y, a), mix(v1.z, v2.z, a), mix(v1.w, v2.w, a));
    }

    // 计算向量长度
    public static double length(vec2 vec2) {
        return length(vec2.x, vec2.y);
    }

    public static double length(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    // 计算指数函数
    public static double exp(double x) {
        return Math.exp(x);
    }

    // 计算浮点数的小数部分
    public static float fract(float x) {
        return x - (int) x;
    }

    public static vec3 floor(vec3 vec3) {
        return vec3(Math.floor(vec3.x), Math.floor(vec3.y), Math.floor(vec3.z));
    }

    public static int color(float r, float g, float b, float a) {
        int red = (int) (r * 255), green = (int) (g * 255), blue = (int) (b * 255), alpha = (int) (a * 255);
        red = Math.min(red, 255);
        green = Math.min(green, 255);
        blue = Math.min(blue, 255);
        alpha = Math.min(alpha, 255);
        return color(red, green, blue, alpha);
    }

    public static int color(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) |
               ((r & 0xFF) << 16) |
               ((g & 0xFF) << 8) |
               ((b & 0xFF));
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) return min;
        return Math.min(value, max);
    }
}