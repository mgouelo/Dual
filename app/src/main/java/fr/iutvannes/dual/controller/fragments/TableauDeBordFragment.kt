package fr.iutvannes.dual.controller.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.viewmodel.SessionViewModel
import fr.iutvannes.dual.model.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment to display the dashboard.
 *
 * @see SessionViewModel
 * @see AppDatabase
 * @see R.layout.fragment_tableau_de_bord
 */
class TableauDeBordFragment : Fragment(R.layout.fragment_tableau_de_bord) {

    /* Variable to keep track of the initial count */
    private var countInitial = 0

    /* Variable for the SessionViewModel instance */
    private val sessionViewModel: SessionViewModel by activityViewModels()

    /**
     * This function is called when the fragment view is created.
     * It initializes interactions with views.
     *
     * @param view The fragment view.
     * @param savedInstanceState The data saved during the activity's state.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionBtn = view.findViewById<Button>(R.id.launchASession)

        // Managing the click on the session start button
        sessionBtn.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                countInitial = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.resultatDao().getCount()
                }
                // We start the session
                sessionViewModel.startSession(requireContext())
            }
        }

        val nbResultat = view.findViewById<TextView>(R.id.text_resultats_count)

        val btnExport = view.findViewById<Button>(R.id.btn_download_excel) // Your button's XML ID

        // Opening a coroutine in the I/O thread to count the results
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (sessionViewModel.running.value) {
                    val totalEnBase = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.resultatDao().getCount()
                    }
                    val resultatsSeance = totalEnBase - countInitial
                    nbResultat.text = "Résultats reçus : $resultatsSeance"
                }
                kotlinx.coroutines.delay(2000)
            }
        }

        val qrCode = view.findViewById<ImageView>(R.id.qrCodeView)
        qrCode.setBackgroundColor(Color.DKGRAY) // DEBUG
        val sessionUrl = view.findViewById<TextView>(R.id.textUrl)
        // Opening a coroutine in the IO thread to generate the QR code
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            sessionViewModel.url.collect { url ->
                if (url != null) {
                    Toast.makeText(requireContext(), "URL: $url", Toast.LENGTH_LONG).show()
                    qrCode.visibility = View.VISIBLE
                    sessionUrl.visibility = View.VISIBLE
                    nbResultat.visibility = View.VISIBLE
                    btnExport.visibility = View.VISIBLE
                    sessionUrl.text = url
                    qrCode.setImageBitmap(genererQRCode(url))
                }
            }
        }
        // Opening a coroutine in the I/O thread to handle the session start button
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            sessionViewModel.running.collect { running ->
                sessionBtn.isEnabled = !running // If the server is running, we disable the button.
            }
        }

        // Managing clicks on the export button
        btnExport.setOnClickListener {
            val currentUrl = sessionViewModel.url.value
            if (currentUrl != null) {
                // We construct the download URL
                val downloadUrl = "$currentUrl/api/admin/export"

                // We open the tablet's browser to start the download
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Démarrez une session d'abord", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Generates a QR code from text.
     *
     * @param text The text to encode in the QR code.
     * @return The generated QR code as a Bitmap.
     */
    private fun genererQRCode(text: String): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bmp
    }
}
