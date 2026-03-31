function testNormaliserTexte() {
    console.log("\n=== Test normaliserTexte(texte) ===");

    console.log("\n--- Cas normaux ---");
    testCasNormaliser("Éléonore", "eleonore", false);
    testCasNormaliser("AdÈlE", "adele", false);
    testCasNormaliser("François", "francois", false);

    console.log("\n--- Cas avec caractères spéciaux ---");
    testCasNormaliser("Jean-Baptiste", "jean baptiste", false);
    testCasNormaliser("D'Artagnan", "d artagnan", false);

    console.log("\n--- Cas des espaces ---");
    testCasNormaliser("  Espaces en trop  ", "espaces en trop", false);

    console.log("\n--- Cas combiné ---");
    testCasNormaliser("  L'Étoile-Filante  ", "l etoile filante", false);

    console.log("\n--- Cas d'erreur ---");
    testCasNormaliser(null, "", true);
    testCasNormaliser(undefined, "", true);
}

/**
 * Test d'un cas particulier de normaliserTexte()
 * @param {string} texte Le texte d'entrée
 * @param {string} attendu Le texte formaté attendu
 * @param {boolean} casErr vrai si une erreur est attendue
 */
function testCasNormaliser(texte, attendu, casErr) {
    try {
        const res = normaliserTexte(texte);
        if (casErr) {
            console.log(`Échec du test pour "${texte}" (aucune exception alors qu'attendue)`);
        } else {
            if (res === attendu) {
                console.log(`Test réussi pour "${texte}" : "${res}"`);
            } else {
                console.error(`Échec du test pour "${texte}" : résultat obtenu = "${res}", attendu = "${attendu}"`);
            }
        }
    } catch (e) {
        if (casErr) {
            console.log(`Test réussi pour "${texte}" (exception capturée : ${e.name})`);
        } else {
            console.error(`Échec du test pour "${texte}" (exception inattendue : ${e.name} - ${e.message})`);
        }
    }
}

/**
 * Fonction pour rendre la recherche + souple :
 * Supprime les accents, met en minuscules, et remplace les tirets/apostrophes par des espaces.
 */
function normaliserTexte(texte) {
    return texte.normalize("NFD")             // Sépare les lettres de leurs accents
                .replace(/[\u0300-\u036f]/g, "") // Supprime les accents
                .replace(/['\-]/g, " ")          // Remplace les apostrophes et tirets par des espaces
                .toLowerCase()                   // Tout en minuscules
                .trim();
}

testNormaliserTexte();