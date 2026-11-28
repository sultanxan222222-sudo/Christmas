package com.christmasdream.launcher

import android.content.Context
import android.graphics.Color
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import android.widget.FrameLayout
import com.google.android.filament.View
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
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
    private var ambientLight: IndirectLight? = null
    private var nightSkybox: Skybox? = null
    private var prayerLightOn = false
    private var isRendering = false
    private var animationStartNanos = 0L
    private var lastRenderNanos = 0L
    private val isTvDevice by lazy { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    private val frameIntervalNanos = 1_000_000_000L / 30L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val viewer = modelViewer
            if (isRendering && viewer != null) {
                // Cap GPU work to 30 FPS; Choreographer still follows the display clock.
                if (lastRenderNanos != 0L && frameTimeNanos - lastRenderNanos < frameIntervalNanos) {
                    Choreographer.getInstance().postFrameCallback(this)
                    return
                }
                lastRenderNanos = frameTimeNanos
                runCatching {
                    val animator = viewer.animator
                    if (animator != null && animator.animationCount > 0) {
                        if (animationStartNanos == 0L) animationStartNanos = frameTimeNanos
                        val elapsed = ((frameTimeNanos - animationStartNanos) / 1_000_000_000.0 % 2.0).toFloat()
                        // The GLB contains separate clips for the flags, flames, lights and kitten.
                        // Apply every clip at the same clock time so the complete scene animates together.
                        for (index in 0 until animator.animationCount) {
                            val duration = animator.getAnimationDuration(index)
                            val clipTime = if (duration > 0f) elapsed % duration else elapsed
                            animator.applyAnimation(index, clipTime)
                        }
                        animator.updateBoneMatrices()
                    }
                    viewer.render(frameTimeNanos)
                }
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
            setBackgroundColor(Color.TRANSPARENT)
        }
        addView(surfaceView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        surface = surfaceView

        // Run our resource cleanup before ModelViewer's own surface-detach cleanup.
        surfaceView.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: android.view.View) = Unit
            override fun onViewDetachedFromWindow(v: android.view.View) {
                stopRendering()
                val engine = modelViewer?.engine
                ambientLight?.let { light -> runCatching { engine?.destroyIndirectLight(light) } }
                nightSkybox?.let { sky -> runCatching { engine?.destroySkybox(sky) } }
                ambientLight = null
                nightSkybox = null
                modelViewer = null
                surface = null
                animationStartNanos = 0L
                lastRenderNanos = 0L
            }
        })

        try {
            val viewer = ModelViewer(surfaceView)
            modelViewer = viewer
            surfaceView.setOnTouchListener { _, event ->
                viewer.onTouchEvent(event)
                if (event.action == MotionEvent.ACTION_UP) performClick()
                true
            }

            val bytes = context.assets.open("models/ramadan_dream_scene.glb").use { it.readBytes() }
            // Filament's native gltfio loader requires a direct buffer. ByteBuffer.wrap()
            // creates a heap buffer whose address is not available to JNI, which can
            // leave the SurfaceView showing only its clear color on Android TV.
            val glbBuffer = ByteBuffer.allocateDirect(bytes.size).apply {
                put(bytes)
                flip()
            }
            viewer.loadModelGlb(glbBuffer)
            viewer.autoPlayAnimations = false
            animationStartNanos = 0L
            viewer.transformToUnitCube()
            viewer.cameraFocalLength = 34f

            // Cool low-level ambient illumination keeps PBR stone, glass and brass readable at night.
            val createdAmbient = IndirectLight.Builder()
                .irradiance(1, floatArrayOf(0.42f, 0.50f, 0.58f))
                .intensity(20000f)
                .build(viewer.engine)
            ambientLight = createdAmbient
            val createdSkybox = Skybox.Builder()
                .color(0.006f, 0.018f, 0.024f, 1.0f)
                .build(viewer.engine)
            nightSkybox = createdSkybox
            viewer.scene.indirectLight = createdAmbient
            viewer.scene.skybox = createdSkybox

            // Night-time baseline; prayer time raises the warm architectural illumination.
            val lightManager = viewer.engine.lightManager
            val lightInstance = lightManager.getInstance(viewer.light)
            lightManager.setIntensity(lightInstance, if (prayerLightOn) 65000f else 8000f)
            applyPrayerPointLights(viewer, prayerLightOn)
            lightManager.setColor(
                lightInstance,
                if (prayerLightOn) 1.0f else 0.58f,
                if (prayerLightOn) 0.78f else 0.72f,
                if (prayerLightOn) 0.57f else 0.82f
            )

            val sceneQuality = if (isTvDevice) View.QualityLevel.LOW else View.QualityLevel.MEDIUM
            viewer.view.renderQuality = viewer.view.renderQuality.apply {
                hdrColorBuffer = sceneQuality
            }
            viewer.view.dynamicResolutionOptions = viewer.view.dynamicResolutionOptions.apply {
                enabled = true
                quality = sceneQuality
            }
            viewer.view.antiAliasing = View.AntiAliasing.FXAA
            // Ambient occlusion is expensive on older TV GPUs; preserve it on phones only.
            viewer.view.ambientOcclusionOptions = viewer.view.ambientOcclusionOptions.apply {
                enabled = !isTvDevice
            }
            viewer.view.bloomOptions = viewer.view.bloomOptions.apply {
                enabled = !isTvDevice
            }

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

    private fun applyPrayerPointLights(viewer: ModelViewer, enabled: Boolean) {
        val asset = viewer.asset ?: return
        val manager = viewer.engine.lightManager
        asset.getEntitiesByPrefix("PRAYER_WINDOW_LIGHT_").forEach { entity ->
            val instance = manager.getInstance(entity)
            if (instance != 0) {
                manager.setIntensity(instance, if (enabled) 1200f else 0f)
                manager.setColor(instance, 1.0f, 0.62f, 0.34f)
            }
        }
    }

    fun setPrayerLightOn(enabled: Boolean) {
        prayerLightOn = enabled
        val viewer = modelViewer ?: return
        runCatching {
            val manager = viewer.engine.lightManager
            val instance = manager.getInstance(viewer.light)
            manager.setIntensity(instance, if (enabled) 65000f else 8000f)
            manager.setColor(
                instance,
                if (enabled) 1.0f else 0.58f,
                if (enabled) 0.78f else 0.72f,
                if (enabled) 0.52f else 0.82f
            )
            applyPrayerPointLights(viewer, enabled)
        }.onFailure {
            android.util.Log.w("Ramadan3D", "Prayer lighting update failed", it)
        }
    }

    private fun startRendering() {
        if (modelViewer == null || isRendering) return
        isRendering = true
        lastRenderNanos = 0L
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private fun stopRendering() {
        isRendering = false
        lastRenderNanos = 0L
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    companion object {
        init {
            Utils.init()
        }
    }
}
