/* === INITIALISATION DES ÉLÉMENTS === */
const badgeNomEleve = document.getElementById("nom-eleve");
const listeHistorique = document.getElementById("liste-historique");
const msgChargement = document.getElementById("msg-chargement");
const templateCarte = document.getElementById("template-carte-historique");

let eleveActif = null;

const auditDico4eme = {
    intensite: { 'Contrôlé': '🟢<br>Peu essoufflé', 'Intense': '🟡<br>Effort soutenu', 'Critique': '🟠<br>Gros souffle', 'Saturation': '🔴<br>Épuisé' },
    durer: { 'Régulier': '✅<br>Vitesse stable', 'Économie': '🐢<br>Gardé de la réserve', 'Décroissant': '📉<br>Fin course difficile' },
    lucidite: { 'Équilibré': '⚖️<br>Rapide et précis', 'Prudent': '🎯<br>Calme et appliqué', 'Instable': '🤠<br>Précipité / Tremblant' }
};

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
 * Fonction utilitaire pour configurer une médaille facilement
 * @param carte - La carte dans laquelle se trouve l'élément de médaille à configurer
 * @param selecteur - Le sélecteur CSS pour trouver l'élément de médaille dans la carte (ex: ".archive-vitesse-medaille")
 * @param nomMedaille - Le nom de la médaille à afficher (ex: "OR", "ARGENT", "DIAMANT", ou null/undefined pour cacher)
 */
const configurerMedaille = (carte, selecteur, nomMedaille) => {
    // Trouver l'élément de médaille dans la carte
    const el = carte.querySelector(selecteur);

    // Si une médaille est spécifiée et qu'elle n'est pas "Sans Médaille", on l'affiche avec le style approprié
    if (nomMedaille && nomMedaille !== "Sans Médaille") {
        el.style.display = "block";
        el.textContent = "Médaille : " + nomMedaille;
        el.style.backgroundColor = couleursMedailles[nomMedaille.toLowerCase()] || "#ccc";
        el.style.color = ["or", "platine"].includes(nomMedaille.toLowerCase()) ? "black" : "white";
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
        // Appel à l'API pour récupérer l'historique de l'élève
        // const response = await fetch(`/api/eleves/historique/${idEleve}`);
        throw new Error("Données non disponible - mode démo activé");
    } catch (error) {
        // --- MODE DÉMO AVEC DONNÉES COMPLEXES ---
        const faussesDonnees = [
            {
                // ==== TEST 4ÈME ====
                dateObj: new Date("2026-03-12T10:00:00"),
                dateStr: "12/03/2026",
                type: "Épreuve Finale 4ème",
                noteFinale: 10.5,
                bilan: {
                    vitesseVal: 102,
                    vitesseRealiseeKmh: "12.2",
                    noteVitesse: 3,
                    medailleVitesse: "OR",

                    tirVal: 8,
                    tirTemps: "1'35",
                    noteTir: 5,

                    vmaVal: 12,
                    noteVma: 1.5,
                    medailleVma: "ARGENT"
                },
                // Utilisation du vocabulaire d'audit 4ème
                audit: { intensite: 'Intense', durer: 'Régulier', lucidite: 'Équilibré' }
            },
            {
                // ==== TEST 6ÈME ====
                dateObj: new Date("2026-03-15T14:30:00"), // Date plus récente, apparaîtra en premier !
                dateStr: "15/03/2026",
                type: "Épreuve Finale 6ème",
                noteFinale: 13.5, // Note sur 15 pour les 6èmes
                bilan: {
                    // Performance
                    nbTours: 8,
                    notePerf: 4.5,
                    medaillePerf: "OR",

                    // Régularité
                    ecartMax: 4,
                    noteRegul: 5,
                    medailleRegul: "DIAMANT",

                    // Tir
                    totalTir: 32, // ex: 32 réussites sur 40 tirs (8 tours * 5)
                    noteTir: 4,
                    medailleTir: "ARGENT"
                },
                // Utilisation du vocabulaire d'audit 6ème
                audit: { intensite: 'Chaud', durer: 'Bien', lucidite: 'Zen' }
            }
        ];

        // Trier les activités de la plus récente à la plus ancienne
        faussesDonnees.sort((a, b) => b.dateObj - a.dateObj);

        // Affichage des cartes d'historique avec les données de démonstration
        afficherCartesHistorique(faussesDonnees);
    }
};

/**
 * Affiche les cartes d'historique à partir des données fournies
 * @param {Array} donnees - Un tableau d'objets représentant les différentes activités de l'élève
 */
