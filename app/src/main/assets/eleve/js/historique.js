/* === INITIALISATION DES ÉLÉMENTS === */
const badgeNomEleve = document.getElementById("nom-eleve");
const listeHistorique = document.getElementById("liste-historique");
const msgChargement = document.getElementById("msg-chargement");

/** Variable globale pour stocker les données de l'élève actif (initialisée dans initialiserProfil) */
let eleveActif = null;

/** Dictionnaires pour traduire les choix d'audit en labels avec emojis (différents pour 4ème et 6ème) */
const auditDico4eme = {
    intensite: { 'Contrôlé': '🟢<br>Peu essoufflé', 'Intense': '🟡<br>Effort soutenu', 'Critique': '🟠<br>Gros souffle', 'Saturation': '🔴<br>Épuisé' },
    durer: { 'Régulier': '✅<br>Vitesse stable', 'Économie': '🐢<br>Gardé de la réserve', 'Décroissant': '📉<br>Fin course difficile' },
    lucidite: { 'Équilibré': '⚖️<br>Rapide et précis', 'Prudent': '🎯<br>Calme et appliqué', 'Instable': '🤠<br>Précipité / Tremblant' }
};

/** Dictionnaire pour les 6èmes avec des labels adaptés à leur vocabulaire d'audit (et des emojis plus "fun") */
const auditDico6eme = {
    intensite: { 'Tranquille': '🟢<br>Je parle', 'Chaud': '🟡<br>Un peu dur', 'Essoufflé': '🟠<br>Je souffle', 'À bout': '🔴<br>À bout' },
    durer: { 'Lent': '🐢<br>Trop tranquille', 'Bien': '✅<br>Allure régulière', 'Vite': '🥵<br>Parti trop vite' },
    lucidite: { 'Zen': '🎯<br>Bien posé', 'Bouge': '⚖️<br>Je tremble', 'Vite': '🤠<br>Vite et mal' }
};

/** Couleurs associées aux médailles pour un affichage cohérent */
const couleursMedailles = {
    "diamant": "#1456DB", "platine": "#b9f2ff", "or": "#ffd700", "argent": "#c0c0c0", "bronze": "#cd7f32"
};

/**
 * Formate un temps donné en millisecondes au format "MM:SS:CS" (minutes, secondes, centièmes de seconde)
 * @param ms - Le temps à formater en millisecondes
 * @returns {string} Le temps formaté sous forme de chaîne de caractères, ou "-" si le temps est nul ou négatif
 */
const formatTemps = (ms) => {
    if (!ms || ms <= 0) return "-";
    const totalSec = Math.floor(ms / 1000);
    const m = Math.floor(totalSec / 60);
    const s = totalSec % 60;
    const cs = Math.floor((ms % 1000) / 10);
    return `${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}:${String(cs).padStart(2,'0')}`;
};

/**
 * Calcule la note d'efficience au tir pour les 4èmes (Entraînements)
 */
const calculerNoteEfficience = (secondes, reussite) => {
    if (reussite === 0) return 0;

    // --- Note Temps ---
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

    // --- Note Réussite ---
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

    return Math.round(((noteTemps + noteReussite) / 2) * 2) / 2;
};

/**
 * Fonction utilitaire pour configurer une médaille facilement
 * @param carte - La carte dans laquelle se trouve l'élément de médaille à configurer
 * @param selecteur - Le sélecteur CSS pour trouver l'élément de médaille dans la carte (ex: ".archive-vitesse-medaille")
 * @param nomMedaille - Le nom de la médaille à afficher (ex: "OR", "ARGENT", "DIAMANT", ou null/undefined pour cacher)
 */
const configurerMedaille = (carte, selecteur, nomMedaille) => {
    const el = carte.querySelector(selecteur);
    if (nomMedaille && nomMedaille !== "Sans Médaille" && nomMedaille !== "—") {
        el.style.display = "block";
        el.textContent = "Médaille : " + nomMedaille;
        const couleurClé = nomMedaille.toLowerCase().replace(/💎|🏆|🥈|🥉/g, '').trim(); // Nettoie les émojis si présents
        el.style.backgroundColor = couleursMedailles[couleurClé] || "#ccc";
        el.style.color = ["or", "platine"].includes(couleurClé) ? "black" : "white";
        el.style.padding = "20px";
        el.style.borderRadius = "12px";
        el.style.fontSize = "1.4rem";
    } else {
        el.style.display = "none";
    }
};

