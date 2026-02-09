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
 * Fragment pour afficher le tableau de bord.
 *
 * @see SessionViewModel
 * @see AppDatabase
 * @see R.layout.fragment_tableau_de_bord
 */
class TableauDeBordFragment : Fragment(R.layout.fragment_tableau_de_bord) {

    /* Variable d'instance */
    private var countInitial = 0

    /* Variable d'instance */
    private val sessionViewModel: SessionViewModel by activityViewModels()

    /**
     * Cette fonction est appelée lorsque la vue du fragment est créée.
     * Elle initialise les interactions avec les vues.
     *
     * @param view La vue du fragment.
     * @param savedInstanceState Les données conservées lors de l'état de l'activité.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionBtn = view.findViewById<Button>(R.id.launchASession)

        // Gestion du clic sur le bouton de démarrage de session
        sessionBtn.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                countInitial = withContext(Dispatchers.IO) {
                    DatabaseProvider.db.resultatDao().getCount()
                }
                //On lance la session
                sessionViewModel.startSession(requireContext())
            }
        }

        val nbResultat = view.findViewById<TextView>(R.id.text_resultats_count)

        val btnExport = view.findViewById<Button>(R.id.btn_download_excel) // L'ID de ton bouton XML

        // Ouverture d'une coroutine dans le thread IO pour compter les résultats
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
        // Ouverture d'une coroutine dans le thread IO pour générer le QR Code
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
        // Ouverture d'une coroutine dans le thread IO pour gérer le bouton de démarrage de session
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            sessionViewModel.running.collect { running ->
                sessionBtn.isEnabled = !running // si le serveur tourne on désactive le btn
            }
        }

        // Gestion du clic sur le bouton d'export
        btnExport.setOnClickListener {
            val currentUrl = sessionViewModel.url.value
            if (currentUrl != null) {
                //On construit l'URL de téléchargement
                val downloadUrl = "$currentUrl/api/admin/export"

                //On ouvre le navigateur de la tablette pour lancer le téléchargement
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(downloadUrl))
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "Démarrez une session d'abord", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Génère un QR Code à partir d'un texte.
     *
     * @param text Le texte à encoder dans le QR Code.
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
