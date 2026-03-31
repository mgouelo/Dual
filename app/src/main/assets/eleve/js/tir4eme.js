// Force le rechargement si la page vient du cache (navigation arrière/avant)
window.addEventListener("pageshow", (event) => {
    if (event.persisted) {
        window.location.reload();
    }
});

// ─── SÉANCE DE TIR 4ÈME ──────────────────────────────────────────────────
let chronoDisplay = document.getElementById("chrono");
let startBtn      = document.getElementById("start");
let stopBtn       = document.getElementById("stop");
const input       = document.getElementById("reussites");
let sectionSaisie = document.getElementById("section-saisie");
let validerBtn    = document.getElementById("valider-tir");
let resultatBox   = document.getElementById("resultat-4eme");
let btnSession    = document.getElementById("btn-envoyer");
const modalTir      = document.getElementById("modal-tir");
const titreTir      = document.getElementById("titre-tir");
const scoreTemp     = document.getElementById("score-temporaire");
const modal = document.getElementById("custom-confirm");
const confirmOk = document.getElementById("confirm-ok");
const confirmCancel = document.getElementById("confirm-cancel");
const btnRetourSeance = document.getElementById("retourSeance");

// Reset du bouton Envoyer au cas où la page viendrait du cache
if (btnSession) {
    btnSession.disabled = false;
    btnSession.textContent = "Enregistrer la session";
    btnSession.style.backgroundColor = "#27ae60";
}

// Variables pour le chronomètre et les séries de tir
let timeout;
let estArrete = true;
let dateDepart = null;
let tempsAccumuleMs = 0; // ms accumulés avant la dernière pause
let tempsTotalEnSecondes = 0;

let series = []; // [{reussites, temps, note}]

/**
 * Calcule la note d'efficience au tir par croisement (Moyenne Temps/Réussite)
 * @param {number} secondes - Temps total cumulé des tirs
 * @param {number} reussite - Nombre total de cibles touchées
 * @returns {number} Note sur 6
 */
function calculerNoteEfficience(secondes, reussite) {
    if (reussite === 0) return 0; // Sécurité si 0 pointé

    // --- Note selon le Temps de tir ---
    let noteTemps = 0;
    if (secondes <= 80) noteTemps = 6;
    else if (secondes <= 85) noteTemps = 5.5;
    else if (secondes <= 90) noteTemps = 5;
    else if (secondes <= 95) noteTemps = 4.5;
    else if (secondes <= 100) noteTemps = 4;
    else if (secondes <= 105) noteTemps = 3.5;
    else if (secondes <= 110) noteTemps = 3;
    else if (secondes <= 115) noteTemps = 2.5;
    else if (secondes <= 120) noteTemps = 2;
    else if (secondes <= 125) noteTemps = 1.5;
    else if (secondes <= 130) noteTemps = 1;
    else noteTemps = 0.5;

    // --- Note selon la Réussite au tir ---
    let noteReussite = 0;
    if (reussite >= 8) noteReussite = 6;
    else if (reussite === 7) noteReussite = 5.5;
    else if (reussite === 6) noteReussite = 5;
    else if (reussite === 5) noteReussite = 4.5;
    else if (reussite === 4) noteReussite = 3.5;
    else if (reussite === 3) noteReussite = 2.5;
    else if (reussite === 2) noteReussite = 1.5;
    else if (reussite === 1) noteReussite = 1;
    else noteReussite = 0.5;

    // --- LE CROISEMENT ---
    let moyenne = (noteTemps + noteReussite) / 2;
    return Math.round(moyenne * 2) / 2;
}

/**
 * Fonction pour faire défiler le temps du chronomètre
 * Calcule le temps total écoulé en ms, puis convertit en minutes, secondes et centièmes
 * Affiche le temps formaté dans l'élément chronoDisplay
 * Utilise setTimeout pour se rappeler toutes les 10ms tant que le chrono n'est pas arrêté
 */
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

/**
 * Démarre le chronomètre si il est arrêté
 * Enregistre la date de départ et lance le défilement du temps
 * Cache la section de saisie des réussites pendant le tir
 */
const demarrerChrono = () => {
    if (estArrete) {
        estArrete = false;
        dateDepart = Date.now();
        defilerTemps();
        stopBtn.style.display = "block";

        if (startBtn) startBtn.style.display = "none";
        if (stopBtn) stopBtn.style.display = "block";
    }
};

