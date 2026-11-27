package com.christmasdream.launcher

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build

/**
 * Plays the bundled Christmas soundtrack.
 *
 * The resource name stays stable (R.raw.jingle_bells), so the soundtrack can be
 * replaced without changing the settings UI or the user's saved music preference.
 */
class JingleMusicPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null
    private var hasAudioFocus = false
    private var resumeAfterFocus = false

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        synchronized(this) {
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    hasAudioFocus = true
                    player?.setVolume(0.20f, 0.20f)
                    if (resumeAfterFocus) {
                        resumeAfterFocus = false
                        try { player?.let { if (!it.isPlaying) it.start() } } catch (_: Exception) { releasePlayer() }
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    player?.setVolume(0.055f, 0.055f)
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    resumeAfterFocus = player?.isPlaying == true
                    try { player?.pause() } catch (_: Exception) { releasePlayer() }
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    resumeAfterFocus = false
                    try { player?.pause() } catch (_: Exception) { releasePlayer() }
                    abandonFocus()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestFocus(): Boolean {
        if (hasAudioFocus) return true
        val result = audioManager.requestAudioFocus(
            focusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        if (!hasAudioFocus) return
        try { audioManager.abandonAudioFocus(focusListener) } catch (_: Exception) { }
        hasAudioFocus = false
    }

    @Synchronized
    fun start() {
        try {
            if (!requestFocus()) return
            var current = player
            if (current == null) {
                current = MediaPlayer.create(appContext, R.raw.jingle_bells) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    current.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                }
                current.isLooping = true
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
        resumeAfterFocus = false
        try { player?.let { if (it.isPlaying) it.pause() } } catch (_: Exception) { releasePlayer() }
        abandonFocus()
    }

    @Synchronized
    fun stop() {
        resumeAfterFocus = false
        release()
    }

    @Synchronized
    fun release() {
        resumeAfterFocus = false
        releasePlayer()
        abandonFocus()
    }

    private fun releasePlayer() {
        try { player?.release() } catch (_: Exception) { }
        player = null
    }
}
