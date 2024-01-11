# android-3d-rendering
An implementation of a real-time rendering engine using Filament library. Filament is a Google-developed, open source, physically-based rendering engine. It is ideal for adding 3D features to your app without the need for a full-fledged game engine. Filament supports a wide range of platforms (iOS, Android, web), but is best suited for Android. Its small core library allows for fast loading, which is important for creating smooth mobile experiences.

The various Filament layers that are available with Maven are summarized below.

- **filament**: Core renderer.
- **gltfio**: Materials and importers for glTF 2.0 model files.
- **filament-utils**: High-level Android utilities, including helpers for Kotlin.
- **filamat**: Enables generation of materials at run time.

![filament](https://github.com/VoidHash/android-3d-rendering/assets/8929413/4f84ad57-2a04-4e3e-bc93-9256bf5b69a8)

---

### Easy mode

1. Setup Filament dependencies
```python
dependencies {  
  def filamentVersion = "1.49.2"  
  implementation("com.google.android.filament:filament-android:$filamentVersion")
  implementation("com.google.android.filament:gltfio-android:$filamentVersion")
  implementation("com.google.android.filament:filament-utils-android:$filamentVersion")
}
```

2. Start Filament Utils in your Activity
```kotlin
//This loads in the native code for the filament-utils layer.
// ...
companion object {
    init {
        Utils.init()
    }
}
// ...
```

3. Setup you Filament basic variables with a SurfaceView and implement Choreographer.FrameCallback
```kotlin
class MainActivity : AppCompatActivity(), Choreographer.FrameCallback {

  //Provides a dedicated drawing surface embedded inside of a view hierarchy.
  private lateinit var surfaceView: SurfaceView

  //Coordinates the timing of animations, input and drawing.
  private lateinit var choreographer: Choreographer

  /* Owns a Filament engine, renderer, swapchain, view, and scene.
  Helps render glTF models into a SurfaceView. */
  private lateinit var modelViewer: ModelViewer

  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      surfaceView = SurfaceView(this).apply { setContentView(this) }
      choreographer = Choreographer.getInstance()
      modelViewer = ModelViewer(surfaceView)
      surfaceView.setOnTouchListener(modelViewer)
  }

  //Called when a new display frame is being rendered.
  override fun doFrame(frameTimeNanos: Long) {
    choreographer.postFrameCallback(this)
       modelViewer.render(currentTime)
  }
```

3. Add choreographer callback in the Android lifecycle events
```kotlin
// ...
override fun onResume() {
   super.onResume()
   choreographer.postFrameCallback(frameCallback)
}

override fun onPause() {
   super.onPause()
   choreographer.removeFrameCallback(frameCallback)
}

override fun onDestroy() {
   super.onDestroy()
   choreographer.removeFrameCallback(frameCallback)
}
// ...
```

4. Create a basic Skybox and load your GLTF model
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
   // ...

   //Create a default Skybox
   modelViewer.scene.skybox = Skybox.Builder().build(modelViewer.engine)
   loadGlb("DamagedHelmet")
}

private fun loadGlb(name: String) {
   //Read an asset from Assets folder
   val buffer = readAsset("models/${name}.glb")
   modelViewer.loadModelGlb(buffer)
   modelViewer.transformToUnitCube()
}

private fun readAsset(assetName: String): ByteBuffer {
   val input = assets.open(assetName)
   val bytes = ByteArray(input.available())
   input.read(bytes)
   return ByteBuffer.wrap(bytes)
}
```

5. Run in a physical device (emulator is not able to run Filament application)