/* === GESTION DE L'AFFICHAGE === */
/** * Initialise le profil de l'élève actif et charge son historique */
const initialiserProfil = () => {
    // Récupération des données du coureur actif
    const coureurData = localStorage.getItem("coureur_actif_objet");

    // Si des données sont présentes, on les affiche et on charge l'historique
    // Sinon, on affiche un message d'erreur et on invite à s'identifier
    if (coureurData) {
        eleveActif = JSON.parse(coureurData);
        const prenom = eleveActif.prenom || "Inconnu";
        const nom = eleveActif.nom || "Anonyme";
        badgeNomEleve.textContent = `${prenom} ${nom.toUpperCase()}`;

        chargerHistorique(eleveActif.id_eleve);
    } else {
        badgeNomEleve.textContent = "Aucun élève sélectionné";
        badgeNomEleve.classList.replace("bg-bleu", "bg-rouge");
        msgChargement.textContent = "Veuillez vous identifier.";
    }
};

/**
 * Charge l'historique de l'élève depuis le serveur (simulation pour le moment)
 * @param idEleve L'identifiant de l'élève dont on veut charger l'historique
 * @returns {Promise<void>} une promesse qui se résout une fois l'historique chargé et affiché
 */
const chargerHistorique = async (idEleve) => {
    try {
        // On interroge le serveur (qui lui, a la base de données centrale)
        const response = await fetch(`/api/eleves/historique/${idEleve}`);

        if (!response.ok) {
            throw new Error(`Erreur serveur: ${response.status}`);
        }

        const historiqueReel = await response.json();

        // Tri par date décroissante
        historiqueReel.sort((a, b) => b.dateObj - a.dateObj);

        afficherCartesHistorique(historiqueReel);

    } catch (error) {
        console.error("Erreur de synchro :", error);
        msgChargement.textContent = "Impossible de synchroniser ton historique.";
        msgChargement.style.color = "red";
    }
};

/**
 * Affiche les cartes d'historique à partir des données fournies, en utilisant
 * 4 templates HTML différents (Épreuve 4ème, Épreuve 6ème, Entraînement 4ème, Entraînement 6ème).
 * @param {Array} donnees - Un tableau d'objets représentant les différentes activités de l'élève
 */