const afficherCartesHistorique = (donnees) => {
    // Masquer le message de chargement
    if (msgChargement) msgChargement.style.display = "none";

    // Si aucune donnée n'est disponible, afficher un message d'information
    if (!donnees || donnees.length === 0) {
        listeHistorique.innerHTML = "<p class='text-center-italic'>Aucun historique trouvé.</p>";
        return;
    }

    donnees.forEach(resultat => {
        const carte = templateCarte.cloneNode(true);
        carte.id = "";
        carte.style.display = "block";

        const is6eme = resultat.type.toLowerCase().includes("6ème") || resultat.type.toLowerCase().includes("6eme");

        // Remplissage de l'En-tête
        carte.querySelector(".archive-titre").textContent = resultat.type;
        carte.querySelector(".archive-date").textContent = resultat.dateStr;

        // Remplissage du Bilan (Différent pour 6ème et 4ème)
        if (is6eme) {
            // ==========================================
            // LOGIQUE 6ÈME (Note sur 15)
            // ==========================================
            carte.querySelector(".archive-note").textContent = `${resultat.noteFinale} / 15`;

            // Colonne 1 : Performance
            carte.querySelector(".archive-col1-titre").textContent = "Performance";
            carte.querySelector(".archive-col1-val").innerHTML = `<strong>Nombre de tours : </strong>${resultat.bilan.nbTours} tours`;
            carte.querySelector(".archive-col1-note").innerHTML = `<strong>Note : </strong> <span>${resultat.bilan.notePerf} / 5</span>`;
            configurerMedaille(carte, ".archive-col1-medaille", resultat.bilan.medaillePerf);

            // Colonne 2 : Régularité
            carte.querySelector(".archive-col2-titre").textContent = "Régularité";
            carte.querySelector(".archive-col2-val").innerHTML = `<strong>Écart max : </strong>${resultat.bilan.ecartMax}s`;
            carte.querySelector(".archive-col2-note").innerHTML = `<strong>Note : </strong> <span>${resultat.bilan.noteRegul} / 5</span>`;
            configurerMedaille(carte, ".archive-col2-medaille", resultat.bilan.medailleRegul);

            // Colonne 3 : Tir
            carte.querySelector(".archive-col3-titre").textContent = "Efficacité Tir";
            carte.querySelector(".archive-col3-val").innerHTML = `<strong>Réussites : </strong>${resultat.bilan.totalTir} / ${resultat.bilan.nbTours * 5}`;
            carte.querySelector(".archive-col3-note").innerHTML = `<strong>Note : </strong> <span>${resultat.bilan.noteTir} / 5</span>`;
            configurerMedaille(carte, ".archive-col3-medaille", resultat.bilan.medailleTir);

        } else {
            // ==========================================
            // LOGIQUE 4ÈME (Note sur 12)
            // ==========================================
            carte.querySelector(".archive-note").textContent = `${resultat.noteFinale} / 12`;

            // Colonne 1 : Intensité
            carte.querySelector(".archive-col1-titre").textContent = "Vitesse (Intensité)";
            carte.querySelector(".archive-col1-val").innerHTML = `<strong>Intensité : </strong>${resultat.bilan.vitesseVal}% de ta VMA`;
            if (resultat.bilan.vitesseRealiseeKmh) {
                const det1 = carte.querySelector(".archive-col1-detail");
                det1.style.display = "block";
                det1.innerHTML = `<strong>Ma vitesse : </strong>${resultat.bilan.vitesseRealiseeKmh} km/h`;
            }
            carte.querySelector(".archive-col1-note").innerHTML = `<strong>Note : </strong> <span>${resultat.bilan.noteVitesse} / 4</span>`;
            configurerMedaille(carte, ".archive-col1-medaille", resultat.bilan.medailleVitesse);

            // Colonne 2 : Tir
            carte.querySelector(".archive-col2-titre").textContent = "Efficience Tir";
            carte.querySelector(".archive-col2-val").innerHTML = `<strong>Précision : </strong>${resultat.bilan.tirVal} cibles touchées`;
            const det2 = carte.querySelector(".archive-col2-detail");
            det2.style.display = "block";
            det2.innerHTML = `<strong>Temps : </strong>${resultat.bilan.tirTemps}`;
            carte.querySelector(".archive-col2-note").innerHTML = `<strong>Note : </strong> <span>${resultat.bilan.noteTir} / 6</span>`;
            carte.querySelector(".archive-col2-medaille").style.display = "none"; // Pas de médaille pour le tir 4ème

            // Colonne 3 : VMA
            carte.querySelector(".archive-col3-titre").textContent = "VMA";
            carte.querySelector(".archive-col3-val").innerHTML = `<strong>VMA de réf : </strong>${resultat.bilan.vmaVal} km/h`;
            carte.querySelector(".archive-col3-note").innerHTML = `<strong>Note : </strong> <span>${resultat.bilan.noteVma} / 2</span>`;
            configurerMedaille(carte, ".archive-col3-medaille", resultat.bilan.medailleVma);
        }

        // ==========================================
        // Remplissage de l'Audit (Génération des boutons)
        // ==========================================

        // 1. On récupère les deux conteneurs de la carte clonée
        const zoneAudit4eme = carte.querySelector(".archive-audit-4eme");
        const zoneAudit6eme = carte.querySelector(".archive-audit-6eme");

        // 2. On affiche le bon conteneur et on y injecte le HTML
        if (is6eme) {
            zoneAudit6eme.style.display = "block"; // On affiche l'audit 6ème
            zoneAudit4eme.style.display = "none";  // On s'assure que l'autre est masqué
            zoneAudit6eme.innerHTML = genererAuditHTML(resultat.audit, true); // On génère avec is6eme=true
        } else {
            zoneAudit4eme.style.display = "block"; // On affiche l'audit 4ème
            zoneAudit6eme.style.display = "none";  // On s'assure que l'autre est masqué
            zoneAudit4eme.innerHTML = genererAuditHTML(resultat.audit, false); // On génère avec is6eme=false
        }

        // Gestion du Clic (Accordéon)
        const header = carte.querySelector(".archive-header");
        const content = carte.querySelector(".archive-content");
        const toggle = carte.querySelector(".archive-toggle");

        // Initialement, le contenu est caché, on affiche seulement l'en-tête
        // Si on clique sur l'en-tête, on bascule l'affichage du contenu et on change le symbole de toggle
        header.addEventListener("click", () => {
            const estFerme = content.style.display === "none";
            content.style.display = estFerme ? "block" : "none";
            toggle.textContent = estFerme ? "▲" : "▼";
        });

        listeHistorique.appendChild(carte);
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