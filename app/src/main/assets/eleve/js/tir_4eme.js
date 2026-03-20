document.addEventListener("DOMContentLoaded", () => {

    let chronoDisplay = document.getElementById("chrono");
    let startBtn      = document.getElementById("start");
    let stopBtn       = document.getElementById("stop");
    const input       = document.getElementById("reussites");
    let sectionSaisie = document.getElementById("section-saisie");
    let validerBtn    = document.getElementById("valider-tir");
    let resultatBox   = document.getElementById("resultat-4eme");
    let btnSession    = document.getElementById("enregistrer-session");

    let timeout;
    let estArrete = true;
    let dateDepart = null;
    let tempsEcoule = 0; // ms accumulés avant la dernière pause
    let tempsTotalEnSecondes = 0;

    let series = []; // [{reussites, temps, note}]

    // ─── NOTE EFFICIENCE ─────────────────────────────────────────────────────

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

    // ─── CHRONO ───────────────────────────────────────────────────────────────

    const defilerTemps = () => {
        if (estArrete) return;
        const totalMs = tempsEcoule + (Date.now() - dateDepart);
        const totalSec = Math.floor(totalMs / 1000);
        const m  = Math.floor(totalSec / 60);
        const s  = totalSec % 60;
        const ms = Math.floor((totalMs % 1000) / 10);
        chronoDisplay.textContent =
            String(m).padStart(2,'0') + ':' +
            String(s).padStart(2,'0') + ':' +
            String(ms).padStart(2,'0');
        timeout = setTimeout(defilerTemps, 10);
    };

    const demarrerChrono = () => {
        if (estArrete) {
            estArrete = false;
            dateDepart = Date.now();
            defilerTemps();
            sectionSaisie.style.display = "none";
        }
    };

    const arreterChrono = () => {
        estArrete = true;
        clearTimeout(timeout);
        tempsEcoule += Date.now() - dateDepart;
        tempsTotalEnSecondes = Math.floor(tempsEcoule / 1000);
        sectionSaisie.style.display = "block";
        setTimeout(() => input.focus(), 100);
    };

    // ─── VALIDATION D'UN TIR ─────────────────────────────────────────────────

    function calculerEtAfficher() {
        let reussites = parseInt(input.value);
        if (isNaN(reussites)) {
            input.classList.add("input-error");
            alert("Veuillez entrer un nombre valide de réussites (0 à 5).");
            return;
        }

        reussites = Math.min(Math.max(reussites, 0), 5);
        const note = calculerNoteEfficience(tempsTotalEnSecondes, reussites);
        series.push({ reussites, temps: tempsTotalEnSecondes, note });

        afficherSeries();

        // Reset chrono pour le prochain tir
        input.value = "";
        input.classList.remove("input-error");
        sectionSaisie.style.display = "none";
        tempsEcoule = 0;
        dateDepart = null;
        chronoDisplay.textContent = "00:00:00";
        tempsTotalEnSecondes = 0;
    }

    // ─── AFFICHAGE DE LA LISTE ────────────────────────────────────────────────

    function afficherSeries() {
        resultatBox.style.display = "block";

        const penaliteMsg = document.getElementById("penalite-msg");
        const detailsTir  = document.getElementById("details-tir");

        penaliteMsg.textContent = `${series.length} tir(s) enregistré(s)`;

        let html = "";
        series.forEach((s, i) => {
            html += `<strong>Tir ${i + 1}</strong> — ${s.reussites}/5 en ${s.temps}s — note : ${s.note}<br>`;
        });

        const total = series.reduce((acc, s) => acc + s.reussites, 0);
        const max   = series.length * 5;
        html += `<br><strong>Total : ${total} / ${max}</strong>`;

        detailsTir.innerHTML = html;
    }

    // ─── ENVOI FINAL ─────────────────────────────────────────────────────────

    async function envoyerSession() {
        if (series.length === 0) { alert("Aucun tir enregistré."); return; }

        const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));
        if (!coureur?.nomComplet) { alert("Identité élève introuvable."); return; }

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

        btnSession.disabled = true;
        btnSession.textContent = "Envoi en cours...";

        try {
            const response = await fetch("/api/biathlon", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(request)
            });
            if (response.ok) {
                btnSession.textContent = "Session envoyée !";
                btnSession.style.backgroundColor = "#7f8c8d";
                setTimeout(() => { window.location.href = "seance.html"; }, 1500);
            } else {
                const err = await response.text();
                alert("Erreur serveur : " + err);
                btnSession.disabled = false;
                btnSession.textContent = "Enregistrer la session";
            }
        } catch (e) {
            console.error("Erreur réseau :", e);
            alert("Erreur réseau.");
            btnSession.disabled = false;
            btnSession.textContent = "Enregistrer la session";
        }
    }

    // ─── ÉCOUTEURS ───────────────────────────────────────────────────────────

    startBtn.addEventListener("click", demarrerChrono);
    stopBtn.addEventListener("click", arreterChrono);
    if (validerBtn) validerBtn.addEventListener("click", calculerEtAfficher);
    input.addEventListener("keypress", (e) => { if (e.key === "Enter") calculerEtAfficher(); });
    btnSession.addEventListener("click", envoyerSession);
});