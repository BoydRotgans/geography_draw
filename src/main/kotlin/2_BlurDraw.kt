import org.openrndr.application
import org.openrndr.color.ColorRGBa
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

        val sourceTarget = 1
        val blur = true
        val maxFrames = listOf(2009, 2040, 3000)

        extend(ScreenRecorder().apply {
            frameRate = 30
            profile = ProresProfile()
            outputFile = "video/scene_${sourceTarget}_blur_${blur}.mov"
            maximumFrames = maxFrames.get(sourceTarget).toLong()
        })

        val gblur = GaussianBlur()
        var canvas = colorBuffer(width, height)
        var contour = colorBuffer(width, height)

        extend {
            drawer.background(ColorRGBa.BLACK)

            val maskImage: ColorBuffer = loadImage("data/frames/MASK${sourceTarget}/mask${frame}.jpg")
            val rgbImage: ColorBuffer = loadImage("data/frames/RGB${sourceTarget}/rgb${frame}.jpg")

            val rt = renderTarget(width, height) {
                colorBuffer()
            }

            drawer.isolatedWithTarget(rt) {
                drawer.clear(ColorRGBa.BLACK)
                drawer.translate(-4.0, -4.0)
                drawer.image(rgbImage)
            }

            atMasks.add(maskImage)
            atRgbs.add(rt.colorBuffer(0))

            if( atMasks.size>150) {
                atMasks[0].destroy()
                atRgbs[0].destroy()

                atMasks.removeAt(0)
                atRgbs.removeAt(0)
            }


                drawer.image(rgbImage)


                drawer.isolated {
                    atMasks.forEachIndexed { index, colorBuffer ->
                        if (index % 2 == 0) {
                            gblur.apply {
                        spread =  15.0 + Math.abs(Math.sin(seconds*0.1+index*0.1)) * 15.0
                    }
                    gblur.apply( atRgbs.get(index), canvas)
                            drawer.shadeStyle = shadeStyle {
                                fragmentTransform = """
                        vec2 textSize = textureSize(p_texture, 0);
                        //vec2 uv = (c_screenPosition + vec2(p_count, 0.0) ) /  textSize;
                        vec2 uv = (c_screenPosition - vec2( (cos(((p_count*0.04)-p_seconds*0.1)))*25.0, 0.0)) / textSize;
                        //vec2 uv = (c_screenPosition) /  textSize;

                        vec4 image = texture(p_texture,  1.0-vec2(1.0-uv.x, uv.y)).rgba;
                        float colorFill = 1.0  - (sin((p_seconds*0.01)+(p_count*0.1))) *0.25 ; //  1.0; //

                        float alpha = 1.0;
                        if(x_fill.r > 0.5) {
                            alpha = 0.0;
                        }

                        x_fill.rgba = vec4( image.r*colorFill,  image.g*colorFill, image.b*colorFill, alpha);
                        """.trimIndent()
                                parameter("texture", canvas)
                                parameter("count", index)
                                parameter("seconds", seconds)
                                parameter("fillIn", ColorRGBa.PINK)
                            }
                            gblur.apply {

                                spread =  1.0 + Math.abs(Math.sin(seconds*0.1+index*0.1)) * 5.0

                            }
                            gblur.apply( colorBuffer, contour)
                            drawer.image(contour)
                        }
                    }
                }

                drawer.isolated {
                    drawer.shadeStyle = shadeStyle {
                        fragmentTransform = """
    
                            vec2 textSize = textureSize(p_texture, 0);
                            vec2 uv = (c_screenPosition) / textSize;
                            vec4 image = texture(p_texture,  1.0-vec2(1.0-uv.x, uv.y)).rgba;
    
                            float alpha = 1.0;
                            if(x_fill.r > 0.5) {
                                alpha = 0.0;
                            }
                            x_fill.rgba = vec4( image.r, image.g, image.b, alpha);
                            """.trimIndent()
                        parameter("texture", atRgbs.last())
                    }
                    drawer.image(atMasks.last())
                }


                frame++

        }
    }
}
