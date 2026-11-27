package com.christmasdream.launcher

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import android.widget.FrameLayout
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer

/**
 * Native Filament viewport for the complete Ramadan GLB scene.
 *
 * The GLB is assembled from licensed, textured source models and detailed
 * Blender-authored geometry. This view does not draw substitute 2D artwork.
 */
class RamadanFilamentHeroView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var surface: SurfaceView? = null
    private var modelViewer: ModelViewer? = null
    private var prayerLightOn = false
    private var isRendering = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val viewer = modelViewer
            if (isRendering && viewer != null) {
                runCatching { viewer.render(frameTimeNanos) }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    init {
        setBackgroundColor(Color.rgb(3, 12, 14))
        clipChildren = true
        clipToPadding = true
        isFocusable = true
        contentDescription = "Полноценная интерактивная 3D-сцена Рамадана"
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post {
            if (modelViewer == null && isAttachedToWindow) createRenderer()
        }
    }

    override fun onDetachedFromWindow() {
        stopRendering()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && isAttachedToWindow) {
            if (modelViewer == null) post { if (modelViewer == null) createRenderer() }
            else startRendering()
        } else {
            stopRendering()
        }
    }

    private fun createRenderer() {
        if (modelViewer != null || !isAttachedToWindow) return

        val surfaceView = SurfaceView(context).apply {
            setZOrderMediaOverlay(true)
            setBackgroundColor(Color.TRANSPARENT)
        }
        addView(surfaceView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        surface = surfaceView

        try {
            val viewer = ModelViewer(surfaceView)
            modelViewer = viewer
            surfaceView.setOnTouchListener { _, event ->
                viewer.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) performClick()
                true
            }

            val bytes = context.assets.open("models/ramadan_dream_scene.glb").use { it.readBytes() }
            viewer.loadModelGlb(ByteBuffer.wrap(bytes))
            viewer.transformToUnitCube()
            viewer.cameraFocalLength = 34f

            // Night-time baseline; prayer time raises the warm architectural illumination.
            val lightManager = viewer.engine.lightManager
            val lightInstance = lightManager.getInstance(viewer.light)
            lightManager.setIntensity(lightInstance, if (prayerLightOn) 68000f else 14500f)
            lightManager.setColor(
                lightInstance,
                if (prayerLightOn) 1.0f else 0.58f,
                if (prayerLightOn) 0.78f else 0.72f,
                if (prayerLightOn) 0.57f else 0.82f
            )

            viewer.view.renderQuality = viewer.view.renderQuality.apply {
                hdrColorBuffer = View.QualityLevel.MEDIUM
            }
            viewer.view.dynamicResolutionOptions = viewer.view.dynamicResolutionOptions.apply {
                enabled = true
                quality = View.QualityLevel.MEDIUM
            }
            viewer.view.antiAliasing = View.AntiAliasing.FXAA
            viewer.view.ambientOcclusionOptions = viewer.view.ambientOcclusionOptions.apply {
                enabled = true
            }
            viewer.view.bloomOptions = viewer.view.bloomOptions.apply {
                enabled = true
            }

            surfaceView.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: android.view.View) = Unit
                override fun onViewDetachedFromWindow(v: android.view.View) {
                    // ModelViewer's own detach listener releases Filament's native resources.
                    stopRendering()
                    modelViewer = null
                    surface = null
                }
            })
            startRendering()
        } catch (error: Throwable) {
            modelViewer = null
            removeView(surfaceView)
            surface = null
            android.util.Log.e("Ramadan3D", "Could not initialize the Filament scene", error)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun setPrayerLightOn(enabled: Boolean) {
        prayerLightOn = enabled
        val viewer = modelViewer ?: return
        runCatching {
            val manager = viewer.engine.lightManager
            val instance = manager.getInstance(viewer.light)
            manager.setIntensity(instance, if (enabled) 68000f else 14500f)
            manager.setColor(
                instance,
                if (enabled) 1.0f else 0.58f,
                if (enabled) 0.78f else 0.72f,
                if (enabled) 0.52f else 0.82f
            )
        }.onFailure {
            android.util.Log.w("Ramadan3D", "Prayer lighting update failed", it)
        }
    }

    private fun startRendering() {
        if (modelViewer == null || isRendering) return
        isRendering = true
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopRendering() {
        isRendering = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    companion object {
        init {
            Utils.init()
        }
    }
}
