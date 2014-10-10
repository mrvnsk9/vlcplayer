package com.outr.mediaplayer;

import java.io.File;

/**
 * Player provides an interface to play media content.
 *
 * @author Matt Hicks <matt@outr.com>
 */
public interface Player {
    /**
     * Loads and begins playing the media supplied in 'file'.
     *
     * This method should block until the width, height, framerate, bufferWidth, and bufferHeight have been determined.
     *
     * @param file the file to play
     */
    public void play(File file);

    /**
     * @return The video's actual width.
     */
    public int getWidth();

    /**
     * @return The video's actual height.
     */
    public int getHeight();

    /**
     * @return The video's framerate.
     */
    public int getFramerate();

    /**
     * @return The buffer's width (may be the same as the video or may be PoT larger).
     */
    public int getBufferWidth();

    /**
     * @return The buffer's height (may be the same as the video or may be PoT larger).
     */
    public int getBufferHeight();

    /**
     * Update the supplied textureId with the current frame data if the frame has changed since last update.
     *
     * @param textureId the texture to update
     */
    public void update(int textureId);

    /**
     * Dispose and cleanup the player.
     */
    public void dispose();
}
