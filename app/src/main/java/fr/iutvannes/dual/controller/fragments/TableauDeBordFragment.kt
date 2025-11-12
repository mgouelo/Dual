package fr.iutvannes.dual.controller.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.viewmodel.SessionViewModel
import fr.iutvannes.dual.model.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cette classe représente le fragment du tableau de bord.
 */
class TableauDeBordFragment : Fragment(R.layout.fragment_tableau_de_bord) {

    private val sessionViewModel: SessionViewModel by activityViewModels()

    /**
     * Cette fonction est appelée lorsque la vue du fragment est créée.
     * Elle initialise les interactions avec les vues.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionBtn = view.findViewById<Button>(R.id.launchASession)
        sessionBtn.setOnClickListener {
            sessionViewModel.startSession(requireContext())
        }

        val qrCode = view.findViewById<ImageView>(R.id.qrCodeView)
        qrCode.setBackgroundColor(Color.DKGRAY) // DEBUG
        val sessionUrl = view.findViewById<TextView>(R.id.textUrl)
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            sessionViewModel.url.collect { url ->
                if (url != null) {
                    Toast.makeText(requireContext(), "URL: $url", Toast.LENGTH_LONG).show()
                    qrCode.visibility = View.VISIBLE
                    sessionUrl.visibility = View.VISIBLE
                    sessionUrl.text = url
                    qrCode.setImageBitmap(genererQRCode(url))
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            sessionViewModel.running.collect { running ->
                sessionBtn.isEnabled = !running // si le serveur tourne on désactive le btn
            }
        }
    }

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
