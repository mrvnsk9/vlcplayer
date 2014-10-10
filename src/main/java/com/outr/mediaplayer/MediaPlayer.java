package com.outr.mediaplayer;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * MediaPlayer provides a display context to render a supplied video file.
 *
 * @author Matt Hicks <matt@outr.com>
 */
public class MediaPlayer {
    public static final int DEPTH = 4;

    private final File video;
    private final Player player;
    private final boolean syncToFramerate;
    private int frequency;
    private int textureId;

    public MediaPlayer(File video, Player player, boolean syncToFramerate) {
        this.video = video;
        this.player = player;
        this.syncToFramerate = syncToFramerate;
    }

    public void run() {
        try {
            player.play(video);         // Synchronously loads the video

            init();

            while (!Display.isCloseRequested()) {
                render();
            }

            player.dispose();
            Display.destroy();
        } catch (LWJGLException exc) {
            exc.printStackTrace();
            System.exit(1);
        }
    }

    public void init() throws LWJGLException {
        Display.setDisplayMode(new DisplayMode(player.getWidth(), player.getHeight()));
        Display.setTitle("MediaPlayer Prototype");
        Display.create();
        frequency = Display.getDisplayMode().getFrequency();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, player.getWidth(), player.getHeight(), 0, 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, DEPTH);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, player.getBufferWidth(), player.getBufferHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
    }

    public void render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glClearColor(0, 0, 0, 0);

        // Give the player the opportunity to update the texture
        player.update(textureId);

        // Draw texture to screen
        GL11.glBegin(GL11.GL_TRIANGLES);
        {
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(0, 0);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(0, player.getBufferHeight());
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(player.getBufferWidth(), player.getBufferHeight());

            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(0, 0);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(player.getBufferWidth(), 0);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(player.getBufferWidth(), player.getBufferHeight());
        }
        GL11.glEnd();

        Display.update();
        Display.sync(syncToFramerate ? player.getFramerate() : frequency);
    }

    public static void main(String[] args) throws Exception {
        File video = new File("trailer_480p.mov");
        Player player = new VLCJPlayer();
        MediaPlayer mediaPlayer = new MediaPlayer(video, player, false);
        mediaPlayer.run();
    }
}
