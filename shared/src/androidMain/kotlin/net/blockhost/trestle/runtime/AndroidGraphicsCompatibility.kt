package net.blockhost.trestle.runtime

import android.content.Context
import android.content.pm.PackageManager
import android.opengl.EGL14
import android.opengl.GLES20

internal enum class AndroidGpuFamily(val label: String) {
    ADRENO("Adreno"),
    MALI("Mali"),
    POWER_VR("PowerVR"),
    UNKNOWN("Unknown GPU"),
}

internal data class AndroidGraphicsCompatibility(
    val gpuFamily: AndroidGpuFamily,
    val renderer: String?,
    val vendor: String?,
    val vulkanVersion: Int?,
) {
    val prefersMobileGlues: Boolean
        get() = gpuFamily == AndroidGpuFamily.ADRENO && adrenoModel?.let { it >= 800 } == true

    val isSupported: Boolean
        get() = vulkanVersion != null && vulkanVersion >= MINIMUM_VULKAN_VERSION

    val unavailableReason: String?
        get() = when {
            vulkanVersion == null ->
                "Minecraft 26.2 requires a hardware Vulkan device. ${deviceLabel()} does not report Vulkan support."
            vulkanVersion < MINIMUM_VULKAN_VERSION ->
                "Minecraft 26.2 requires Vulkan 1.2 or newer. ${deviceLabel()} reports ${formatVulkanVersion(vulkanVersion)}."
            else -> null
        }

    fun summary(): String = buildString {
        append(deviceLabel())
        append("; ")
        append(vulkanVersion?.let(::formatVulkanVersion) ?: "Vulkan unavailable")
    }

    private fun deviceLabel(): String = renderer?.takeIf(String::isNotBlank) ?: gpuFamily.label

    private val adrenoModel: Int?
        get() = renderer
            ?.let { ADRENO_MODEL.find(it) }
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()

    companion object {
        const val MINIMUM_VULKAN_VERSION = (1 shl 22) or (2 shl 12)
        private val ADRENO_MODEL = Regex("""(?i)Adreno(?:\s*\(TM\))?\s*(\d{3})""")

        fun gpuFamily(renderer: String?, vendor: String?): AndroidGpuFamily {
            val description = listOfNotNull(renderer, vendor).joinToString(" ").lowercase()
            return when {
                "adreno" in description || "qualcomm" in description -> AndroidGpuFamily.ADRENO
                "mali" in description || "arm" in description -> AndroidGpuFamily.MALI
                "powervr" in description || "imagination" in description -> AndroidGpuFamily.POWER_VR
                else -> AndroidGpuFamily.UNKNOWN
            }
        }

        fun formatVulkanVersion(version: Int): String {
            val major = version ushr 22
            val minor = (version ushr 12) and 0x3ff
            val patch = version and 0xfff
            return "Vulkan $major.$minor.$patch"
        }
    }
}

internal object AndroidGraphicsCompatibilityProbe {
    fun inspect(context: Context): AndroidGraphicsCompatibility {
        val packageManager = context.packageManager
        val vulkanVersion = packageManager.systemAvailableFeatures
            .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
            ?.version
            ?.takeIf { it > 0 }
        val strings = queryOpenGlStrings()
        return AndroidGraphicsCompatibility(
            gpuFamily = AndroidGraphicsCompatibility.gpuFamily(strings.renderer, strings.vendor),
            renderer = strings.renderer,
            vendor = strings.vendor,
            vulkanVersion = vulkanVersion,
        )
    }

    private fun queryOpenGlStrings(): GraphicsStrings {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) return GraphicsStrings()
        val versions = IntArray(2)
        if (!EGL14.eglInitialize(display, versions, 0, versions, 1)) return GraphicsStrings()
        var surface = EGL14.EGL_NO_SURFACE
        var context = EGL14.EGL_NO_CONTEXT
        return try {
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val count = IntArray(1)
            val attributes = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE,
                EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE,
                EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_NONE,
            )
            if (!EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0) || count[0] == 0) {
                return GraphicsStrings()
            }
            val config = requireNotNull(configs[0])
            context = EGL14.eglCreateContext(
                display,
                config,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0,
            )
            surface = EGL14.eglCreatePbufferSurface(
                display,
                config,
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
                0,
            )
            if (
                context == EGL14.EGL_NO_CONTEXT ||
                surface == EGL14.EGL_NO_SURFACE ||
                !EGL14.eglMakeCurrent(display, surface, surface, context)
            ) {
                GraphicsStrings()
            } else {
                GraphicsStrings(
                    renderer = GLES20.glGetString(GLES20.GL_RENDERER),
                    vendor = GLES20.glGetString(GLES20.GL_VENDOR),
                )
            }
        } catch (_: RuntimeException) {
            GraphicsStrings()
        } finally {
            EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
            if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
            EGL14.eglTerminate(display)
        }
    }

    private data class GraphicsStrings(
        val renderer: String? = null,
        val vendor: String? = null,
    )
}
