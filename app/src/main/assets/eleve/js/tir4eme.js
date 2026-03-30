// ─── SÉANCE DE TIR 4ÈME ──────────────────────────────────────────────────
let chronoDisplay = document.getElementById("chrono");
let startBtn      = document.getElementById("start");
let stopBtn       = document.getElementById("stop");
const input       = document.getElementById("reussites");
let sectionSaisie = document.getElementById("section-saisie");
let validerBtn    = document.getElementById("valider-tir");
let resultatBox   = document.getElementById("resultat-4eme");
let btnSession    = document.getElementById("btn-envoyer");

// Variables pour le chronomètre et les séries de tir
let timeout;
let estArrete = true;
let dateDepart = null;
let tempsEcoule = 0; // ms accumulés avant la dernière pause
let tempsTotalEnSecondes = 0;

let series = []; // [{reussites, temps, note}]

/**
 * Calcule la note d'efficience en fonction du temps et du nombre de réussites
 * @param temps Temps en secondes pour réaliser le tir (temps total au pas de tir)
 * @param reussites Nombre de tirs réussis (0 à 5)
 * @returns {number} Note d'efficience calculée selon la grille définie, ou 0 si aucune réussite
 */
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

/**
 * Fonction pour faire défiler le temps du chronomètre
 * Calcule le temps total écoulé en ms, puis convertit en minutes, secondes et centièmes
 * Affiche le temps formaté dans l'élément chronoDisplay
 * Utilise setTimeout pour se rappeler toutes les 10ms tant que le chrono n'est pas arrêté
 */
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
        sectionSaisie.style.display = "none";
    }
};

/**
 * Arrête le chronomètre, calcule le temps total écoulé en secondes, et affiche la section de saisie des réussites
 */
const arreterChrono = () => {
    estArrete = true;
    clearTimeout(timeout);
    tempsEcoule += Date.now() - dateDepart;
    tempsTotalEnSecondes = Math.floor(tempsEcoule / 1000);
    sectionSaisie.style.display = "block";
    setTimeout(() => input.focus(), 100);
};

/**
 * Calcule la note d'efficience en fonction du temps total au pas de tir et du nombre de réussites
 * Enregistre la série dans le tableau des séries, puis affiche la liste des séries enregistrées
 */
function calculerEtAfficher() {
    // Valide le nombre de réussites entré par l'utilisateur
    let reussites = parseInt(input.value);
    if (isNaN(reussites)) {
        input.classList.add("input-error");
        alert("Veuillez entrer un nombre valide de réussites (0 à 5).");
        return;
    }

    // Limite le nombre de réussites entre 0 et 5
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

/**
 * Affiche la liste des séries de tir enregistrées, avec le nombre de réussites, le temps total au pas de tir et la note d'efficience
 * Affiche également le total des réussites sur le nombre maximum possible
 */
function afficherSeries() {
    resultatBox.style.display = "block";

    // Affiche le nombre de séries enregistrées
    const penaliteMsg = document.getElementById("penalite-msg");
    const detailsTir  = document.getElementById("details-tir");

    penaliteMsg.textContent = `${series.length} tir(s) enregistré(s)`;

    // Construit le HTML pour afficher les détails de chaque série
    let html = "";
    series.forEach((s, i) => {
        html += `<strong>Tir ${i + 1}</strong> — ${s.reussites}/5 en ${s.temps}s — note : ${s.note}<br>`;
    });

    // Calcule le total des réussites et le maximum possible, puis affiche le total en bas de la liste
    const total = series.reduce((acc, s) => acc + s.reussites, 0);
    const max   = series.length * 5;
    html += `<br><strong>Total : ${total} / ${max}</strong>`;

    detailsTir.innerHTML = html;
}

/**
 * Envoie les données de la session de tir au serveur via une requête POST
 * @returns {Promise<void>} Une promesse qui se résout lorsque la requête est terminée, avec gestion des erreurs et mise à jour de l'interface en conséquence
 */
async function envoyerSession() {
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
        tempsAuPasDeTir: series.map(s => s.temps),
        tempsAuTour:     []
    };

    // Désactive le bouton d'envoi et change son texte pour indiquer que l'envoi est en cours
    btnSession.disabled = true;
    btnSession.textContent = "Envoi en cours...";

    // Envoie la requête POST au serveur et gère la réponse
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

// Ajout des écouteurs d'événements pour les boutons et le champ de saisie
startBtn.addEventListener("click", demarrerChrono);
stopBtn.addEventListener("click", arreterChrono);
if (validerBtn) validerBtn.addEventListener("click", calculerEtAfficher);
input.addEventListener("keypress", (e) => { if (e.key === "Enter") calculerEtAfficher(); });
btnSession.addEventListener("click", envoyerSession);
