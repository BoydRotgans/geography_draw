import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.gui.addTo
import org.openrndr.extra.videoprofiles.ProresProfile
import org.openrndr.ffmpeg.ScreenRecorder

fun main() = application {
    configure {
        width = 1920
        height = 1080

    }

    program {



        val frameCount = 10
        val atMasks = mutableListOf<ColorBuffer>()
        val atRgbs = mutableListOf<ColorBuffer>()

        var frame = 0


        extend(ScreenRecorder().apply {
            frameRate = 30
            profile = ProresProfile()
            outputFile = "video/scene_1_solid.mov"
            maximumFrames = 2010
        })


        extend {
            drawer.background(ColorRGBa.BLACK)

            val maskImage: ColorBuffer = loadImage("data/frames/MASK1/mask${frame}.jpg")
            val rgbImage: ColorBuffer = loadImage("data/frames/RGB1/rgb${frame}.jpg")

            var rt = renderTarget(width, height) {
                colorBuffer()
            }

            drawer.isolatedWithTarget(rt) {
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


            drawer.image(atRgbs.last())


                drawer.isolated {
                    atMasks.forEachIndexed { index, colorBuffer ->
                        if (index % 5 == 0) {
//                    blur.apply {
//                        spread =  5.0 + Math.abs(Math.sin(seconds*0.1+index*0.1)) * 5.0
//                    }
//                    blur.apply( cbBackList.get( index), canvas)
                            drawer.shadeStyle = shadeStyle {
                                fragmentTransform = """
                        vec2 textSize = textureSize(p_texture, 0);
                        //vec2 uv = (c_screenPosition + vec2(p_count, 0.0) ) /  textSize;
                        //vec2 uv = (c_screenPosition - vec2( (cos(((p_count*0.1)-p_seconds*0.01)))*10.0, 0.0)) / textSize;
                        vec2 uv = (c_screenPosition) /  textSize;

                        vec4 image = texture(p_texture,  1.0-vec2(1.0-uv.x, uv.y)).rgba;
                        float colorFill = 1.0  - (sin((p_seconds*0.01)+(p_count*0.1))) *0.25 ; //  1.0; //

                        float alpha = 1.0;
                        if(x_fill.r < 0.5) {
                            alpha = 0.0;
                        }

                        x_fill.rgba = vec4( image.r*colorFill,  image.g*colorFill, image.b*colorFill, alpha);
                        """.trimIndent()
                                parameter("texture", atRgbs.get(index))
                                parameter("count", index)
                                parameter("seconds", seconds)
                                parameter("fillIn", ColorRGBa.PINK)
                            }
//                    blur.apply {
//                        window = 5
//                        spread =  10.0
//                        gain = 1.0
//                gain = 1.5
//                    }
//                    blur.apply( colorBuffer, countour)


                            drawer.image(colorBuffer)
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
                            if(x_fill.r < 0.5) {
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