/**
 * Arrête le chronomètre, calcule le temps total écoulé en secondes, et affiche la section de saisie des réussites
 */
const arreterChrono = () => {
    if (!estArrete) {
        estArrete = true;
        clearTimeout(timeout);

        // Sauvegarde de l'instant exact de l'arrêt
        tempsAccumuleMs += (Date.now() - dateDepart);
        tempsTotalEnSecondes = Math.floor(tempsAccumuleMs / 1000);

        // Bascule des boutons
        if (startBtn) startBtn.style.display = "block";
        if (stopBtn) stopBtn.style.display = "none";

        // Configuration et Affichage de la modale
        if (titreTir) titreTir.textContent = `Tir Série n°${series.length + 1}`;
        if (modalTir) {
            modalTir.style.display = "flex";
            setTimeout(() => modalTir.classList.add("show"), 10);
        }
    }
};

/**
 * Colore le bouton de score sélectionné dans la modale
 */
function setScoreTir(valeur) {
    scoreTemp.value = valeur;
    const boutons = document.querySelectorAll('.btn-score');
    boutons.forEach(btn => {
        btn.classList.remove('selected');
        if(parseInt(btn.textContent) === valeur) {
            btn.classList.add('selected');
        }
    });
}

/**
 * Calcule la note d'efficience en fonction du temps total au pas de tir et du nombre de réussites
 * Enregistre la série dans le tableau des séries, puis affiche la liste des séries enregistrées
 */
function calculerEtAfficher() {
    const estSelectionne = document.querySelector('.btn-score.selected');
    if (!estSelectionne) {
        alert("Tu dois sélectionner ton nombre de cibles avant de valider !");
        return;
    }

    let reussites = parseInt(scoreTemp.value);

    // On n'enregistre plus la note ici, on garde juste les données brutes
    series.push({ reussites, temps: tempsTotalEnSecondes });
    afficherSeries();

    // Fermeture de la modale
    modalTir.style.display = "none";
    modalTir.classList.remove("show");

    // Reset pour le prochain tir
    document.querySelectorAll('.btn-score').forEach(b => b.classList.remove('selected'));
    scoreTemp.value = "0";
    tempsAccumuleMs = 0;
    dateDepart = null;
    if (chronoDisplay) chronoDisplay.textContent = "00:00:00";
    tempsTotalEnSecondes = 0;
}

/**
 * Affiche la liste des séries de tir enregistrées, avec le nombre de réussites, le temps total au pas de tir et la note d'efficience
 * Affiche également le total des réussites sur le nombre maximum possible
 */
function afficherSeries() {
    if (resultatBox) resultatBox.style.display = "block";

    // Affiche le nombre de séries enregistrées
    const penaliteMsg = document.getElementById("penalite-msg");
    const detailsTir  = document.getElementById("details-tir");

    if (penaliteMsg) penaliteMsg.textContent = `${series.length} tir(s) enregistré(s)`;

    let html = '<div class="tir-results-list">';

    let totalReussites = 0;
    let totalTemps = 0;

    // Affichage des lignes individuelles (sans note)
    series.forEach((s, i) => {
        totalReussites += s.reussites;
        totalTemps += s.temps;

        let couleurScore = s.reussites >= 4 ? "#27ae60" : (s.reussites >= 2 ? "#f39c12" : "#e74c3c");

        html += `
        <div class="tir-result-row">
            <div class="tir-result-title">Tir ${i + 1}</div>
            <div class="tir-result-stats">
                <span class="tir-badge" style="color: ${couleurScore};">${s.reussites} / 5</span>
                <span class="tir-detail">⏱️ ${s.temps}s</span>
            </div>
            <hr style="border:none; border-top: 1px solid #ccc; margin: 10px 0;">
        </div>`;
    });

    html += '</div>';

    // Calcul de la note globale sur les totaux
    const noteGlobale = calculerNoteEfficience(totalTemps, totalReussites);
    const maxCibles = series.length * 5;

    // Affichage du Bilan Global avec un joli cadre
    html += `
    <div style="margin-top: 25px; padding: 15px; border-radius: 8px; border: 2px solid #303586; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
        <h3 style="margin: 0 0 15px 0; color: #303586; font-size: 1.3rem;">BILAN GLOBAL</h3>
        <p style="font-size: 1.1rem; margin: 5px 0;"><strong>Précision :</strong> ${totalReussites} / ${maxCibles} cibles</p>
        <p style="font-size: 1.1rem; margin: 5px 0;"><strong>Temps Cumulé :</strong> ${totalTemps} secondes</p>
        <hr style="border:none; border-top: 1px solid #303586; margin: 15px 0;">
        <p style="font-size: 1.3rem; margin: 0; color: #333;">Note Efficience : <strong style="color:#303586;">${noteGlobale} / 6</strong></p>
    </div>`;

    if (detailsTir) detailsTir.innerHTML = html;
}

