package fr.iutvannes.dual.controller.viewmodel

import androidx.lifecycle.AndroidViewModel
import android.app.Application
import androidx.room.Room
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.importation.ImportReport
import fr.iutvannes.dual.model.importation.ImportService
import fr.iutvannes.dual.model.importation.readers.CsvStudentReader
import fr.iutvannes.dual.model.importation.readers.OdfStudentReader
import fr.iutvannes.dual.model.importation.readers.XlsStudentReader
import java.io.InputStream

class ImportViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "dual.db"
    ).build()

    private val importService = ImportService( // on fournie à l'objet importService tous les readers et la DB pour que l'import puiise avoir lieu
        readers = listOf(
            CsvStudentReader(),
            XlsStudentReader(),
            OdfStudentReader()
        ),
        eleveDao = db.EleveDao()
    )

    suspend fun importer(input: InputStream, fileName: String, mime: String?): ImportReport {
        return importService.import(input, fileName, mime)
    }
}