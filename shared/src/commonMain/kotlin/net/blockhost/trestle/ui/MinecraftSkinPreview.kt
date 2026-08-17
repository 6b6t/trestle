package net.blockhost.trestle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.isActive
import net.blockhost.trestle.auth.SkinVariant
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun MinecraftSkinHead(
    texture: ByteArray?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit,
) {
    val image = remember(texture) {
        texture?.let { bytes -> runCatching { bytes.decodeToImageBitmap() }.getOrNull() }
            ?.takeIf { it.width >= 48 && it.height >= 16 }
    }
    Box(
        modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (image == null) {
            fallback()
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val destinationSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                drawImage(
                    image = image,
                    srcOffset = BaseHeadOffset,
                    srcSize = HeadSize,
                    dstSize = destinationSize,
                    filterQuality = FilterQuality.None,
                )
                drawImage(
                    image = image,
                    srcOffset = HeadOverlayOffset,
                    srcSize = HeadSize,
                    dstSize = destinationSize,
                    filterQuality = FilterQuality.None,
                )
            }
        }
    }
}

private val BaseHeadOffset = IntOffset(8, 8)
private val HeadOverlayOffset = IntOffset(40, 8)
private val HeadSize = IntSize(8, 8)

/**
 * A small software renderer for Minecraft's 64×64 skin model. The cuboid and UV layout follows
 * skinview3d's MIT-licensed model implementation, ported to Compose so it works on Android and JVM.
 */
