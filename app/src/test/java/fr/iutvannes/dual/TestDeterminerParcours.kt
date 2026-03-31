package fr.iutvannes.dual

import org.junit.Test
import fr.iutvannes.dual.model.Algo.Parcours
import fr.iutvannes.dual.model.Algo.VMA
import org.junit.Before

class TestDeterminerParcours {


    /**
     * L’objet Parcours à tester
     */
    private lateinit var parcours: Parcours

    /**
     * Instancie l’objet avant chaque test
     */
    @Before
    fun setUp() {
        parcours = Parcours()
    }

    /**
     * Test de la méthode determinerParcours()
     */
    @Test
    fun testDeterminerParcours() {
        println()
        println("Test determinerParcours(vma)")

        println("--- Cas normaux (Milieu d'intervalle) ---")
        testCasDeterminerParcours(10.0f, "Coupelles jaunes", false)
        testCasDeterminerParcours(11.0f, "Plots verts", false)
        testCasDeterminerParcours(12.0f, "Coupelles bleues", false)
        testCasDeterminerParcours(13.0f, "Plots bleus", false)
        testCasDeterminerParcours(14.0f, "Coupelles rouges", false)
        testCasDeterminerParcours(14.8f, "Plots rouges", false)
        testCasDeterminerParcours(16.0f, "Grand tour", false)

        println("--- Cas limites (Pivots exacts) ---")
        // à 10.5 pile, vma < 10.5 est faux, donc on passe à < 11.5 (Plots verts)
        testCasDeterminerParcours(10.5f, "Plots verts", false)
        testCasDeterminerParcours(11.5f, "Coupelles bleues", false)
        testCasDeterminerParcours(12.5f, "Plots bleus", false)
        testCasDeterminerParcours(13.5f, "Coupelles rouges", false)
        testCasDeterminerParcours(14.5f, "Plots rouges", false)
        // ici c'est <= 15.0f, donc 15.0 donne "Plots rouges", mais 15.1 donnera "Grand tour"
        testCasDeterminerParcours(15.0f, "Plots rouges", false)
        testCasDeterminerParcours(15.1f, "Grand tour", false)

        println("--- Cas d'erreur ---")
        testCasDeterminerParcours(-2.0f, "Coupelles jaunes", false)
        // la fonction ne crash pas et renvoie silencieusement le parcours coupelle jaune soit le parcours le plus court
        // cela évite de faire crash l'application même en cas de valeur érronée ce qui laisse la possibilité de modifier
        // la valeur de la vma plus tard
    }

    /**
     * Test d'un cas particulier de determinerParcours()
     * @param vma la VMA de l'élève
     * @param attendu le parcours attendu
     * @param casErr vrai si une erreur (exception) est attendue
     */
    private fun testCasDeterminerParcours(vma: Float, attendu: String, casErr: Boolean) {
        try {
            val res = parcours.determinerParcours(vma)
            if (casErr) {
                println("Échec du test pour VMA $vma (aucune exception alors qu’attendue)")
            } else {
                if (res == attendu) {
                    println("Test réussi pour VMA $vma : $res")
                } else {
                    println("Échec du test pour VMA $vma : résultat obtenu = $res, attendu = $attendu")
                }
            }
        } catch (e: Exception) {
            if (casErr) {
                println("Test réussi pour VMA $vma (exception capturée : ${e::class.simpleName})")
            } else {
                println("Échec du test pour VMA $vma (exception inattendue : ${e::class.simpleName})")
            }
        }
    }
}