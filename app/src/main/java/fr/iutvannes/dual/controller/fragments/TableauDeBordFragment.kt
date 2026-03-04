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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import fr.iutvannes.dual.R
import fr.iutvannes.dual.controller.MainActivity
import fr.iutvannes.dual.controller.viewmodel.SessionViewModel
import fr.iutvannes.dual.infrastructure.server.KtorServer
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.utils.EmailService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private var classeActuelle: String = ""

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
            //Si la séance est déjà en cours, on arrête la séance
            if (sessionViewModel.running.value) {
                //Demander confirmation
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Terminer la séance ?")
                    .setMessage("Voulez-vous vraiment arrêter la séance actuelle ?")
                    .setPositiveButton("Oui, arrêter") { _, _ ->

                        sessionViewModel.stopSession()
                        KtorServer.idSeanceActuelle = 0
                        Toast.makeText(requireContext(), "Séance terminée", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Annuler", null) // Ne fait rien si on clique sur annuler
                    .show()


            //Si la séance n'est pas en cours, on la lance
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    //Récupération des classes depuis la DB
                    val classes = withContext(Dispatchers.IO) { DatabaseProvider.db.classeDao().getAllNames() }

                    //Boîte de dialogue pour la CLASSE
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Sélectionner la classe")
                        .setItems(classes.toTypedArray()) { _, index ->
                            val classeChoisie = classes[index]

                            //Boîte de dialogue pour le TYPE
                            val types = arrayOf("Test VMA", "Entraînement", "Épreuve Finale")
                            android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Type de séance ($classeChoisie)")
                                .setItems(types) { _, typeIndex ->
                                    val typeChoisi = types[typeIndex]

                                    //Lancement final
                                    lancerLaSeance(classeChoisie, typeChoisi)
                                }
                                .show()
                        }
                        .show()
                }
            }
        }

        val layoutUrl = view.findViewById<View>(R.id.layoutUrl)
        val cardResultats = view.findViewById<View>(R.id.cardResultats)
        val nbResultat = view.findViewById<TextView>(R.id.text_resultats_count)
        val btnExport = view.findViewById<Button>(R.id.btn_download_excel)

        // Opening a coroutine in the I/O thread to count the results
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (sessionViewModel.running.value) {
                    val totalEnBase = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.resultatDao().getCount()
                    }

                    val depart = (activity as? MainActivity)?.countInitialSession ?: 0
                    val resultatsSeance = totalEnBase - depart

                    //On met à jour le nb de perfs enregistrés
                    nbResultat.text = "$resultatsSeance"
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
                if (running) {
                    sessionBtn.text = "Arrêter la séance"
                    val couleurBleu = ContextCompat.getColor(requireContext(), R.color.bleu)
                    sessionBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(couleurBleu)
                    qrCode.visibility = View.VISIBLE
                    layoutUrl.visibility = View.VISIBLE
                    cardResultats.visibility = View.VISIBLE
                    btnExport.visibility = View.VISIBLE

                    //Mise à jour du texte du bouton export
                    val dateAujourdhui = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(java.util.Date())
                    btnExport.text = "Télécharger les résultats (séance $dateAujourdhui - $classeActuelle)"

                } else {
                    sessionBtn.text = "Lancer une séance"
                    val couleurBleu = ContextCompat.getColor(requireContext(), R.color.bleu)
                    sessionBtn.backgroundTintList = android.content.res.ColorStateList.valueOf(couleurBleu)
                    qrCode.visibility = View.GONE
                    layoutUrl.visibility = View.GONE
                    cardResultats.visibility = View.GONE
                }
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

    /**
     * Méthode pour lancer une séance.
     * @param type : type de séance (Entraînement ou Évaluation)
     */
    private fun lancerLaSeance(classe: String, type: String) {
        classeActuelle = classe
        viewLifecycleOwner.lifecycleScope.launch {
            val idGenere = withContext(Dispatchers.IO) {
                val nouvelleSeance = fr.iutvannes.dual.model.persistence.Seance(
                    date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date()),
                    id_prof = DatabaseProvider.db.profDAO().getProfId(),
                    type = type,    //"Entraînement" ou "Épreuve Finale"
                    classe = classe
                )
                DatabaseProvider.db.seanceDao().insert(nouvelleSeance)
            }

            KtorServer.idSeanceActuelle = idGenere.toInt()

            val count = withContext(Dispatchers.IO) {
                DatabaseProvider.db.resultatDao().getCount()
            }
            (activity as MainActivity).countInitialSession = count

            sessionViewModel.startSession(requireContext())

            Toast.makeText(requireContext(), "Séance $type ($classe) lancée", Toast.LENGTH_SHORT).show()
        }
    }
}
