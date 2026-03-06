document.addEventListener("DOMContentLoaded", () => {

    let chronoDisplay = document.getElementById("chrono");
    let startBtn = document.getElementById("start");
    let stopBtn = document.getElementById("stop");
    const input = document.getElementById("reussites");

    // Nouveaux éléments pour la gestion 4ème
    let sectionSaisie = document.getElementById("section-saisie");
    let validerBtn = document.getElementById("valider-tir");
    let resultatBox = document.getElementById("resultat-4eme");

    let minutes = 0, secondes = 0, millisecondes = 0;
    let timeout;
    let estArrete = true;
    let tempsTotalEnSecondes = 0;

    function calculerNoteEfficience(temps, reussites) {
        // La note est de 0 si la réussite est de 0, peu importe le temps
        if (reussites === 0) return 0;

        // Barème du temps (en secondes)
        if (temps <= 80) return 6;      // 1'20 et -
        if (temps <= 85) return 5.5;    // 1'25
        if (temps <= 90) return 5;      // 1'30
        if (temps <= 95) return 4.5;    // 1'35
        if (temps <= 100) return 4;     // 1'40
        if (temps <= 105) return 3.5;    // 1'45
        if (temps <= 110) return 3;      // 1'50
        if (temps <= 115) return 2.5;    // 1'55
        if (temps <= 120) return 2;      // 2'00
        if (temps <= 125) return 1.5;    // 2'05
        if (temps <= 130) return 1;      // 2'10
        return 0.5;                      // 2'15 et +
    }

    const defilerTemps = () => {
        if (estArrete) return;
        millisecondes += 10;
        if (millisecondes === 1000) { millisecondes = 0; secondes++; }
        if (secondes === 60) { secondes = 0; minutes++; }

        let m = minutes < 10 ? "0" + minutes : minutes;
        let s = secondes < 10 ? "0" + secondes : secondes;
        let ms = Math.floor(millisecondes / 10);
        ms = ms < 10 ? "0" + ms : ms;

        chronoDisplay.textContent = `${m}:${s}:${ms}`;
        timeout = setTimeout(defilerTemps, 10);
    };

    const demarrerChrono = () => {
        if (estArrete) {
            estArrete = false;
            defilerTemps();

            // On cache les anciens résultats si on recommence une série
            sectionSaisie.style.display = "none";
            resultatBox.style.display = "none";
        }
    };
    const arreterChrono = () => {
        estArrete = true;
        clearTimeout(timeout);

        // On calcule le temps total en secondes pour l'efficience
        tempsTotalEnSecondes = (minutes * 60) + secondes;

        // Une fois arrêté, on affiche la zone pour saisir le score de la série
        sectionSaisie.style.display = "block";

        // Focus automatique sur l'input
        setTimeout(() => input.focus(), 100); // Un léger délai assure que l'élément est visible
    };


    //Fonction d'envoi JSON au serveur (tablette prof)
    async function envoyerResultat4eme(totalReussites, totalFautes, temps, noteEfficience) {
        const identite = localStorage.getItem("eleve_identite");
        const classe = localStorage.getItem("eleve_classe");

        const event = {
            type: "TIR_RESULTAT_4EME",
            studentId: identite,
            payload: {
                total: totalReussites,
                fautes: totalFautes, // Nombre de fautes
                tempsTirSec: temps, // Ajout du temps total en secondes pour l'efficience
                noteEfficience: noteEfficience, // Note d'efficience calculée
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

    // Ecouteur pour la touche Entrée sur l'input (ergonomie)
    input.addEventListener("keypress", (e) => {
        if (e.key === "Enter") {
            calculerEtAfficher();
        }
    });

    function calculerEtAfficher() {
        let reussites = parseInt(input.value);

        if (!isNaN(reussites)) {
            // Borner entre 0 et 5
            reussites = Math.min(Math.max(reussites, 0), 5);
            input.value = reussites;

            // Cacher la saisie après validation pour éviter les doubles clics ---
            sectionSaisie.style.display = "none";
            let fautes = 5 - reussites;
            // Calcul de la note d'efficience exacte
            let noteExacte = calculerNoteEfficience(tempsTotalEnSecondes, reussites);

            // Affichage du résultat final et des pénalités
            resultatBox.style.display = "block";
            const msgPenalite = document.getElementById("penalite-msg");
            const detailsTir = document.getElementById("details-tir");

            // On remet le style par défaut avant d'appliquer le nouveau
            msgPenalite.classList.remove("success", "danger");

            // Règle 4ème : 1 faute = 1 tour de pénalité
            if (fautes > 0) {
                msgPenalite.textContent = `RÉALISE ${fautes} TOUR(S) DE PÉNALITÉ`;
                msgPenalite.classList.add("danger");
            } else {
                msgPenalite.textContent = "AUCUNE PÉNALITÉ !";
                msgPenalite.classList.add("success");
            }

            detailsTir.innerHTML = `Réussites : ${reussites}/5 <br>
                                    Temps : ${tempsTotalEnSecondes}s<br>
                                    Note d'efficience : ${noteExacte}`;

            // Envoi des données
            envoyerResultat4eme(reussites, fautes, tempsTotalEnSecondes, noteExacte);

            // On vide l'input pour la prochaine série
            input.value = "";
            input.classList.remove("input-error");
        } else {
            input.classList.add("input-error"); // Ajoute une bordure rouge
            alert("Veuillez entrer un nombre valide de réussites (0 à 5).");
        }
    }

    // Les écouteurs d'événements
    startBtn.addEventListener("click", demarrerChrono);
    stopBtn.addEventListener("click", arreterChrono);

    // Bouton final pour enregistrer la série
    if (validerBtn) {
        validerBtn.addEventListener("click", calculerEtAfficher);
    }

});