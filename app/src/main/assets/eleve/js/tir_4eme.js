// Force le rechargement si la page vient du cache (navigation arrière/avant)
window.addEventListener("pageshow", (event) => {
    if (event.persisted) {
        window.location.reload();
    }
});

document.addEventListener("DOMContentLoaded", () => {

    // 1. Déclaration de toutes les variables UI
    const chronoDisplay = document.getElementById("chrono");
    const startBtn      = document.getElementById("start");
    const stopBtn       = document.getElementById("stop");
    const input         = document.getElementById("reussites");
    const sectionSaisie = document.getElementById("section-saisie");
    const validerBtn    = document.getElementById("valider-tir");
    const resultatBox   = document.getElementById("resultat-4eme");
    const btnSession    = document.getElementById("btn-envoyer");

    // Reset du bouton Envoyer au cas où la page viendrait du cache
    if (btnSession) {
        btnSession.disabled = false;
        btnSession.textContent = "Enregistrer la session";
        btnSession.style.backgroundColor = "#27ae60";
    }

    // 2. Variables du chronomètre (basées sur l'horloge absolue pour 0 décalage)
    let timeout;
    let estArrete = true;
    let dateDepart = null;
    let tempsAccumuleMs = 0;
    let tempsTotalEnSecondes = 0;

    let series = []; // Va stocker l'historique des tirs [{reussites, temps, note}]

    // --- LOGIQUE DE NOTATION ---
    function calculerNoteEfficience(temps, reussites) {
        if (reussites === 0) return 0;
        if (temps <= 80)  return 6;
        if (temps <= 85)  return 5.5;
        if (temps <= 90)  return 5;
        if (temps <= 95)  return 4.5;
        if (temps <= 100) return 4;
        if (temps <= 105) return 3.5;
        if (temps <= 110) return 3;
        if (temps <= 115) return 2.5;
        if (temps <= 120) return 2;
        if (temps <= 125) return 1.5;
        if (temps <= 130) return 1;
        return 0.5;
    }

    // --- GESTION DU CHRONOMÈTRE ---
    const defilerTemps = () => {
        if (estArrete) return;

        // Calcul du temps écoulé basé sur l'horloge de la tablette
        const totalMs = tempsAccumuleMs + (Date.now() - dateDepart);

        let m = Math.floor(totalMs / 60000);
        let s = Math.floor((totalMs % 60000) / 1000);
        let ms = Math.floor((totalMs % 1000) / 10);

        if (chronoDisplay) {
            chronoDisplay.textContent =
                String(m).padStart(2, '0') + ':' +
                String(s).padStart(2, '0') + ':' +
                String(ms).padStart(2, '0');
        }

        timeout = setTimeout(defilerTemps, 10);
    };

    const demarrerChrono = () => {
        if (estArrete) {
            estArrete = false;
            dateDepart = Date.now();
            if (sectionSaisie) sectionSaisie.style.display = "none";
            defilerTemps();
        }
    };

    const arreterChrono = () => {
        if (!estArrete) {
            estArrete = true;
            clearTimeout(timeout);

            // Sauvegarde de l'instant exact de l'arrêt
            tempsAccumuleMs += (Date.now() - dateDepart);
            tempsTotalEnSecondes = Math.floor(tempsAccumuleMs / 1000);

            // Affiche la zone de saisie du score
            if (sectionSaisie) sectionSaisie.style.display = "block";
            setTimeout(() => { if (input) input.focus(); }, 100);
        }
    };

    // --- VALIDATION D'UN TIR ---
    function calculerEtAfficher() {
        if (!input) return;

        let reussites = parseInt(input.value);
        if (isNaN(reussites)) {
            input.classList.add("input-error");
            alert("Veuillez entrer un nombre valide de réussites (0 à 5).");
            return;
        }

        reussites = Math.min(Math.max(reussites, 0), 5); // Force entre 0 et 5
        const note = calculerNoteEfficience(tempsTotalEnSecondes, reussites);
        series.push({ reussites, temps: tempsTotalEnSecondes, note });

        afficherSeries();

        // Réinitialise l'interface pour le prochain tir
        input.value = "";
        input.classList.remove("input-error");
        if (sectionSaisie) sectionSaisie.style.display = "none";
        tempsAccumuleMs = 0;
        dateDepart = null;
        if (chronoDisplay) chronoDisplay.textContent = "00:00:00";
        tempsTotalEnSecondes = 0;
    }

    // --- AFFICHAGE DE L'HISTORIQUE ---
    function afficherSeries() {
        if (resultatBox) resultatBox.style.display = "block";

        const penaliteMsg = document.getElementById("penalite-msg");
        const detailsTir  = document.getElementById("details-tir");

        if (penaliteMsg) penaliteMsg.textContent = `${series.length} tir(s) enregistré(s)`;

        let html = "";
        series.forEach((s, i) => {
            html += `<strong>Tir ${i + 1}</strong> — ${s.reussites}/5 en ${s.temps}s — note : ${s.note}<br>`;
        });

        const total = series.reduce((acc, s) => acc + s.reussites, 0);
        const max   = series.length * 5;
        html += `<br><strong>Total : ${total} / ${max}</strong>`;

        if (detailsTir) detailsTir.innerHTML = html;
    }

    // --- ENVOI AU SERVEUR ---
    async function envoyerSession() {
        if (series.length === 0) { alert("Aucun tir enregistré."); return; }

        const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));
        if (!coureur?.nomComplet) { alert("Identité de l'élève introuvable."); return; }

        const parts  = coureur.nomComplet.trim().split(" ");
        const prenom = parts[0] || "";
        const nom    = parts.slice(1).join(" ") || "";

        const request = {
            prenom,
            nom,
            distance:        coureur.vma_distance ? parseFloat(coureur.vma_distance) : 0,
            nbTours:         0,
            nbTirsReussi:    series.map(s => s.reussites),
            tempsAuPasDeTir: series.map(s => s.temps),
            tempsAuTour:     []
        };

        if (btnSession) {
            btnSession.disabled = true;
            btnSession.textContent = "Envoi en cours...";
        }

        try {
            const response = await fetch("/api/biathlon", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(request)
            });
            if (response.ok) {
                if (btnSession) {
                    btnSession.textContent = "Session envoyée !";
                    btnSession.style.backgroundColor = "#7f8c8d";
                }
                // Retour au Hub
                setTimeout(() => { window.location.href = "seance.html"; }, 1500);
            } else {
                const err = await response.text();
                alert("Erreur serveur : " + err);
                if (btnSession) {
                    btnSession.disabled = false;
                    btnSession.textContent = "Enregistrer la session";
                }
            }
        } catch (e) {
            console.error("Erreur réseau :", e);
            alert("Erreur réseau.");
            if (btnSession) {
                btnSession.disabled = false;
                btnSession.textContent = "Enregistrer la session";
            }
        }
    }

    // Attachement des écouteurs d'événements0
    if (startBtn) startBtn.addEventListener("click", demarrerChrono);
    if (stopBtn) stopBtn.addEventListener("click", arreterChrono);
    if (validerBtn) validerBtn.addEventListener("click", calculerEtAfficher);
    if (input) input.addEventListener("keypress", (e) => { if (e.key === "Enter") calculerEtAfficher(); });
    if (btnSession) btnSession.addEventListener("click", envoyerSession);
});