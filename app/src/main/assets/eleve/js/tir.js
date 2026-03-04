document.addEventListener("DOMContentLoaded", () => {
    const nombreDeTirs = 6;
    const btnEnvoyer = document.getElementById("btn-envoyer");

    // Focus automatique sur le 1er input
    const inputTir1 = document.getElementById("tir1");
    setTimeout(() => inputTir1.focus(), 100); // Un léger délai assure que l'élément est visible


    //Fonction d'envoi JSON au serveur (tablette prof)
    async function envoyerResultatAuServeur(total, medaille) {
        const identite = localStorage.getItem("eleve_identite");
        const classe = localStorage.getItem("eleve_classe");

        const event = {
            type: "TIR_RESULTAT_6EME",
            studentId: identite,
            payload: {
                total: String(total),
                medaille: medaille,
                classe: classe,
                timestamp: String(Date.now())
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
                    //Feedback visuel et redirection
                    btnEnvoyer.textContent = "Résultats transmis !";
                    btnEnvoyer.style.backgroundColor = "#7f8c8d";
                    setTimeout(() => { window.location.href = "../index.html"; }, 1500);
                }
            } catch (error) {
                console.error("Erreur de connexion au serveur :", error);
                btnEnvoyer.textContent = "Erreur de connexion";
                btnEnvoyer.disabled = false;
            }
    }

    function calculerResultats() {
        let total = 0;
        let compteTirs = 0;

        for (let i = 1; i <= nombreDeTirs; i++) {
            const champ = document.getElementById(`tir${i}`);
            let valeur = parseInt(champ.value);

            if (!isNaN(valeur) && champ.value !== "") {
                //Strict nécessaire : borner entre 0 et 5 (5 balles max par tir)
                valeur = Math.min(Math.max(valeur, 0), 5);
                champ.value = valeur; // Correction visuelle pour l'élève

                total += valeur;
                compteTirs++;
            }
        }

        const resultatsDiv = document.getElementById("resultats");
        resultatsDiv.style.display = "block";
        document.getElementById("total").textContent = total;

        //Ajout de la médaille selon le barème officiel
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
            display.style.padding = "20px";
            display.style.borderRadius = "12px";
            display.style.fontSize = "1.6rem";

            // Sauvegarde pour le bilan final
            localStorage.setItem("tir_total", total);
            localStorage.setItem("tir_medaille", medaille);

            //Appel de l'envoie JSON
            if (btnEnvoyer) {
                btnEnvoyer.style.display = "block";
                btnEnvoyer.onclick = () => {
                    btnEnvoyer.disabled = true; //Empêche l'élève de cliquer plusieurs fois
                    btnEnvoyer.textContent = "Envoi en cours...";
                    envoyerResultatAuServeur(total, medaille);
                };
            }

        } else {
            display.textContent = `Saisie en cours (${compteTirs}/${nombreDeTirs})`;
            display.style.backgroundColor = "transparent";
        }
    }

    // Ecouteur pour la touche Entrée sur l'input (ergonomie)
    const inputTir6eme = document.querySelector(".tir6eme");
    inputTir6eme.addEventListener("keypress", (e) => {
        if (e.key === "Enter") {
            calculerResultats();
        }
    });

    for (let i = 1; i <= nombreDeTirs; i++) {
        const inputTir = document.getElementById(`tir${i}`);
        if (inputTir) {
            inputTir.addEventListener("input", calculerResultats);
        }
    }
});