const afficherCartesHistorique = (donnees) => {
    if (msgChargement) msgChargement.style.display = "none";

    // 1. On filtre les données pour EXCLURE les tests VMA
    const historiqueFiltre = donnees.filter(resultat => {
        const typeSeance = (resultat.type || "").toLowerCase();
        // On ne garde la séance QUE SI elle ne contient pas le mot "vma"
        return !typeSeance.includes("vma");
    });

    // 2. On vérifie s'il reste des choses à afficher après le filtre
    if (!historiqueFiltre || historiqueFiltre.length === 0) {
        listeHistorique.innerHTML = "<p class='text-center-italic'>Aucun historique de biathlon trouvé.</p>";
        return;
    }

    historiqueFiltre.forEach(resultat => {
        // 🛡️ SÉCURITÉ : Un try/catch pour éviter qu'une séance buggée bloque tout l'historique
        try {
            const typeSeance = (resultat.type || "").toLowerCase();
            const isEpreuve = typeSeance.includes("épreuve") || typeSeance.includes("epreuve");

            // 🛡️ SÉCURITÉ : Si "6ème" n'est pas dans le titre de la séance, on regarde la classe de l'élève !
            const classeEleve = eleveActif && eleveActif.classe ? eleveActif.classe.toString() : "";
            const is6eme = typeSeance.includes("6ème") || typeSeance.includes("6eme") || classeEleve.includes("6");

            // 1. CHOIX DU TEMPLATE
            let templateId = "";
            if (isEpreuve && is6eme) templateId = "template-epreuve-6eme";
            else if (isEpreuve && !is6eme) templateId = "template-epreuve-4eme";
            else if (!isEpreuve && is6eme) templateId = "template-entrainement-6eme";
            else templateId = "template-entrainement-4eme";

            const templateCarte = document.getElementById(templateId);
            if (!templateCarte) return; // Si le template manque, on passe au suivant

            const carte = templateCarte.cloneNode(true);
            carte.id = "";
            carte.style.display = "block";

            // 2. EN-TÊTE COMMUN
            carte.querySelector(".archive-titre").textContent = resultat.type;
            carte.querySelector(".archive-date").textContent = resultat.dateStr;

            // 3. REMPLISSAGE SPÉCIFIQUE
            if (isEpreuve && is6eme) {
                const bilan = resultat.bilan || {}; // Sécurité
                carte.querySelector(".archive-note").textContent = `${resultat.noteFinale || 0} / 15`;

                carte.querySelector(".archive-col1-val").innerHTML = `<strong>Tours : </strong>${bilan.nbTours || 0}`;
                carte.querySelector(".archive-col1-note").innerHTML = `<strong>Note : </strong> <span>${bilan.notePerf || 0} / 5</span>`;
                configurerMedaille(carte, ".archive-col1-medaille", bilan.medaillePerf);

                carte.querySelector(".archive-col2-val").innerHTML = `<strong>Écart max : </strong>${bilan.ecartMax || 0}s`;
                carte.querySelector(".archive-col2-note").innerHTML = `<strong>Note : </strong> <span>${bilan.noteRegul || 0} / 5</span>`;
                configurerMedaille(carte, ".archive-col2-medaille", bilan.medailleRegul);

                carte.querySelector(".archive-col3-val").innerHTML = `<strong>Réussites : </strong>${bilan.totalTir || 0} / ${(bilan.nbTours || 0) * 5}`;
                carte.querySelector(".archive-col3-note").innerHTML = `<strong>Note : </strong> <span>${bilan.noteTir || 0} / 5</span>`;
                configurerMedaille(carte, ".archive-col3-medaille", bilan.medailleTir);

                if (resultat.audit && Object.keys(resultat.audit).length > 0) {
                    const zoneAudit = carte.querySelector(".archive-audit-6eme");
                    if (zoneAudit) zoneAudit.innerHTML = genererAuditHTML(resultat.audit, true);
                }

            } else if (isEpreuve && !is6eme) {
                const bilan = resultat.bilan || {}; // Sécurité
                carte.querySelector(".archive-note").textContent = `${resultat.noteFinale || 0} / 12`;

                carte.querySelector(".archive-col1-val").innerHTML = `<strong>Intensité : </strong>${bilan.vitesseVal || 0}% VMA`;
                if (bilan.vitesseRealiseeKmh) {
                    const det1 = carte.querySelector(".archive-col1-detail");
                    if (det1) {
                        det1.style.display = "block";
                        det1.innerHTML = `<strong>Vitesse : </strong>${bilan.vitesseRealiseeKmh} km/h`;
                    }
                }
                carte.querySelector(".archive-col1-note").innerHTML = `<strong>Note : </strong> <span>${bilan.noteVitesse || 0} / 4</span>`;
                configurerMedaille(carte, ".archive-col1-medaille", bilan.medailleVitesse);

                carte.querySelector(".archive-col2-val").innerHTML = `<strong>Précision : </strong>${bilan.tirVal || 0} cibles`;
                const det2 = carte.querySelector(".archive-col2-detail");
                if (det2) {
                    det2.style.display = "block";
                    const tempsTirSecondes = Math.floor((bilan.tirTempsMs || 0) / 1000);
                    det2.innerHTML = `<strong>Temps : </strong>${tempsTirSecondes}s`;
                }

                carte.querySelector(".archive-col2-note").innerHTML = `<strong>Note : </strong> <span>${bilan.noteTir || 0} / 6</span>`;

                carte.querySelector(".archive-col3-val").innerHTML = `<strong>VMA réf : </strong>${bilan.vmaVal || 0} km/h`;
                carte.querySelector(".archive-col3-note").innerHTML = `<strong>Note : </strong> <span>${bilan.noteVma || 0} / 2</span>`;
                configurerMedaille(carte, ".archive-col3-medaille", bilan.medailleVma);

                if (resultat.audit && Object.keys(resultat.audit).length > 0) {
                    const zoneAudit = carte.querySelector(".archive-audit-4eme");
                    if (zoneAudit) zoneAudit.innerHTML = genererAuditHTML(resultat.audit, false);
                }

            } else {
                // ENTRAÎNEMENT
                const zoneDetails = carte.querySelector(".archive-details-entrainement");
                let htmlEntrainement = "";

                // 🛡️ CORRECTION MAJEURE ICI : Les entraînements peuvent ne pas avoir l'enveloppe "bilan"
                const data = resultat.bilan || resultat || {};

                if (data.tours && data.tours.length > 0) {
                    htmlEntrainement += `<h4 style="margin: 15px 0 10px 0; color: #303586; font-size: 1.2rem;">🏃 Course détaillée</h4>`;
                    htmlEntrainement += `<div style="background: #f8f9fa; padding: 15px; border-radius: 8px; border-left: 5px solid #e74c3c;">`;
                    data.tours.forEach((tourMs, index) => {
                        htmlEntrainement += `<div style="margin-bottom: 5px; font-size: 1.1rem;"><strong>Tour ${index + 1}:</strong> ${formatTemps(tourMs)}</div>`;
                    });
                    htmlEntrainement += `</div>`;
                }

                if (data.tirs && data.tirs.length > 0) {
                    htmlEntrainement += `<h4 style="margin: 20px 0 10px 0; color: #303586; font-size: 1.2rem;">🎯 Tir détaillé</h4>`;

                    if (is6eme) {
                        htmlEntrainement += `<div style="display: flex; flex-direction: column; gap: 10px;">`;
                        let totalReussites = 0;
                        data.tirs.forEach((score, index) => {
                            // Sécurité pour gérer les anciens et nouveaux formats
                            const valScore = (typeof score === 'object' && score !== null) ? (score.reussites || 0) : score;
                            totalReussites += valScore;
                            let couleurScore = valScore >= 4 ? "#27ae60" : (valScore >= 2 ? "#f39c12" : "#e74c3c");
                            htmlEntrainement += `
                            <div style="display: flex; justify-content: space-between; align-items: center; background-color: #f8f9fa; padding: 12px 15px; border-radius: 8px; border-left: 5px solid #303586;">
                                <div style="font-weight: bold; font-size: 1.1rem;">Série ${index + 1}</div>
                                <span style="background-color: ${couleurScore}; color: white; padding: 4px 12px; border-radius: 12px; font-weight: bold; font-size: 1.1rem;">${valScore} / 5</span>
                            </div>`;
                        });
                        htmlEntrainement += `</div>`;

                        const maxPossible = data.tirs.length * 5;
                        const pct = maxPossible > 0 ? (totalReussites / maxPossible) * 100 : 0;
                        let medaille = "BRONZE", couleur = "#cd7f32";
                        if (pct >= 90)      { medaille = "DIAMANT"; couleur = "#1456DB"; }
                        else if (pct >= 80) { medaille = "PLATINE"; couleur = "#b9f2ff"; }
                        else if (pct >= 70) { medaille = "OR";      couleur = "#ffd700"; }
                        else if (pct >= 60) { medaille = "ARGENT";  couleur = "#c0c0c0"; }

                        htmlEntrainement += `
                        <div style="margin-top: 20px; padding: 15px; border-radius: 8px; border: 2px solid #333; text-align: center;">
                            <p style="font-size: 1.6rem; margin: 0;">Total réussites : <span style="font-weight: bold;">${totalReussites} / ${maxPossible}</span></p>
                            <div style="margin-top: 15px; font-weight: bold; color: white; background-color:${couleur}; padding:15px; border-radius:12px; font-size:1.4rem;">Médaille : ${medaille}</div>
                        </div>`;

                    } else {
                        htmlEntrainement += `<div style="background: #f8f9fa; padding: 15px; border-radius: 8px; border-left: 5px solid #303586;">`;
                        let totalReussites = 0;
                        let totalTempsMs = 0;

                        data.tirs.forEach((s, i) => {
                            const reussites = typeof s === 'object' ? (s.reussites || 0) : s;
                            const tempsMs = typeof s === 'object' ? (s.tempsMs || 0) : 0;

                            totalReussites += reussites;
                            totalTempsMs += tempsMs;

                            let couleurScore = reussites >= 4 ? "#27ae60" : (reussites >= 2 ? "#f39c12" : "#e74c3c");
                            let secondesTir = Math.floor(tempsMs / 1000);

                            htmlEntrainement += `
                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 5px;">
                                <div style="font-weight: bold; font-size: 1.1rem;">Tir ${i + 1}</div>
                                <div>
                                    <span style="color: ${couleurScore}; font-weight: bold; font-size: 1.1rem; margin-right: 15px;">${reussites} / 5</span>
                                    <span style="color: #666; font-size: 1.1rem;">⏱️ ${secondesTir}s</span>
                                </div>
                            </div>
                            <hr style="border:none; border-top: 1px solid #ccc; margin: 10px 0;">`;
                        });
                        htmlEntrainement += `</div>`;

                        const maxCibles = data.tirs.length * 5;
                        const totalSecondes = Math.floor(totalTempsMs / 1000);
                        const noteGlobale = calculerNoteEfficience(totalSecondes, totalReussites);

                        htmlEntrainement += `
                        <div style="margin-top: 25px; padding: 15px; border-radius: 8px; border: 2px solid #303586; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1);">
                            <h3 style="margin: 0 0 15px 0; color: #303586; font-size: 1.3rem;">BILAN GLOBAL</h3>
                            <p style="font-size: 1.1rem; margin: 5px 0;"><strong>Précision :</strong> ${totalReussites} / ${maxCibles} cibles</p>
                            <p style="font-size: 1.1rem; margin: 5px 0;"><strong>Temps Cumulé :</strong> ${totalSecondes} secondes</p>
                            <hr style="border:none; border-top: 1px solid #303586; margin: 15px 0;">
                            <p style="font-size: 1.3rem; margin: 0; color: #333;">Note Efficience : <strong style="color:#303586;">${noteGlobale} / 6</strong></p>
                        </div>`;
                    }
                }

                if (htmlEntrainement === "") {
                    htmlEntrainement = "<p class='text-center-italic'>Aucune donnée détaillée enregistrée pour cette séance.</p>";
                }

                if (zoneDetails) {
                    zoneDetails.innerHTML = htmlEntrainement;
                }
            }

            // 4. ACCORDÉON
            const header = carte.querySelector(".archive-header");
            const content = carte.querySelector(".archive-content");
            const toggle = carte.querySelector(".archive-toggle");

            if (header && content && toggle) {
                header.addEventListener("click", () => {
                    const estFerme = content.style.display === "none";
                    content.style.display = estFerme ? "block" : "none";
                    toggle.textContent = estFerme ? "▲" : "▼";
                });
            }

            listeHistorique.appendChild(carte);

        } catch (erreur) {
            // Si une séance plante, on l'affiche dans la console mais on NE BLOQUE PAS le reste !
            console.error("Impossible de charger cette entrée d'historique :", resultat, erreur);
        }
    });
};

