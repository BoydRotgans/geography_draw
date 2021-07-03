import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.GaussianBlur
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.videoprofiles.ProresProfile
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    configure {
        width = 1920
        height = 1080

    }

    program {

        val atMasks = mutableListOf<ColorBuffer>()
        val atRgbs = mutableListOf<ColorBuffer>()

        var frame = 0

        val sourceTarget = 0
        val blur = true
        val maxFrames = listOf(2009, 2040, 3000)

        extend(ScreenRecorder().apply {
            frameRate = 30
            outputFile = "video/scene_${sourceTarget}_plain.mov"
            maximumFrames = maxFrames.get(sourceTarget).toLong()
        })

        val gblur = GaussianBlur()
        var canvas = colorBuffer(width, height)
        var contour = colorBuffer(width, height)

        val plain = renderTarget(width, height) {
            colorBuffer()
        }

        drawer.isolatedWithTarget(plain) {
            val rgbImage: ColorBuffer = loadImage("data/frames/RGB${sourceTarget}/rgb${frame}.jpg")
            drawer.image(rgbImage)
        }

        val rt = renderTarget(width, height) {
                colorBuffer()
        }

        extend {
            drawer.background(ColorRGBa.BLACK)

            val maskImage: ColorBuffer = loadImage("data/frames/MASK${sourceTarget}/mask${frame}.jpg")
            val rgbImage: ColorBuffer = loadImage("data/frames/RGB${sourceTarget}/rgb${frame}.jpg")


            drawer.isolatedWithTarget(rt) {
                drawer.clear(ColorRGBa.BLACK)
                drawer.translate(-4.0, -4.0)
                drawer.image(rgbImage)
            }



            drawer.isolatedWithTarget(plain) {

                drawer.isolated {
                    drawer.shadeStyle = shadeStyle {
                        fragmentTransform = """
    
                            vec2 textSize = textureSize(p_texture, 0);
                            vec2 uv = (c_screenPosition) / textSize;
                            vec4 image = texture(p_texture,  1.0-vec2(1.0-uv.x, uv.y)).rgba;
    
                            float alpha = 1.0;
                            if(x_fill.r < 0.5) {
                                alpha = 0.0;
                            }
                            x_fill.rgba = vec4( image.r, image.g, image.b, alpha);
                            """.trimIndent()
                        parameter("texture", rt.colorBuffer(0))
                    }
                    drawer.image(maskImage)
                }

            }

            drawer.image(plain.colorBuffer(0))

                frame++

        }
    }
}
