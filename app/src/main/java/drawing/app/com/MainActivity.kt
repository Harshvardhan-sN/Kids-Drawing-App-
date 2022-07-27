package drawing.app.com

import android.app.ActionBar
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dailog_brush_size.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.invoke.ConstantCallSite
import java.util.jar.Manifest


class MainActivity : AppCompatActivity() {

    private var drawingView : DrawingView? = null
    private var mImageButtonCur: ImageButton? = null
    var customProgress: Dialog? = null

    val openGallaryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imgBackground: ImageView = findViewById(R.id.frames_image)
                imgBackground.setImageURI(result.data?.data)
            }
        }

    val gallaryExcess: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
                if(isGranted) {
                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGallaryLauncher.launch(pickIntent)
                }
                else{
                    if(permissionName==android.Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this,"Opps you just Denied the Permission :(",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val iBrush: ImageButton = findViewById(R.id.brush_size)
        val btnGallary: ImageButton =findViewById(R.id.photo_btn)
        val linearLayoutPaintColor = findViewById<LinearLayout>(R.id.color_pallet)
        val btnUndo: ImageButton = findViewById(R.id.undo_btn)
        val btnRedo: ImageButton = findViewById(R.id.redo_btn)
        val btnSave: ImageButton = findViewById(R.id.btn_save)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(5.toFloat())

        mImageButtonCur = linearLayoutPaintColor[8] as ImageButton
        mImageButtonCur!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_selected)
        )

        iBrush.setOnClickListener {
            showBSizeDialog()
        }

        undo_btn.setOnClickListener{
            drawingView?.onClickUndo()
        }
        redo_btn.setOnClickListener{
            drawingView?.onClickRedo()
        }

        btnGallary.setOnClickListener { view ->
            RequestStoragePermission()
        }

        btnSave.setOnClickListener{
            showCustomProgress()
            if(isReadStorageAllowed()){
                lifecycleScope.launch {
                    val finalDrawingView: FrameLayout = findViewById(R.id.frame_layout)
                    val myBitmap: Bitmap = getBitmapFromView(finalDrawingView)
                    saveBitmapFile(myBitmap)
                }
            }
        }
    }

    private fun showBSizeDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView((R.layout.dailog_brush_size))
        brushDialog.setTitle("Brush Size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener {
            drawingView?.setSizeForBrush(3.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener {
            drawingView?.setSizeForBrush(7.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener {
            drawingView?.setSizeForBrush(13.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View){
        if(view !== mImageButtonCur){
            val imgBtn = view as ImageButton
            val clTag = imgBtn.tag.toString()
            drawingView?.setColor(clTag)
            imgBtn.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_selected)
            )
            mImageButtonCur?.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet)
            )
            mImageButtonCur = view
        }
    }
    private fun showRationaleDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){
                    dialog, _->dialog.dismiss()
            }
        builder.create().show()
    }
    private fun RequestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE))
                {
            showRationaleDialog("Kids Drawing App","Kids Drawing App " +
                    "needs to Access Your External Storage")
        }
        else {
            gallaryExcess.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ))
        }
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,
        android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,
            view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        } else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap!=null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val fIn = File(
                        externalCacheDir?.absoluteFile.toString() +
                                File.separator + "KidsDrawingApp" + System.currentTimeMillis() / 1000 + ".png"
                    )
                    val fOut = FileOutputStream(fIn)
                    fOut.write(bytes.toByteArray())
                    fOut.close()

                    result = fIn.absolutePath

                    runOnUiThread {
                        cancelProgressBar()
                        if (result.isNotEmpty()) {
                            Toast.makeText(
                                this@MainActivity,
                                "File Saved Successfully :) $result", Toast.LENGTH_SHORT
                            ).show()
                            shareFiles(result)
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "Something went Wrong!! :(", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return  result
    }

    private fun showCustomProgress() {
        customProgress = Dialog(this)
        customProgress?.setContentView(R.layout.progress_dialog)
        customProgress?.show()
    }
    private  fun cancelProgressBar(){
        if(customProgress != null){
            customProgress?.dismiss()
            customProgress = null
        }
    }

    private fun shareFiles(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path, uri ->
            val shareInit = Intent()
            shareInit.action =  Intent.ACTION_SEND
            shareInit.putExtra(Intent.EXTRA_STREAM,uri)
            shareInit.type = "image/png"
            startActivity(Intent.createChooser(shareInit,"Share"))
        }
    }

}