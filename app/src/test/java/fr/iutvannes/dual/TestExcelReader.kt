package fr.iutvannes.dual

import fr.iutvannes.dual.model.importation.StudentDraft
import fr.iutvannes.dual.model.importation.readers.XlsStudentReader
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class TestExcelStudentReader {

    /** L’objet à tester */
    private lateinit var reader: XlsStudentReader

    /** Instancie le lecteur avant chaque test */
    @Before
    fun setUp() {
        reader = XlsStudentReader()
    }

    /** Test principal de lecture d’un fichier Excel */
    @Test
    fun testReadExcel() {
        println()
        println("===== Test ExcelStudentReader.read() =====")

        println("Cas normal : 2 élèves valides")
        val normalWorkbook = createWorkbook(
            headers = listOf("Prénom", "Nom", "Classe"),
            rows = listOf(
                listOf("Glen", "Potay", "4A"),
                listOf("Nolann", "Les-Cops", "3B")
            )
        )
        testCasLecture(normalWorkbook, 2, false)

        println("Cas avec ligne vide")
        val workbookVide = createWorkbook(
            headers = listOf("Prénom", "Nom", "Classe"),
            rows = listOf(
                listOf("Glen", "Potay", "4A"),
                listOf("", "", ""), // ligne vide
                listOf("Nolann", "Les-Cops", "5C")
            )
        )
        testCasLecture(workbookVide, 2, false)

        println("Cas d’erreur : colonne 'Prénom' manquante")
        val workbookErreur = createWorkbook(
            headers = listOf("Nom", "Classe"),
            rows = listOf(listOf("Weis", "4B"))
        )
        testCasLecture(workbookErreur, 0, true)
    }

    /**
     * Test d’un cas particulier de lecture
     * @param wb le classeur Excel à tester
     * @param attendu nombre d’élèves attendus
     * @param casErr vrai si une exception est attendue
     */
    private fun testCasLecture(wb: XSSFWorkbook, attendu: Int, casErr: Boolean) {
        try {
            val input = workbookToInputStream(wb)
            val res: List<StudentDraft> = reader.read(input)

            if (casErr) {
                println("Échec du test : aucune exception alors que 1 attendue")
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

    /**
     * Crée un classeur Excel de test
     */
    private fun createWorkbook(headers: List<String>, rows: List<List<String>>): XSSFWorkbook {
        val wb = XSSFWorkbook()
        val sheet = wb.createSheet("Feuille1")

        // ligne d’en-tête
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }

        // lignes de données
        rows.forEachIndexed { index, values ->
            val row = sheet.createRow(index + 1)
            values.forEachIndexed { col, value ->
                row.createCell(col).setCellValue(value)
            }
        }
        return wb
    }

    /**
     * Convertit le classeur Excel en InputStream (pour simulation d’un vrai fichier)
     */
    private fun workbookToInputStream(wb: XSSFWorkbook): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        wb.use { it.write(out) }
        return ByteArrayInputStream(out.toByteArray())
    }
}
