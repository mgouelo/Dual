document.addEventListener("DOMContentLoaded", () => {
    const nombreDeTirs = 6;


    //Fonction d'envoi JSON au serveur (tablette prof)
    async function envoyerResultatAuServeur(total, medaille) {
        const identite = localStorage.getItem("eleve_identite");
        const classe = localStorage.getItem("eleve_classe");

        const event = {
            type: "TIR_RESULTAT_6EME",
            studentId: identite,
            payload: {
                total: total,
                medaille: medaille,
                classe: classe,
                timestamp: Date.now()
            }
        };

        try {
            const response = await fetch('/event', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(event)
            });
            if (response.ok) {
                console.log("Données envoyées avec succès au serveur Ktor.");
            }
        } catch (error) {
            console.error("Erreur de connexion au serveur :", error);
        }
    }

    function calculerResultats() {
        let total = 0;
        let compteTirs = 0;

        for (let i = 1; i <= nombreDeTirs; i++) {
            const champ = document.getElementById(`tir${i}`);
            let valeur = parseInt(champ.value);

            if (!isNaN(valeur) && champ.value !== "") {
                // Strict nécessaire : borner entre 0 et 5 (5 balles max par tir)
                valeur = Math.min(Math.max(valeur, 0), 5);
                champ.value = valeur; // Correction visuelle pour l'élève

                total += valeur;
                compteTirs++;
            }
        }

        const resultatsDiv = document.getElementById("resultats");
        resultatsDiv.style.display = "block";
        document.getElementById("total").textContent = total;

        // Ajout de la médaille selon le barème officiel
        const display = document.getElementById("medaille-display");
        if (compteTirs === nombreDeTirs) {
            let medaille = "BRONZE";
            let couleur = "#cd7f32";

            if (total >= 25) { medaille = "DIAMANT"; couleur = "#1456DB"; }
            else if (total >= 22) { medaille = "PLATINE"; couleur = "#b9f2ff"; }
            else if (total >= 19) { medaille = "OR"; couleur = "#ffd700"; }
            else if (total >= 16) { medaille = "ARGENT"; couleur = "#c0c0c0"; }

            display.textContent = "Médaille : " + medaille;
            display.style.backgroundColor = couleur;
            display.style.padding = "10px";
            display.style.borderRadius = "12px";

            // Sauvegarde pour le bilan final
            localStorage.setItem("tir_total", total);
            localStorage.setItem("tir_medaille", medaille);

            //Appel de l'envoie JSON
            envoyerResultatAuServeur(total, medaille);

        } else {
            display.textContent = `Saisie en cours (${compteTirs}/${nombreDeTirs})`;
            display.style.backgroundColor = "transparent";
        }
    }

    for (let i = 1; i <= nombreDeTirs; i++) {
        const inputTir = document.getElementById(`tir${i}`);
        if (inputTir) {
            inputTir.addEventListener("input", calculerResultats);
        }
    }
});