@Composable
fun MinecraftSkinPreview(
    texture: ByteArray?,
    variant: SkinVariant,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    animate: Boolean = true,
    emptyLabel: String = "Skin preview unavailable",
) {
    val pixels = remember(texture) {
        texture?.let { bytes -> runCatching { bytes.decodeToImageBitmap().toPixelMap() }.getOrNull() }
    }
    var yaw by remember(texture) { mutableFloatStateOf(-0.42f) }
    var animationTime by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pixels, animate) {
        if (pixels == null || !animate) return@LaunchedEffect
        val startedAt = withFrameNanos { it }
        while (isActive) {
            animationTime = withFrameNanos { now -> (now - startedAt) / 1_000_000_000f }
        }
    }

    Box(
        modifier
            .background(PreviewBackground)
            .semantics { contentDescription = "Interactive 3D Minecraft skin preview" }
            .then(
                if (interactive) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            yaw += dragAmount.x * 0.012f
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        if (pixels != null) {
            Canvas(Modifier.fillMaxSize()) {
                val pose = sin(animationTime * 1.8f) * if (animate) 0.13f else 0f
                val polygons = buildSkinPolygons(
                    pixels = pixels,
                    slim = variant == SkinVariant.SLIM,
                    yaw = yaw,
                    pose = pose,
                    canvasWidth = size.width,
                    canvasHeight = size.height,
                )
                polygons.sortedBy(Polygon::depth).forEach { polygon ->
                    val path = Path().apply {
                        moveTo(polygon.points[0].x, polygon.points[0].y)
                        polygon.points.drop(1).forEach { lineTo(it.x, it.y) }
                        close()
                    }
                    drawPath(path, polygon.color)
                }
            }
        } else {
            Text(
                emptyLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = Vec3(x * scale, y * scale, z * scale)
}

private data class Cuboid(
    val minimum: Vec3,
    val maximum: Vec3,
    val baseUv: UvBox,
    val overlayUv: UvBox?,
    val pivotY: Float,
    val rotationX: Float = 0f,
    val mirrorU: Boolean = false,
    val head: Boolean = false,
)

private data class UvBox(val u: Int, val v: Int, val width: Int, val height: Int, val depth: Int)
private data class UvFace(val x: Int, val y: Int, val width: Int, val height: Int, val mirrorU: Boolean = false)
private data class Face(
    val origin: Vec3,
    val horizontal: Vec3,
    val vertical: Vec3,
    val uv: UvFace,
    val brightness: Float,
)

private data class Polygon(val points: List<Offset>, val color: Color, val depth: Float)

private fun buildSkinPolygons(
    pixels: PixelMap,
    slim: Boolean,
    yaw: Float,
    pose: Float,
    canvasWidth: Float,
    canvasHeight: Float,
): List<Polygon> {
    val modern = pixels.height >= 64
    val armWidth = if (slim) 3f else 4f
    val cuboids = listOf(
        Cuboid(Vec3(-4f, 24f, -4f), Vec3(4f, 32f, 4f), UvBox(0, 0, 8, 8, 8), UvBox(32, 0, 8, 8, 8), 24f, head = true),
        Cuboid(Vec3(-4f, 12f, -2f), Vec3(4f, 24f, 2f), UvBox(16, 16, 8, 12, 4), UvBox(16, 32, 8, 12, 4).takeIf { modern }, 24f),
        Cuboid(Vec3(-4f - armWidth, 12f, -2f), Vec3(-4f, 24f, 2f), UvBox(40, 16, armWidth.toInt(), 12, 4), UvBox(40, 32, armWidth.toInt(), 12, 4).takeIf { modern }, 24f, rotationX = pose),
        Cuboid(Vec3(4f, 12f, -2f), Vec3(4f + armWidth, 24f, 2f), if (modern) UvBox(32, 48, armWidth.toInt(), 12, 4) else UvBox(40, 16, armWidth.toInt(), 12, 4), UvBox(48, 48, armWidth.toInt(), 12, 4).takeIf { modern }, 24f, rotationX = -pose, mirrorU = !modern),
        Cuboid(Vec3(-4f, 0f, -2f), Vec3(0f, 12f, 2f), UvBox(0, 16, 4, 12, 4), UvBox(0, 32, 4, 12, 4).takeIf { modern }, 12f, rotationX = -pose),
        Cuboid(Vec3(0f, 0f, -2f), Vec3(4f, 12f, 2f), if (modern) UvBox(16, 48, 4, 12, 4) else UvBox(0, 16, 4, 12, 4), UvBox(0, 48, 4, 12, 4).takeIf { modern }, 12f, rotationX = pose, mirrorU = !modern),
    )
    val scale = min(canvasWidth / 22f, canvasHeight / 38f)
    val center = Offset(canvasWidth / 2f, canvasHeight * 0.52f)
    return buildList {
        cuboids.forEach { cuboid ->
            addCuboid(pixels, cuboid, cuboid.baseUv, 0f, yaw, scale, center)
            cuboid.overlayUv?.let { overlay ->
                addCuboid(pixels, cuboid, overlay, if (cuboid.head) 0.5f else 0.25f, yaw, scale, center)
            }
        }
    }
}

private fun MutableList<Polygon>.addCuboid(
    pixels: PixelMap,
    cuboid: Cuboid,
    uvBox: UvBox,
    inflate: Float,
    yaw: Float,
    scale: Float,
    center: Offset,
) {
    val minimum = cuboid.minimum - Vec3(inflate, inflate, inflate)
    val maximum = cuboid.maximum + Vec3(inflate, inflate, inflate)
    faces(minimum, maximum, uvBox).forEach { face ->
        for (v in 0 until face.uv.height) {
            for (u in 0 until face.uv.width) {
                val sourceU = if (face.uv.mirrorU.xor(cuboid.mirrorU)) face.uv.width - u - 1 else u
                val color = pixels[face.uv.x + sourceU, face.uv.y + v]
                if (color.alpha <= 0.01f) continue
                val u0 = u.toFloat() / face.uv.width
                val u1 = (u + 1f) / face.uv.width
                val v0 = v.toFloat() / face.uv.height
                val v1 = (v + 1f) / face.uv.height
                val corners = listOf(
                    face.origin + face.horizontal * u0 + face.vertical * v0,
                    face.origin + face.horizontal * u1 + face.vertical * v0,
                    face.origin + face.horizontal * u1 + face.vertical * v1,
                    face.origin + face.horizontal * u0 + face.vertical * v1,
                ).map { point -> transform(point, cuboid.pivotY, cuboid.rotationX, yaw) }
                val projected = corners.map { project(it, scale, center) }
                val shaded = color.shade(face.brightness)
                add(Polygon(projected, shaded, corners.sumOf { it.z.toDouble() }.toFloat() / 4f))
            }
        }
    }
}

private fun faces(minimum: Vec3, maximum: Vec3, box: UvBox): List<Face> {
    val top = UvFace(box.u + box.depth, box.v, box.width, box.depth)
    val bottom = UvFace(box.u + box.width + box.depth, box.v, box.width, box.depth, mirrorU = true)
    val left = UvFace(box.u, box.v + box.depth, box.depth, box.height)
    val front = UvFace(box.u + box.depth, box.v + box.depth, box.width, box.height)
    val right = UvFace(box.u + box.width + box.depth, box.v + box.depth, box.depth, box.height)
    val back = UvFace(box.u + box.width + box.depth * 2, box.v + box.depth, box.width, box.height, mirrorU = true)
    val width = maximum.x - minimum.x
    val height = maximum.y - minimum.y
    val depth = maximum.z - minimum.z
    return listOf(
        Face(Vec3(minimum.x, maximum.y, maximum.z), Vec3(width, 0f, 0f), Vec3(0f, 0f, -depth), top, 1.08f),
        Face(Vec3(minimum.x, minimum.y, minimum.z), Vec3(width, 0f, 0f), Vec3(0f, 0f, depth), bottom, 0.62f),
        Face(Vec3(minimum.x, maximum.y, minimum.z), Vec3(0f, 0f, depth), Vec3(0f, -height, 0f), left, 0.76f),
        Face(Vec3(minimum.x, maximum.y, maximum.z), Vec3(width, 0f, 0f), Vec3(0f, -height, 0f), front, 1f),
        Face(Vec3(maximum.x, maximum.y, maximum.z), Vec3(0f, 0f, -depth), Vec3(0f, -height, 0f), right, 0.88f),
        Face(Vec3(maximum.x, maximum.y, minimum.z), Vec3(-width, 0f, 0f), Vec3(0f, -height, 0f), back, 0.7f),
    )
}

private fun transform(point: Vec3, pivotY: Float, rotationX: Float, yaw: Float): Vec3 {
    val localY = point.y - pivotY
    val posed = Vec3(
        point.x,
        localY * cos(rotationX) - point.z * sin(rotationX) + pivotY,
        localY * sin(rotationX) + point.z * cos(rotationX),
    )
    val centered = posed - Vec3(0f, 16f, 0f)
    val pitched = Vec3(centered.x, centered.y * cos(-0.08f) - centered.z * sin(-0.08f), centered.y * sin(-0.08f) + centered.z * cos(-0.08f))
    return Vec3(
        pitched.x * cos(yaw) + pitched.z * sin(yaw),
        pitched.y,
        -pitched.x * sin(yaw) + pitched.z * cos(yaw),
    )
}

private fun project(point: Vec3, scale: Float, center: Offset): Offset {
    val perspective = 54f / (54f - point.z)
    return Offset(
        center.x + point.x * scale * perspective,
        center.y - point.y * scale * perspective,
    )
}

private fun Color.shade(amount: Float): Color = Color(
    red = (red * amount).coerceIn(0f, 1f),
    green = (green * amount).coerceIn(0f, 1f),
    blue = (blue * amount).coerceIn(0f, 1f),
    alpha = alpha,
)

private val PreviewBackground = Color(0xFF171613)
