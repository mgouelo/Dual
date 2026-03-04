//package fr.iutvannes.dual
//
//import fr.iutvannes.dual.model.importation.StudentDraft
//import org.junit.Before
//import org.junit.Test
//import org.odftoolkit.simple.SpreadsheetDocument
//import java.io.ByteArrayInputStream
//import java.io.ByteArrayOutputStream
//
//class TestOdfStudentReader {
//
//    // objet à tester
//    private lateinit var reader: OdsStudentReader
//
//    @Before
//    fun setUp() {
//        reader = OdsStudentReader()
//    }
//
//    // test principal de lecture d’un fichier ODF
//    @Test
//    fun testReadOds() {
//        println()
//        println("===== Test OdfStudentReader.read() =====")
//
//        println("Cas normal : 2 élèves valides")
//        val odsNormal = createOdsWorkbook(
//            headers = listOf("Prénom", "Nom", "Classe"),
//            rows = listOf(
//                listOf("Nolann", "LesCops", "4A"),
//                listOf("Matth", "Gouélo", "3B")
//            )
//        )
//        testCasLecture(odsNormal, 2, false)
//
//        println("Cas avec ligne vide")
//        val odsVide = createOdsWorkbook(
//            headers = listOf("Prénom", "Nom", "Classe"),
//            rows = listOf(
//                listOf("Nolann", "LesCops", "4A"),
//                listOf("", "", ""),
//                listOf("Matthieu", "Gouélo", "5C")
//            )
//        )
//        testCasLecture(odsVide, 2, false)
//
//        println("Cas d’erreur : colonne 'Prénom' manquante")
//        val odsErreur = createOdsWorkbook(
//            headers = listOf("Nom", "Classe"),
//            rows = listOf(listOf("LesCops", "4B"))
//        )
//        testCasLecture(odsErreur, 0, true)
//    }
//
//    /**
//     * test d’un cas particulier de lecture ODS
//     */
//    private fun testCasLecture(input: ByteArrayInputStream, attendu: Int, casErr: Boolean) {
//        try {
//            val res: List<StudentDraft> = reader.read(input)
//            if (casErr) {
//                println("Echec : aucune exception alors qu’attendue")
//            } else {
//                if (res.size == attendu) {
//                    println("Test réussi : ${res.size} élèves lus")
//                    res.forEach { println("   -> ${it.firstName} ${it.lastName} (${it.classe ?: "aucune classe"})") }
//                } else {
//                    println("Echec : ${res.size} élèves lus, attendu = $attendu")
//                }
//            }
//        } catch (e: Exception) {
//            if (casErr) {
//                println("Test réussi (exception capturée : ${e::class.simpleName})")
//            } else {
//                println("Echec : exception inattendue (${e::class.simpleName})")
//            }
//        }
//    }
//
//    /**
//     * Crée un tableur ODS en mémoire puor le test
//     */
//    private fun createOdsWorkbook(headers: List<String>, rows: List<List<String>>): ByteArrayInputStream {
//        val doc = SpreadsheetDocument.newSpreadsheetDocument()
//        val sheet = doc.getSheetByIndex(0)
//
//        // ligne d’en-tête
//        headers.forEachIndexed { c, h ->
//            sheet.getCellByPosition(c, 0).setStringValue(h)
//        }
//
//        // lignes de données
//        rows.forEachIndexed { r, values ->
//            values.forEachIndexed { c, value ->
//                sheet.getCellByPosition(c, r + 1).setStringValue(value)
//            }
//        }
//
//        val out = ByteArrayOutputStream()
//        doc.save(out)
//        doc.close()
//        return ByteArrayInputStream(out.toByteArray())
//    }
//}
