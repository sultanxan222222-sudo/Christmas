package com.christmasdream.launcher

import android.content.Context
import android.media.MediaPlayer

/**
 * Plays the bundled, sampled orchestral arrangement (about three minutes).
 * MediaPlayer decodes the real MP3 recording; this is deliberately not a beep/sine-wave synth.
 */
class JingleMusicPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var player: MediaPlayer? = null

    @Synchronized
    fun start() {
        try {
            var current = player
            if (current == null) {
                current = MediaPlayer.create(appContext, R.raw.jingle_bells) ?: return
                current.isLooping = true
                // Quiet background level. Android's own media-volume control still applies.
                current.setVolume(0.20f, 0.20f)
                player = current
            }
            if (!current.isPlaying) current.start()
        } catch (_: Exception) {
            release()
        }
    }

    @Synchronized
    fun pause() {
        try {
            player?.let { if (it.isPlaying) it.pause() }
        } catch (_: Exception) {
            release()
        }
    }

    @Synchronized
    fun stop() {
        release()
    }

    @Synchronized
    fun release() {
        try {
            player?.release()
        } catch (_: Exception) {
        } finally {
            player = null
        }
    }
}
