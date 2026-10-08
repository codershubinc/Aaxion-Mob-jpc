import re

with open("app/src/main/java/com/codershubinc/aaxion_mob/MainActivity.kt", "r") as f:
    content = f.read()

# Fix onWindowFocusChanged
content = content.replace("""    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        val imageLoader = ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.1)
                    .build()
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
        }
    }""", """    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            updateSystemBars()
        }
    }""")

# Fix hideSystemBars -> updateSystemBars
content = content.replace("""    private fun hideSystemBars() {
        val imageLoader = ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.1)
                    .build()
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }""", """    private fun updateSystemBars() {
        val configuration = resources.configuration
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isTablet = configuration.smallestScreenWidthDp >= 600

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isLandscape || isTablet) {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }""")

content = content.replace("hideSystemBars()", "updateSystemBars()")

with open("app/src/main/java/com/codershubinc/aaxion_mob/MainActivity.kt", "w") as f:
    f.write(content)
