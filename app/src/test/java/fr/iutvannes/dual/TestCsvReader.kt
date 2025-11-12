package fr.iutvannes.dual

import fr.iutvannes.dual.model.importation.StudentDraft
import fr.iutvannes.dual.model.importation.readers.CsvStudentReader
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class TestCsvReader {
    // objet à tester
    private lateinit var reader: CsvStudentReader

    // Instancie le lecteur avant chaque test
    @Before
    fun setUp() {
        reader = CsvStudentReader()
    }

    // Test principal de lecture d’un fichier CSV
    @Test
    fun testReadCsv() {
        println()
        println("===== Test CsvStudentReader.read() =====")

        println("Cas normal : 2 élèves valides")
        val csvNormal = """
            Prénom;Nom;Classe
            Marin;Weis;4A
            Matthieu;Gouélo;3B
        """.trimIndent()
        testCasLecture(csvNormal, 2, false)

        println("Cas normal : 2 élèves valides + une colonne à sauter")
        val csv4Colonne = """
            Prénom;DateNaissance;Nom;Classe
            Marin;2006;Weis;4A
            Matthieu;2006;Gouélo;3B
        """.trimIndent()
        testCasLecture(csv4Colonne, 2, false)

        println("Cas avec ligne vide")
        val csvVide = """
            Prénom;Nom;Classe
            Léa;Dupont;4A
            
            Sam;Bernard;5C
        """.trimIndent()
        testCasLecture(csvVide, 2, false)

        println("Cas d’erreur : colonne 'Prénom' manquante")
        val csvErreur = """
            Nom;Classe
            Durand;4B
        """.trimIndent()
        testCasLecture(csvErreur, 0, true)
    }

    /**
     * Test d’un cas particulier de lecture CSV
     * @param contenu contenu CSV simulé
     * @param attendu nombre d’élèves attendus
     * @param casErr vrai si une exception est attendue
     */
    private fun testCasLecture(contenu: String, attendu: Int, casErr: Boolean) {
        try {
            val input = ByteArrayInputStream(contenu.toByteArray(StandardCharsets.UTF_8))
            val res: List<StudentDraft> = reader.read(input)

            if (casErr) {
                println("Echec : aucune exception alors qu’une était attendue")
            } else {
                if (res.size == attendu) {
                    println("Test réussi : ${res.size} élèves lus")
                    res.forEach { println("   -> ${it.firstName} ${it.lastName} (${it.classe ?: "aucune classe"})") }
                } else {
                    println("Echec : ${res.size} élèves lus, attendu = $attendu")
                }
            }
        } catch (e: Exception) {
            if (casErr) {
                println("Test réussi (exception capturée : ${e::class.simpleName})")
            } else {
                println("Echec : exception inattendue (${e::class.simpleName})")
            }
        }
    }
}