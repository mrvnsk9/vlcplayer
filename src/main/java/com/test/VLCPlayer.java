package com.test;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

/**
 *
 *
 */
public class VLCPlayer extends VLCPLayerEventAdapter {
    private int texture = 0;
    private int fps = 0;
    private int width = 640;
    private int height = 480;
    private boolean resized = false;

    public VLCPlayer() {
    }

    public void run() {
        try {
            setDisplaySize(width, height);
            Display.setTitle("VLC Player Prototype");
            Display.create();
        } catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        init();

        VLCJMediaPlayer player = new VLCJMediaPlayer("D:\\big_buck_bunny_480p_h264.mov", texture, this);
        player.play();

        while (!Display.isCloseRequested()) {
            if (resized) resize();
//            Display.update();
            Display.sync((fps > 0) ? fps : 60);
        }

        player.stop();
        Display.destroy();
    }

    private void init() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        texture = GL11.glGenTextures();
    }

    private void render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glClearColor(0, 0, 0, 0);
    }

    private void resize() {
        setDisplaySize(width, height);
        GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
        resized = false;
    }

    @Override
    public void mediaMetaChanged(MediaMeta meta) {
        super.mediaMetaChanged(meta);
        if ((meta.getWidth() + meta.getHeight() + meta.getFps()) > 0) {
            width = meta.getWidth();
            height = meta.getHeight();
            fps = meta.getFps();
            resized = true;
        }
    }

    private static void setDisplaySize(int width, int height) {
        try {
            Display.setDisplayMode(new DisplayMode(width, height));
        } catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main( String[] args )
    {
        new VLCPlayer().run();
    }
}
