package com.voidhash.android_3d_rendering

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.utils.Float3
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Mat4
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import com.google.android.filament.utils.rotation
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity(), Choreographer.FrameCallback {

    //Provides a dedicated drawing surface embedded inside of a view hierarchy.
    private lateinit var surfaceView: SurfaceView

    //Coordinates the timing of animations, input and drawing.
    private lateinit var choreographer: Choreographer

    /* Owns a Filament engine, renderer, swapchain, view, and scene.
    Helps render glTF models into a SurfaceView. */
    private lateinit var modelViewer: ModelViewer

    private val startTime = System.nanoTime()

    private val animationSpeed = 10f

    //This loads in the native code for the filament-utils layer.
    companion object {
        init {
            Utils.init()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        surfaceView = SurfaceView(this).apply { setContentView(this) }
        choreographer = Choreographer.getInstance()
        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer)

        loadGlb("models/DamagedHelmet/glTF-Binary/DamagedHelmet")
        loadEnvironment("skybox/venetian_crossroads_2k/venetian_crossroads_2k")

        //Create a default Skybox
        //modelViewer.scene.skybox = Skybox.Builder().build(modelViewer.engine)
    }

    //Called when a new display frame is being rendered.
    override fun doFrame(frameTimeNanos: Long) {
        //Posts a frame callback to run on the next frame.
        val seconds = (frameTimeNanos - startTime).toDouble() / 1_000_000_000
        choreographer.postFrameCallback(this)

        //Enable animation to our GLTF
        modelViewer.animator?.apply {
            if (animationCount > 0) {
                applyAnimation(0, seconds.toFloat())
            }
            updateBoneMatrices()
        }

        // Reset the root transform, then rotate it around the Z axis.
        modelViewer.asset?.apply {
            //Transform the root node of the scene such that it fits into a 1x1x1 cube centered at the origin.
            modelViewer.transformToUnitCube()
            val rootTransform = this.root.getTransform()
            val degrees = animationSpeed * seconds.toFloat()
            val zAxis = Float3(0f, 0f, 1f)
            this.root.setTransform(rootTransform * rotation(zAxis, degrees))
        }

        modelViewer.render(frameTimeNanos)
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(this)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(this)
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun loadGlb(name: String) {
        val buffer = readAsset("${name}.glb")
        modelViewer.loadModelGlb(buffer)
        //Transform the root node of the scene such that it fits into a 1x1x1 cube centered at the origin.
        modelViewer.transformToUnitCube()
    }

    private fun loadGltf(name: String) {
        val buffer = readAsset("models/${name}.gltf")
        modelViewer.loadModelGltf(buffer) { uri -> readAsset("models/$uri") }
        //Transform the root node of the scene such that it fits into a 1x1x1 cube centered at the origin.
        modelViewer.transformToUnitCube()
    }

    private fun loadEnvironment(ibl: String) {
        // Create the indirect light source and add it to the scene.
        val bufferLight = readAsset("${ibl}_ibl.ktx")
        KTX1Loader.createIndirectLight(modelViewer.engine, bufferLight).apply {
            intensity = 50_000f
            modelViewer.scene.indirectLight = this
        }

        // Create the sky box and add it to the scene.
        val bufferSkybox = readAsset("${ibl}_skybox.ktx")
        KTX1Loader.createSkybox(modelViewer.engine, bufferSkybox).apply {
            modelViewer.scene.skybox = this
        }
    }

    // Extract the transformable component from the root and use that instead
    private fun Int.getTransform(): Mat4 {
        val tm = modelViewer.engine.transformManager
        return Mat4.of(*tm.getTransform(tm.getInstance(this), FloatArray(32)))
    }

    // Set the transformable component from the root
    private fun Int.setTransform(mat: Mat4) {
        val tm = modelViewer.engine.transformManager
        tm.setTransform(tm.getInstance(this), mat.toFloatArray())
    }

    // This function is to show how can we use the ECS to manipulate our model
    // in order to work, you need to know the entity name that you wants to disable
    // try to use BusterDrone as a model and pass "Scheibe_Boden_0" as entityName
    private fun disableModelParts(entityName: String) {
        val asset = modelViewer.asset!!
        val rm = modelViewer.engine.renderableManager

        for (entity in asset.entities) {

            val renderable = rm.getInstance(entity)

            if (renderable == 0) {
                continue
            }

            if (asset.getName(entity) == entityName) {
                rm.setLayerMask(renderable, 0xff, 0x00)
            }

            val material = rm.getMaterialInstanceAt(renderable, 0)
            material.setParameter("emissiveFactor", 0f, 0f, 0f)
        }
    }

}