/**
 * Envoie les données de la session de tir au serveur via une requête POST
 * @returns {Promise<void>} Une promesse qui se résout lorsque la requête est terminée, avec gestion des erreurs et mise à jour de l'interface en conséquence
 */
async function envoyerSession() {
    const confirmationAction = await demanderConfirmation("Mettre fin à la session et envoyer les données ?");

    if (confirmationAction) {
        // Valide qu'il y a au moins une série enregistrée avant d'envoyer
        if (series.length === 0) { alert("Aucun tir enregistré."); return; }

        // Récupère les informations du coureur actif depuis le localStorage
        const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));
        if (!coureur?.nomComplet) { alert("Identité élève introuvable."); return; }

        // Extrait le prénom et le nom à partir du nom complet du coureur
        const parts  = coureur.nomComplet.trim().split(" ");
        const prenom = parts[0] || "";
        const nom    = parts.slice(1).join(" ") || "";

        // Construit l'objet de requête à envoyer au serveur, avec les données de la session de tir
        const request = {
            prenom,
            nom,
            distance:        coureur.vma_distance ? parseFloat(coureur.vma_distance) : 0,
            nbTours:         0,
            nbTirsReussi:    series.map(s => s.reussites),
            tempsAuPasDeTir: series.map(s => s.temps * 1000),
            tempsAuTour:     []
        };

        // Désactive le bouton d'envoi et change son texte pour indiquer que l'envoi est en cours
        if (btnSession) {
            btnSession.disabled = true;
            btnSession.textContent = "Envoi en cours...";
        }

        // Envoie la requête POST au serveur et gère la réponse
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
                // Retour au Hub proprement après 1.5s
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
}

/**
 * Affiche une boîte de confirmation avant de retourner à la page de session, pour éviter les pertes de données accidentelles
 * @returns {Promise<void>} Une promesse qui se résout lorsque l'utilisateur a pris une décision, avec redirection vers la page de session si il confirme, ou maintien sur la page actuelle s'il annule
 */
const retourSeance = async() =>{
    if (await demanderConfirmation("Abandonner la session en cours et retourner sur Session Biathlon ?")) {
        window.location.href = "../pages/seance.html";
    }
}

/* === SYSTÈME DE MODALES === */
/**
 * Boîte de confirmation avec OK / Annuler.
 * @param {string} message - Le message à afficher dans la boîte de confirmation.
 * @return {Promise<boolean>} Une promesse qui se résout en true si l'utilisateur confirme, ou false s'il annule.
 * */
const demanderConfirmation = (message) => {
    document.getElementById("confirm-message").textContent = message;
    modal.style.display = "flex";
    setTimeout(() => { modal.classList.add("show"); }, 10);

    return new Promise((resolve) => {
        confirmOk.onclick = () => {
            modal.classList.remove("show");
            setTimeout(() => { modal.style.display = "none"; }, 300);
            resolve(true);
        };
        confirmCancel.onclick = () => {
            modal.classList.remove("show");
            setTimeout(() => { modal.style.display = "none"; }, 300);
            resolve(false);
        };
    });
};
// (Utilise globalThis.setScoreTir pour la rendre accessible par le onClick HTML)
globalThis.setScoreTir = setScoreTir;

// Attachement des écouteurs d'événements0
if (startBtn) startBtn.addEventListener("click", demarrerChrono);
if (stopBtn) stopBtn.addEventListener("click", arreterChrono);
if (validerBtn) validerBtn.addEventListener("click", calculerEtAfficher);
if (btnSession) btnSession.addEventListener("click", envoyerSession);
if(btnRetourSeance) btnRetourSeance.addEventListener("click", retourSeance);
