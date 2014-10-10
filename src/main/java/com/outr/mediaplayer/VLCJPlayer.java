package com.outr.mediaplayer;

import com.sun.jna.Memory;
import org.lwjgl.opengl.GL11;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.logger.Logger;
import uk.co.caprica.vlcj.player.*;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.BufferFormatCallback;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.RenderCallback;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VLCJPlayer is a rough implementation of Player utilizing VLCJ.
 *
 * @author Matt Hicks <matt@outr.com>
 */
public class VLCJPlayer implements Player {
    static {
        LibXUtil.initialise();
    }

    private final LibVlc vlc;
    private final DirectMediaPlayer mediaPlayer;
    private int framerate = 0;
    private int width = 0;
    private int height = 0;
    private ByteBuffer buffer;
    private int bufferWidth = 0;
    private int bufferHeight = 0;
    private AtomicBoolean dirty = new AtomicBoolean(false);

    public VLCJPlayer() {
        vlc = LibVlcFactory.factory().create();

        Logger.info("version: {}", vlc.libvlc_get_version());
        Logger.info("compiler: {}", vlc.libvlc_get_compiler());
        Logger.info("changeset: {}", vlc.libvlc_get_changeset());

        List<String> vlcArgs = new ArrayList<>();

//        vlcArgs.add("--verbose=3");
        vlcArgs.add("--no-snapshot-preview");
        vlcArgs.add("--quiet");
        vlcArgs.add("--quiet-synchro");
        vlcArgs.add("--intf");
        vlcArgs.add("dummy");

        Logger.debug("vlcArgs={}", vlcArgs);

        MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory(vlcArgs.toArray(new String[vlcArgs.size()]));
        mediaPlayerFactory.setUserAgent("vlcj test player");

        List<AudioOutput> audioOutputs = mediaPlayerFactory.getAudioOutputs();
        Logger.debug("audioOutputs={}", audioOutputs);

        mediaPlayer = mediaPlayerFactory.newDirectMediaPlayer(new TestBufferFormatCallback(), new TestRenderCallback());
        mediaPlayer.addMediaPlayerEventListener(new TestMediaPlayerEventAdapter());
    }

    @Override
    public void play(File file) {
        mediaPlayer.startMedia(file.getAbsolutePath());
        while (framerate == 0 || width == 0 || height == 0 || bufferWidth == 0 || bufferHeight == 0) {
            try {
                Thread.sleep(100);
            } catch(InterruptedException exc) {
                throw new RuntimeException(exc);
            }
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getFramerate() {
        return framerate;
    }

    @Override
    public int getBufferWidth() {
        return bufferWidth;
    }

    @Override
    public int getBufferHeight() {
        return bufferHeight;
    }

    @Override
    public void update(int textureId) {
        // Update the texture if it's dirty
        if (dirty.compareAndSet(true, false)) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, bufferWidth, bufferHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        }
    }

    @Override
    public void dispose() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    private final class TestBufferFormatCallback implements BufferFormatCallback {
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            return new BufferFormat("RGBA", sourceWidth, sourceHeight, new int[] { sourceWidth * MediaPlayer.DEPTH }, new int[] { sourceHeight });
        }
    }

    private final class TestRenderCallback implements RenderCallback {
        @Override
        public void display(DirectMediaPlayer mediaPlayer, Memory[] nativeBuffers, BufferFormat bufferFormat) {
            bufferWidth = bufferFormat.getWidth();
            bufferHeight = bufferFormat.getHeight();
            buffer = nativeBuffers[0].getByteBuffer(0, (long) (bufferHeight * bufferWidth * MediaPlayer.DEPTH));
            dirty.set(true);
        }
    }

    private final class TestMediaPlayerEventAdapter extends MediaPlayerEventAdapter {
        @Override
        public void mediaMetaChanged(uk.co.caprica.vlcj.player.MediaPlayer mediaPlayer, int metaType) {
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
}
