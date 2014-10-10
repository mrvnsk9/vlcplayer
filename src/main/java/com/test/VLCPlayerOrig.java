package com.test;

import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.logger.Logger;
import uk.co.caprica.vlcj.player.*;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 *
 */
public class VLCPlayerOrig {
    /**
     * Log level, used only if the -Dvlcj.log= system property has not already been set.
     */
    private static final String VLCJ_LOG_LEVEL = "DEBUG";

    /**
     * Change this to point to your own vlc installation, or comment out the code if you want to use
     * your system default installation.
     * <p>
     * This is a bit more explicit than using the -Djna.library.path= system property.
     */
    private static final String NATIVE_LIBRARY_SEARCH_PATH = null;

    /**
     * Set to true to dump out native JNA memory structures.
     */
    private static final String DUMP_NATIVE_MEMORY = "false";

    /**
     * Video depth multiplier
     */
    private static final int DEPTH = 4;

    private ByteBuffer buffer;
    private int texture = 0;
    private int framerate = 0;
    private int width = 0;
    private int height = 0;
    private int bufferWidth = 0;
    private int bufferHeight = 0;
    private AtomicBoolean dirty = new AtomicBoolean(false);

    static {
        if(null == System.getProperty("vlcj.log")) {
            System.setProperty("vlcj.log", VLCJ_LOG_LEVEL);
        }

        // Safely try to initialise LibX11 to reduce the opportunity for native
        // crashes - this will silently throw an Error on Windows (and maybe MacOS)
        // that can safely be ignored
        LibXUtil.initialise();

        if(null != NATIVE_LIBRARY_SEARCH_PATH) {
            Logger.info("Explicitly adding JNA native library search path: '{}'", NATIVE_LIBRARY_SEARCH_PATH);
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), NATIVE_LIBRARY_SEARCH_PATH);
        }

        System.setProperty("jna.dump_memory", DUMP_NATIVE_MEMORY);
    }

    public VLCPlayerOrig() {
        LibVlc libVlc = LibVlcFactory.factory().create();

        Logger.info("version: {}", libVlc.libvlc_get_version());
        Logger.info("compiler: {}", libVlc.libvlc_get_compiler());
        Logger.info("changeset: {}", libVlc.libvlc_get_changeset());
    }

    public void run() {
        List<String> vlcArgs = new ArrayList<>();

        vlcArgs.add("--verbose=3");
        vlcArgs.add("--no-snapshot-preview");
//        vlcArgs.add("--quiet");
        vlcArgs.add("--quiet-synchro");
        vlcArgs.add("--intf");
        vlcArgs.add("dummy");

        // Special case to help out users on Windows (supposedly this is not actually needed)...
        // if(RuntimeUtil.isWindows()) {
        // vlcArgs.add("--plugin-path=" + WindowsRuntimeUtil.getVlcInstallDir() + "\\plugins");
        // }
        // else {
        // vlcArgs.add("--plugin-path=/home/linux/vlc/lib");
        // }

        // vlcArgs.add("--plugin-path=" + System.getProperty("user.home") + "/.vlcj");

        Logger.debug("vlcArgs={}", vlcArgs);

        MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory(vlcArgs.toArray(new String[vlcArgs.size()]));
        mediaPlayerFactory.setUserAgent("vlcj test player");

        List<AudioOutput> audioOutputs = mediaPlayerFactory.getAudioOutputs();
        Logger.debug("audioOutputs={}", audioOutputs);

        DirectMediaPlayer mediaPlayer = mediaPlayerFactory.newDirectMediaPlayer(new TestBufferFormatCallback(), new TestRenderCallback());
        mediaPlayer.addMediaPlayerEventListener(new TestMediaPlayerEventAdapter());
        String media = "trailer_480p.mov";
        mediaPlayer.startMedia(media);

        try {
            Display.setDisplayMode(new DisplayMode(width, height));
            Display.setTitle("Player Prototype");
            Display.create();
        } catch (LWJGLException e) {
            e.printStackTrace();
            System.exit(0);
        }

        init();

        while (!Display.isCloseRequested()) {
            if (buffer != null && dirty.get()) {
                render();
                Display.update();
                Display.sync((framerate > 0) ? framerate : 60);
                dirty.set(false);
            }
        }

        mediaPlayer.stop();
        mediaPlayer.release();
        Display.destroy();
    }

    private void init() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, 1, -1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
    }

    private void render() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glClearColor(0, 0, 0, 0);
        if (texture == 0) {
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            texture = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, DEPTH);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, bufferWidth, bufferHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, bufferWidth, bufferHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

        GL11.glBegin(GL11.GL_TRIANGLES);
        {
        /* 1st triangle */
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(0, 0);
            GL11.glTexCoord2f(0, 1);
            GL11.glVertex2f(0, bufferHeight);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(bufferWidth, bufferHeight);

        /* 2nd triangle */
            GL11.glTexCoord2f(0, 0);
            GL11.glVertex2f(0, 0);
            GL11.glTexCoord2f(1, 0);
            GL11.glVertex2f(bufferWidth, 0);
            GL11.glTexCoord2f(1, 1);
            GL11.glVertex2f(bufferWidth, bufferHeight);
        }
        GL11.glEnd();
    }

    private final class TestMediaPlayerEventAdapter extends MediaPlayerEventAdapter {
        @Override
        public void mediaMetaChanged(MediaPlayer mediaPlayer, int metaType) {
            super.mediaMetaChanged(mediaPlayer, metaType);
            if (!mediaPlayer.getTrackInfo(TrackType.VIDEO).isEmpty()) {
                VideoTrackInfo info = ((VideoTrackInfo) mediaPlayer.getTrackInfo(TrackType.VIDEO).get(0));
                framerate = info.frameRate();
                width = info.width();
                height = info.height();
                System.out.println("Meta Changed: " + info.width() + "x" + info.height() + ": " + info.frameRate());
            }
        }
    }

    private final class TestRenderCallback implements RenderCallback {

        @Override
        public void display(DirectMediaPlayer mediaPlayer, Memory[] nativeBuffers, BufferFormat bufferFormat) {
            bufferWidth = bufferFormat.getWidth();
            bufferHeight = bufferFormat.getHeight();
            buffer = nativeBuffers[0].getByteBuffer(0, (long) (bufferHeight * bufferWidth * DEPTH));
            dirty.set(true);
        }

    }

    private final class TestBufferFormatCallback implements BufferFormatCallback {

        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            return new BufferFormat("RGBA", sourceWidth, sourceHeight, new int[] { sourceWidth * DEPTH }, new int[] { sourceHeight });
        }

    }


    public static void main( String[] args )
    {
        new VLCPlayerOrig().run();
    }
}
