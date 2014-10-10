package com.test;

/**
 * Created by Brian on 10/9/2014.
 */
public class MediaMeta {
    private final int width;
    private final int height;
    private final int fps;

    public MediaMeta(int width, int height, int fps) {
        this.width = width;
        this.height = height;
        this.fps = fps;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFps() {
        return fps;
    }
}