/**
 * Génère le HTML de l'audit en surlignant les choix passés de l'élève
 * @param {object} choixEleve - L'objet contenant les choix de l'élève (intensite, durer, lucidite)
 * @param {boolean} is6eme - Vrai si l'épreuve est de niveau 6ème, faux sinon
 * @returns {string} Le HTML généré pour la section audit
 */
const genererAuditHTML = (choixEleve, is6eme) => {
    let html = "";

    // 1. Choix du bon dictionnaire selon le niveau
    const dicoAUtiliser = is6eme ? auditDico6eme : auditDico4eme;

    // 2. Choix des bons labels de section selon le niveau (copiés de tes modales)
    const sectionLabels = is6eme ?
        [
            { cle: 'intensite', titre: "Mon essoufflement" },
            { cle: 'durer', titre: "Ma vitesse sur les 20 minutes" },
            { cle: 'lucidite', titre: "Ma maîtrise au moment du tir" }
        ] :
        [
            { cle: 'intensite', titre: "Engagement (Moteur)" },
            { cle: 'durer', titre: "Gestion de l'allure" },
            { cle: 'lucidite', titre: "Lucidité face aux cibles" }
        ];

    // 3. Parcours des sections et génération du HTML en surlignant les choix de l'élève
    sectionLabels.forEach(sec => {
        html += `<div class="audit-section" style="margin-bottom:10px;"><p style="margin-bottom:5px;"><strong>${sec.titre} :</strong></p><div class="btn-group-audit" style="display:flex; gap:5px;">`;

        // On parcourt les options possibles pour cette catégorie
        for (const [valeur, labelHTML] of Object.entries(dicoAUtiliser[sec.cle])) {
            // Si c'est ce que l'élève a choisi, on ajoute la classe 'selected'
            const isSelected = choixEleve[sec.cle] === valeur ? "selected" : "";
            // On désactive les clics avec pointer-events:none (déjà fait dans le HTML parent)
            html += `<button class="btn-audit ${isSelected}" style="flex:1; padding:5px; font-size:0.8rem;">${labelHTML}</button>`;
        }

        html += `</div></div>`;
    });
    return html;
};

// Appeler l'initialisation du profil au chargement de la page
document.addEventListener("DOMContentLoaded", initialiserProfil);