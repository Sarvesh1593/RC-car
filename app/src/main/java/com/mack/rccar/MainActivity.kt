package com.mack.rccar


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.FileUtils
import android.util.Base64
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mack.rccar.databinding.ActivityMainBinding
import com.mack.rccar.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class MainActivity : AppCompatActivity() {
    lateinit var labels : List<String>
    private lateinit var binding : ActivityMainBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var imageProcess: ImageProcessor
    val paint = Paint()
    var colors = listOf<Int>(
        Color.BLUE,Color.GREEN,Color.RED,Color.GRAY,Color.BLACK,
        Color.DKGRAY,Color.MAGENTA,Color.YELLOW,Color.RED
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = FirebaseDatabase.getInstance()

        labels = FileUtil.loadLabels(this,"labels.txt")
        imageProcess = ImageProcessor.Builder().add(ResizeOp(300,300,ResizeOp.ResizeMethod.BILINEAR)).build()
        database.getReference("liveVideoStream")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Check if the snapshot contains a value and if that value is a HashMap
                    if (snapshot.exists() && snapshot.value is HashMap<*, *>) {
                        val frameDataMap = snapshot.value as HashMap<*, *>

                        // Assuming the HashMap has a key named "frameData" that contains the base64-encoded frame
                        val frameData: String? = frameDataMap["frameData"] as? String

                        if (frameData != null) {
                            val frameBytes = Base64.decode(frameData, Base64.DEFAULT)
                            val bitmap: Bitmap = BitmapFactory.decodeByteArray(frameBytes, 0, frameBytes.size)
                            binding.videoView.setImageBitmap(bitmap)

                            val model = SsdMobilenetV11Metadata1.newInstance(applicationContext)

                            // Creates inputs for reference.
                            var image = TensorImage.fromBitmap(bitmap)

                            image = imageProcess.process(image)
                            // Runs model inference and gets result.
                            val outputs = model.process(image)
                            val locations = outputs.locationsAsTensorBuffer.floatArray
                            val classes = outputs.classesAsTensorBuffer.floatArray
                            val scores = outputs.scoresAsTensorBuffer.floatArray
                            val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

                            var mutable = bitmap.copy(Bitmap.Config.ARGB_8888,true)

                            val canvas = Canvas(mutable)

                            val h = mutable.height
                            val w = mutable.width

                            paint.textSize = h/15f
                            paint.strokeWidth = h/85f

                            var x =0
                            scores.forEachIndexed { index, fl ->
                                x = index
                                x *= 4
                                if (fl > 0.5) {
                                    paint.setColor(colors.get(index))
                                    paint.style = Paint.Style.STROKE
                                    val rect = RectF(locations.get(x + 1) * w, locations.get(x) * h, locations.get(x + 3) * w, locations.get(x + 2) * h)
                                    canvas.drawRect(rect, paint)

                                    // Set the text typeface to bold
                                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                                    paint.style = Paint.Style.FILL

                                    // Calculate the y-coordinate for the text at the bottom of the box
                                    val textY = rect.bottom

                                    // Draw the text in bold at the bottom
                                    canvas.drawText(labels.get(classes.get(index).toInt()) + " " + fl.toString(), locations.get(x + 1) * w, textY, paint)

                                    // Reset the typeface to the default after drawing the text
                                    paint.typeface = Typeface.DEFAULT
                                }
                            }


                            binding.videoView.setImageBitmap(mutable)
                            // Releases model resources if no longer used.
                            model.close()
                        } else {
                            // Handle the case where "frameData" is not found or is not a String
                            // You can add logging or error handling here
                        }
                    } else {
                        // Handle the case where the snapshot does not contain a HashMap
                        // You can add logging or error handling here
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle onCancelled event if needed
                }
            })

        binding.forward.setOnClickListener {
            database.reference.child("Move").child("move").setValue("F")
        }
        binding.back.setOnClickListener {
            database.reference.child("Move").child("move").setValue("B")
        }
        binding.right.setOnClickListener {
            database.reference.child("Move").child("move").setValue("R")
        }
        binding.left.setOnClickListener {
            database.reference.child("Move").child("move").setValue("L")
        }
        binding.stop.setOnClickListener {
            database.reference.child("Move").child("move").setValue("S")
        }
        binding.location.setOnClickListener {
            startActivity(Intent(this,MapsActivity::class.java))
        }
        binding.Up.setOnClickListener {
            database.reference.child("PTMove").child("direction").setValue("U")
        }
        binding.Down.setOnClickListener {
            database.reference.child("PTMove").child("direction").setValue("D")
        }
        binding.Right.setOnClickListener {
            database.reference.child("PTMove").child("direction").setValue("R")
        }
        binding.Left.setOnClickListener {
            database.reference.child("PTMove").child("direction").setValue("L")
        }
        binding.StopPT.setOnClickListener {
            database.reference.child("PTMove").child("direction").setValue("S")
        }
    }

}



