package fr.iutvannes.dual

import androidx.room.Room
import fr.iutvannes.dual.model.database.AppDatabase
import fr.iutvannes.dual.model.persistence.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class TestDAO {

    private lateinit var db: AppDatabase

    @Before
    fun initDb() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // Test eleve
    @Test
    fun testInsertAndGetEleve() = runBlocking {
        val eleve = Eleve(nom = "Dupont", prenom = "Jean")
        val id = db.EleveDao().insert(eleve).toInt()

        val resultat = db.EleveDao().getEleveById(id)
        assertNotNull(resultat)
        assertEquals("Dupont", resultat?.nom)
    }

    @Test
    fun testUpdateEleve() = runBlocking {
        val eleve = Eleve(nom = "Durand", prenom = "Paul")
        val id = db.EleveDao().insert(eleve).toInt()

        val updated = eleve.copy(id_eleve = id, nom = "Durand-Modifié")
        val rows = db.EleveDao().update(updated)

        val resultat = db.EleveDao().getEleveById(id)
        assertEquals(1, rows)
        assertEquals("Durand-Modifié", resultat?.nom)
    }

    @Test
    fun testDeleteEleve() = runBlocking {
        val eleve = Eleve(nom = "Martin", prenom = "Léa")
        val id = db.EleveDao().insert(eleve).toInt()

        val rowsDeleted = db.EleveDao().delete(id)
        assertEquals(1, rowsDeleted)

        val resultat = db.EleveDao().getEleveById(id)
        assertNull(resultat)
    }

    // Test prof
    @Test
    fun testInsertAndGetProfByEmail() = runBlocking {
        val prof = Prof(nom = "Tibermacine", prenom = "Chouki", email = "chouki.tibermacine@iutvannes.fr", password = "Password1.")
        val id = db.profDAO().insert(prof).toInt()

        val resultat = db.profDAO().getProfByEmail("chouki.tibermacine@iutvannes.fr")
        assertNotNull(resultat)
        assertEquals("Chouki", resultat?.prenom)
    }

    // Test seance
    @Test
    fun testInsertSeance() = runBlocking {
        val profId = db.profDAO().insert(
            Prof(nom = "Kamp", prenom = "Jean-François", email = "jf.kamp@iutvannes.fr", password = "Password1.")
        ).toInt()

        val seance = Seance(date = "2025-11-06", nb_tours = 3, nb_cibles = 5, id_prof = profId)
        val id = db.seanceDao().insert(seance).toInt()

        val resultat = db.seanceDao().getSeanceById(id)
        assertNotNull(resultat)
        assertEquals(3, resultat?.nb_tours)
    }

    // Test resultat
    @Test
    fun testInsertResultat() = runBlocking {
        val eleveId = db.EleveDao().insert(Eleve(nom = "Bernard", prenom = "Luc")).toInt()
        val profId = db.profDAO().insert(Prof(nom = "Lemoine", prenom = "Anne", email = "anne@iutvannes.fr", password = "Password1.")).toInt()
        val seanceId = db.seanceDao().insert(Seance(date = "2025-05-10", nb_tours = 4, nb_cibles = 5, id_prof = profId)).toInt()

        val resultat = Resultat(id_eleve = eleveId, id_seance = seanceId, temp_course = 25.3F, cibles_touchees = 4, note_finale = 15.5F)
        val id = db.resultatDao().insert(resultat).toInt()

        val res = db.resultatDao().getResultatById(id)
        assertNotNull(resultat)
        assertEquals(4, res?.cibles_touchees)
    }
}
