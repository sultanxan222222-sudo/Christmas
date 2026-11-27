package com.christmasdream.launcher

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import java.util.concurrent.ThreadLocalRandom

data class ChristmasTrack(val uri: String, val title: String)

/** Lightweight playlist engine for the built-in Christmas MP3 player. */
class ChristmasPlaylistPlayer(
    context: Context,
    private val onChanged: (playing: Boolean, position: Int, duration: Int, title: String) -> Unit
) {
    private val appContext = context.applicationContext
    val tracks = mutableListOf<ChristmasTrack>()
    var currentIndex = 0
        private set
    var shuffle = false
    var repeatAll = true
    var repeatOne = false
    private var player: MediaPlayer? = null
    var isPlaying = false
        private set

    val currentTrack: ChristmasTrack? get() = tracks.getOrNull(currentIndex)
    val position: Int get() = try { player?.currentPosition ?: 0 } catch (_: Exception) { 0 }
    val duration: Int get() = try { player?.duration ?: 0 } catch (_: Exception) { 0 }

    fun play(index: Int = currentIndex) {
        if (tracks.isEmpty()) return
        currentIndex = index.coerceIn(0, tracks.lastIndex)
        releasePlayer()
        val track = tracks[currentIndex]
        try {
            player = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                }
                setDataSource(appContext, Uri.parse(track.uri))
                setOnPreparedListener {
                    it.start()
                    this@ChristmasPlaylistPlayer.isPlaying = true
                    publish()
                }
                setOnCompletionListener {
                    if (repeatOne) play(currentIndex) else next()
                }
                setOnErrorListener { _, _, _ ->
                    this@ChristmasPlaylistPlayer.isPlaying = false
                    publish()
                    true
                }
                prepareAsync()
            }
            isPlaying = false
            publish()
        } catch (_: Exception) {
            releasePlayer()
            isPlaying = false
            publish()
        }
    }

    fun toggle() {
        val p = player
        if (p == null) { play(); return }
        try {
            if (p.isPlaying) { p.pause(); isPlaying = false }
            else { p.start(); isPlaying = true }
        } catch (_: Exception) { play() }
        publish()
    }

    fun pause() {
        try { player?.let { if (it.isPlaying) it.pause() } } catch (_: Exception) { }
        isPlaying = false
        publish()
    }

    fun next() {
        if (tracks.isEmpty()) return
        val nextIndex = if (shuffle && tracks.size > 1) {
            var candidate = currentIndex
            while (candidate == currentIndex) candidate = ThreadLocalRandom.current().nextInt(tracks.size)
            candidate
        } else if (currentIndex < tracks.lastIndex) currentIndex + 1 else if (repeatAll) 0 else currentIndex
        if (nextIndex == currentIndex && !repeatAll && !shuffle) { pause(); seekTo(0); return }
        play(nextIndex)
    }

    fun previous() {
        if (tracks.isEmpty()) return
        if (position > 3000) { seekTo(0); return }
        play(if (currentIndex > 0) currentIndex - 1 else tracks.lastIndex)
    }

    fun seekTo(value: Int) {
        try { player?.seekTo(value.coerceIn(0, duration)) } catch (_: Exception) { }
        publish()
    }

    fun remove(index: Int) {
        if (index !in tracks.indices) return
        val wasCurrent = index == currentIndex
        tracks.removeAt(index)
        if (tracks.isEmpty()) {
            releasePlayer()
            currentIndex = 0
            isPlaying = false
        } else if (wasCurrent) {
            currentIndex = (index.coerceAtMost(tracks.lastIndex))
            play(currentIndex)
        } else {
            if (index < currentIndex) currentIndex--
        }
        publish()
    }

    fun publish() {
        onChanged(isPlaying, position, duration, currentTrack?.title ?: "Выберите праздничную музыку")
    }

    fun release() {
        releasePlayer()
        isPlaying = false
    }

    private fun releasePlayer() {
        try { player?.setOnPreparedListener(null); player?.setOnCompletionListener(null); player?.release() } catch (_: Exception) { }
        player = null
    }
}
