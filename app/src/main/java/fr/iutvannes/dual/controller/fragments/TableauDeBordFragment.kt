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
 * Cette classe représente le fragment du tableau de bord.
 */
class TableauDeBordFragment : Fragment(R.layout.fragment_tableau_de_bord) {


    private var countInitial = 0
    private val sessionViewModel: SessionViewModel by activityViewModels()

    /**
     * Cette fonction est appelée lorsque la vue du fragment est créée.
     * Elle initialise les interactions avec les vues.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionBtn = view.findViewById<Button>(R.id.launchASession)
        sessionBtn.setOnClickListener {
            //Si la séance est déjà en cours, on arrête la séance
            if (sessionViewModel.running.value) {
                //Demander confirmation
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Terminer la séance ?")
                    .setMessage("Voulez-vous arrêter et recevoir le bilan par email ?")
                    .setPositiveButton("Oui, terminer") { _, _ ->

                        viewLifecycleOwner.lifecycleScope.launch {
                            val idSeance = KtorServer.idSeanceActuelle
                            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())

                            //Récupération des données (Résultats + Elèves)
                            val (resultats, eleves) = withContext(Dispatchers.IO) {
                                val res = DatabaseProvider.db.resultatDao().getBySeance(idSeance)
                                val ele = DatabaseProvider.db.EleveDao().getAll() // Liste de tous les élèves
                                Pair(res, ele)
                            }

                            //Faire le lien (Map) pour avoir les noms
                            val eleveMap = eleves.associateBy { it.id_eleve }

                            //Construction du CSV
                            val csvHeader = "Nom;Prenom;Temps;Cibles;Note\n"
                            val csvRows = resultats.joinToString("\n") { res ->
                                val e = eleveMap[res.id_eleve]
                                "${e?.nom ?: "Inconnu"};${e?.prenom ?: "Inconnu"};${res.temp_course};${res.cibles_touchees};${res.note_finale}"
                            }
                            val fullCsv = csvHeader + csvRows

                            //Récupération de l'email du prof et arrêt de la séance
                            val emailProf = withContext(Dispatchers.IO) { DatabaseProvider.db.profDAO().getProfEmail() }
                            sessionViewModel.stopSession()
                            KtorServer.idSeanceActuelle = 0

                            //Envoie de l'email
                            val success = EmailService.sendExcelExportEmail(emailProf, dateStr, fullCsv)

                            if (success) {
                                Toast.makeText(requireContext(), "Bilan envoyé à $emailProf", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(requireContext(), "Erreur d'envoi de l'email", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Annuler", null)
                    .show()


            //Si la séance n'est pas en cours, on la lance
            } else {
                //Lancement de la séance
                viewLifecycleOwner.lifecycleScope.launch {
                    val idGenere = withContext(Dispatchers.IO) {
                        val nouvelleSeance = fr.iutvannes.dual.model.persistence.Seance(
                            date = SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.FRANCE).format(java.util.Date()),
                            id_prof = DatabaseProvider.db.profDAO().getProfId()
                        )
                        DatabaseProvider.db.seanceDao().insert(nouvelleSeance)
                    }

                    KtorServer.idSeanceActuelle = idGenere.toInt()

                    val count = withContext(Dispatchers.IO) {
                        DatabaseProvider.db.resultatDao().getCount()
                    }
                    (activity as MainActivity).countInitialSession = count

                    sessionViewModel.startSession(requireContext())
                }
            }
        }

        val layoutUrl = view.findViewById<View>(R.id.layoutUrl)
        val cardResultats = view.findViewById<View>(R.id.cardResultats)
        val nbResultat = view.findViewById<TextView>(R.id.text_resultats_count)
        val btnExport = view.findViewById<Button>(R.id.btn_download_excel)

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
                    btnExport.text = "Télécharger les résultats (séance $dateAujourdhui)"

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
