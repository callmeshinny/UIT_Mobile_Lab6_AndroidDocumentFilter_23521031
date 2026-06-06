package com.callmeshinny.androiddocumentfilter

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import org.opencv.android.OpenCVLoader
import kotlin.math.min

class MainActivity : Activity() {

    private val imagePickRequestCode = 1001

    private lateinit var imageViewOriginal: ImageView
    private lateinit var imageViewResult: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var buttonApplyFilter: Button
    private lateinit var buttonReset: Button

    private var originalBitmap: Bitmap? = null
    private var filteredBitmap: Bitmap? = null
    private val shadowRemovalFilter = ShadowRemovalFilter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!OpenCVLoader.initLocal()) {
            Toast.makeText(this, "OpenCV could not be loaded.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        imageViewOriginal = findViewById(R.id.imageViewOriginal)
        imageViewResult = findViewById(R.id.imageViewResult)
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.textStatus)
        buttonApplyFilter = findViewById(R.id.buttonApplyFilter)
        buttonReset = findViewById(R.id.buttonReset)

        findViewById<Button>(R.id.buttonChooseImage).setOnClickListener {
            openImagePicker()
        }

        buttonApplyFilter.setOnClickListener {
            applyShadowRemoval()
        }

        buttonReset.setOnClickListener {
            resetResult()
        }

        updateButtons()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "Choose document image"), imagePickRequestCode)
    }

    private fun applyShadowRemoval() {
        val source = originalBitmap
        if (source == null) {
            Toast.makeText(this, "Please choose an image first.", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        statusText.text = "Removing shadow..."
        buttonApplyFilter.isEnabled = false

        shadowRemovalFilter.apply(source) { result ->
            progressBar.visibility = View.GONE
            buttonApplyFilter.isEnabled = true

            if (result == null) {
                statusText.text = "Filter failed. Please try another image."
                Toast.makeText(this, "Shadow removal failed.", Toast.LENGTH_LONG).show()
                return@apply
            }

            filteredBitmap = result
            imageViewResult.setImageBitmap(result)
            statusText.text = "Shadow removal completed."
            updateButtons()
        }
    }

    private fun resetResult() {
        filteredBitmap = null
        imageViewResult.setImageResource(android.R.color.transparent)
        statusText.text = "Result cleared. Apply the filter again if needed."
        updateButtons()
    }

    private fun updateButtons() {
        val hasImage = originalBitmap != null
        buttonApplyFilter.isEnabled = hasImage
        buttonReset.isEnabled = filteredBitmap != null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != imagePickRequestCode || resultCode != RESULT_OK) {
            return
        }

        val uri: Uri = data?.data ?: return

        try {
            val bitmap = loadBitmapFromUri(uri)
            originalBitmap = resizeBitmap(bitmap, 1600)
            filteredBitmap = null

            imageViewOriginal.setImageBitmap(originalBitmap)
            imageViewResult.setImageResource(android.R.color.transparent)

            statusText.text = "Image loaded. Tap Remove Shadow to continue."
            updateButtons()
        } catch (exception: Exception) {
            originalBitmap = null
            filteredBitmap = null
            statusText.text = "Could not load image."
            Toast.makeText(this, "Image loading failed.", Toast.LENGTH_LONG).show()
            updateButtons()
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        contentResolver.openInputStream(uri).use { inputStream ->
            return BitmapFactory.decodeStream(inputStream)
                ?: throw IllegalArgumentException("Invalid image")
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        val scale = min(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            .copy(Bitmap.Config.ARGB_8888, true)
    }
}
