/**
 * Synchronise la VMA du coureur actif avec les données du serveur.
 * @returns {Promise<void>} Une promesse qui se résout lorsque la synchronisation est terminée, avec gestion des erreurs et mise à jour de l'interface en conséquence
 */
async function synchroniserVMAServeur() {
    const coureurData = localStorage.getItem("coureur_actif_objet");
    if (!coureurData) return;

    let coureur = JSON.parse(coureurData);
    if (!coureur.id_eleve) return;

    try {
        const response = await fetch(`/api/eleves/${coureur.id_eleve}`);
        if (response.ok) {
            const data = await response.json();
            const vmaProf = parseFloat(data.vma) || 0;

            if (vmaProf > 0 && coureur.vma !== vmaProf) {
                console.log(`VMA actualisée depuis le serveur : ${vmaProf}`);
                coureur.vma = vmaProf;
                localStorage.setItem("coureur_actif_objet", JSON.stringify(coureur));

                const vmaAffichage = document.getElementById("vma-actuelle");
                if (vmaAffichage) vmaAffichage.textContent = vmaProf.toFixed(1);

                if (typeof afficherParcoursVMA === "function") {
                    afficherParcoursVMA();
                }
            }
        }
    } catch (error) {
        console.warn("Mode hors-ligne, VMA locale conservée.");
    }
}

// S'exécute automatiquement dès que le fichier est chargé par une page
window.addEventListener("pageshow", synchroniserVMAServeur);