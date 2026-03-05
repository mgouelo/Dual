let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let stopBtn = document.getElementById("stop");
let startBtn = document.getElementById("start");
const modal = document.getElementById("custom-confirm");
const modalNormal = document.getElementById("confirm-normal");
const confirmOk = document.getElementById("confirm-ok");
const confirmOkNormal = document.getElementById("confirm-ok-normal");
const confirmCancel = document.getElementById("confirm-cancel");
let enregistrerBtn = document.getElementById("enregistrer");
let supprimerBtn = document.getElementById("supprimer");
const btnValiderTir = document.getElementById("valider-tir");
const terminerBtn = document.getElementById("terminer");
const btnVoirBilan = document.getElementById("btnVoirBilan");

// Variable pour savoir quand l'élève a commencé à courir (initialement à 20:00:00)
let tempsDepartCourse = { min: 20, sec: 0, ms: 0 };

let minutes = 20;
let secondes = 0;
let millisecondes = 0;
let timeout;
let estArrete = true;
let tourActuel = 1;

// Stockage des données pour le bilan final
let historiqueEpreuve = []; // Résultats de chaque tour (temps et score de tir)
let autoEval = {intensite: "", durer: "", lucidite: ""};


/* Cette fonction gère le déroulement du temps.
Elle s'appelle elle-même toutes les 10ms tant que le chronomètre n'est pas arrêté. */
const defilerTemps = () => {
    if (estArrete) return;

    millisecondes -= 10;

    if (millisecondes < 0) {
        millisecondes = 990;
        secondes--;
    }

    if (secondes < 0) {
        secondes = 59;
        minutes--;
    }

    // Arrêt automatique à zéro
    // Ajout d'une sécurité pour le chron afin de éviter les valeurs négatives en cas de bug ou de manipulation rapide des boutons
    if (minutes < 0 || (minutes === 0 && secondes === 0 && millisecondes <= 0)) {
        minutes = 0;
        secondes = 0;
        millisecondes = 0;
        estArrete = true;
        clearTimeout(timeout);
        chrono.textContent = "00:00:00";
        confirmation("Temps écoulé ! Fin de l'épreuve.");
        return;
    }

    // Affichage formaté avec des zéros, padStart pemet de faire ça facilement en convertissant les nombres en chaînes de caractères et en ajoutant des zéros devant si nécessaire
    let m = minutes.toString().padStart(2, '0');
    let s = secondes.toString().padStart(2, '0');
    let ms = Math.floor(millisecondes / 10).toString().padStart(2, '0');

    chrono.textContent = `${m}:${s}:${ms}`;

    timeout = setTimeout(defilerTemps, 10);
};

/* Démarre le chronomètre si il est arrêté. */
const demarrer = () => {
    if (estArrete) {
        estArrete = false;
        defilerTemps();
    }
};

/* Arrête le chronomètre si il est en cours. */
const arreter = () => {
    if (!estArrete) {
        estArrete = true;
        clearTimeout(timeout);
    }
};

/* Réinitialise le chronomètre et les tours après confirmation de l'utilisateur. */
const reset = async() => {
    const confirmation = await demanderConfirmation("Réinitialiser le chronomètre ?");

    if (confirmation) {
        estArrete = true;
        clearTimeout(timeout);

        // Remettre à la valeur de départ (20 minutes)
        minutes = 20;
        secondes = 0;
        millisecondes = 0;
        chrono.textContent = "20:00:00";
    }

    tempsDepartCourse = { min: 20, sec: 0, ms: 0 };
    historiqueEpreuve = [];
    tourActuel = 1;

    // Réinitialiser les tours
    const listeTours = document.getElementById("listeTours");
    if (listeTours) {
        listeTours.innerHTML = "";
    }
};

/* Enregistre le temps et ouvre la saisie du tir */
const enregistrer = () => {
    // On vérifie que le chrono a tourné et n'est pas à 20:00:00 (compte à rebours)
    if(chrono.textContent !== "20:00:00" && !estArrete) {

        // Calcul de la durée de course (Ref précédente - Chrono actuel)
        let dureeCourse = (tempsDepartCourse.min * 60 + tempsDepartCourse.sec) - (minutes * 60 + secondes);
        dureeCourse = Math.max(0, dureeCourse); // Sécurité pour éviter les valeurs négatives en cas de bug ou de manipulation rapide des boutons

        // On stocke temporairement cette durée pour la valider plus tard
        document.getElementById("temps-tour-cache").value = dureeCourse;
        tempsDebutTir = { min: minutes, sec: secondes }; // On mémorise le moment où on a commencé à tirer

        // On prépare la modale de tir et affiche la salve de tir (la modale)
        document.getElementById("titre-tir").textContent = `Score Tir - Tour ${tourActuel}`;
        document.getElementById("modal-tir").style.display = "flex";
        document.getElementById("modal-tir").classList.add("show");
    }
};

/**
 * Met à jour le score de tir sélectionné dans le champ caché
 * @param {number} valeur - Le nombre de cibles touchées (0 à 5)
 */
function setScoreTir(valeur) {
    // On enregistre la valeur dans l'input hidden pour validerTourEtTir
    document.getElementById("score-temporaire").value = valeur;

    // Ajoute un retour visuel (couleur) sur le bouton cliqué
    const boutons = document.querySelectorAll('.btn-score');
    boutons.forEach(btn => {
        btn.classList.remove('selected'); // On retire la couleur de tous les boutons
        if(parseInt(btn.textContent) === valeur) {
            btn.classList.add('selected'); // On colore le bouton sélectionné
        }
    });
}

/**
 * Valide le tour en cours en enregistrant le score de tir et le temps, puis met à jour l'affichage de la liste des tours.
 * Cette fonction est appelée lorsque l'utilisateur clique sur le bouton de validation dans la modale de tir.
 */
const validerTourEtTir = () => {
    // Vérification que l'utilisateur a bien sélectionné un score de tir avant de valider
    let btnSelectionne = false; // Variable pour vérifier si un bouton a été sélectionné
    document.querySelectorAll('.btn-score').forEach(button => {
        // Vérification que il y a au moins un bouton de sélectionner
        if (button.classList.contains('selected')) {
            btnSelectionne = true;
        }
    });
    if (!btnSelectionne) {
        confirmation("Veuillez sélectionner un score de tir avant de valider.");
        return;
    }

    const score = parseInt(document.getElementById("score-temporaire").value);
    const dureeCourse = parseInt(document.getElementById("temps-tour-cache").value);

    // Calcul de la durée de tir (Début du tir - Chrono actuel)
    let dureeTir = (tempsDebutTir.min * 60 + tempsDebutTir.sec) - (minutes * 60 + secondes);

    // Stockage de l'historique pour le bilan final
    historiqueEpreuve.push({
        tour: tourActuel,
        course: dureeCourse,
        tir: dureeTir,
        totalTour: dureeCourse + dureeTir,
        scoreTir: score,
    });

    // Preparation du tour suivant : on considère que le départ du prochain tour est à l'heure actuelle du chrono
    tempsDepartCourse = { min: minutes, sec: secondes, ms: millisecondes };

    // Création de l'affichage dans la liste
    const listeTours = document.getElementById("listeTours");
    const nouveauTour = document.createElement("div");
    nouveauTour.style.marginBottom = "10px";
    nouveauTour.innerHTML = `
        <strong>Tour ${tourActuel} :</strong> Course ${dureeCourse}s | Tir <span class="badge-tir">${score}/5</span>
    `;
    listeTours.appendChild(nouveauTour);

    // Fermeture de la modale et incrémentation
    document.getElementById("modal-tir").style.display = "none";
    tourActuel++;

    // Reset de la sélection visuelle pour le prochain tour (les boutons de score)
    document.querySelectorAll('.btn-score').forEach(b => b.classList.remove('selected'));
};

/* Supprime le dernier tour enregistré de la liste des tours si le chronomètre n'est pas à zéro. */
const supprimer = () => {
    if(chrono.textContent != "00:00:00"){
        const listeTours = document.getElementById("listeTours"); // conteneur de tous les tours
        /* Vérifie s'il y a au moins un tour enregistré avant de tenter de supprimer le dernier. */
        if (listeTours.lastElementChild) {
            listeTours.lastElementChild.remove(); // Supprime le dernier enfant affiché
            tourActuel--;
        }
    }
};

/* Affiche une boîte de confirmation personnalisée et retourne une promesse qui se résout en fonction du choix de l'utilisateur. */
const demanderConfirmation = (message) => {
    document.getElementById("confirm-message").textContent = message;

    modal.style.display = "flex";
    // Petit timeout pour laisser le navigateur appliquer le display:flex
    // avant de lancer l'animation CSS
    setTimeout(() => {
        modal.classList.add("show");
    }, 10);

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

/* Affiche une boîte de confirmation personnalisée et retourne une promesse qui se résout en fonction du choix de l'utilisateur. */
const confirmation = (message) => {
    document.getElementById("confirm-message-normal").textContent = message;

    modalNormal.style.display = "flex";
    // Petit timeout pour laisser le navigateur appliquer le display:flex
    // avant de lancer l'animation CSS
    setTimeout(() => {
        modalNormal.classList.add("show");
    }, 10);

    return new Promise((resolve) => {
        confirmOkNormal.onclick = () => {
            modalNormal.classList.remove("show");
            setTimeout(() => { modalNormal.style.display = "none"; }, 300);
            resolve(true);
        };
    });
};

// Fonction asynchrone pour terminer l'épreuve, calculer les résultats et afficher le bilan final
const terminerEpreuve = ()=> {
    // Performance : Nombre de tours réalisés
    // Barème : 7 tours = 5pts, 6 tours = 4pts, etc.
    const nbTours = historiqueEpreuve.length;
    let notePerf = 0;
    if (nbTours >= 7) notePerf = 5;
    else if (nbTours >= 6.5) notePerf = 4.5;
    else if (nbTours >= 6) notePerf = 4;
    else if (nbTours >= 5.5) notePerf = 3.5;
    else if (nbTours >= 5) notePerf = 3;
    else if (nbTours >= 4.5) notePerf = 2.5;
    else if (nbTours >= 4) notePerf = 2;
    else if (nbTours >= 3.5) notePerf = 1.5;
    else if (nbTours >= 3) notePerf = 1;
    else notePerf = 0.5;

    // Régularité : Écart entre le meilleur et le pire temps de course
    // On compare les temps de course uniquement
    const tempsCourses = historiqueEpreuve.map(t => t.course);
    const maxCourse = Math.max(...tempsCourses);
    const minCourse = Math.min(...tempsCourses);
    const ecartMax = maxCourse - minCourse;
    let noteRegul = 0;
    if (ecartMax < 10) noteRegul = 5;
    else if (ecartMax <= 15) noteRegul = 4;
    else if (ecartMax <= 20) noteRegul = 3;
    else if (ecartMax <= 25) noteRegul = 2;
    else noteRegul = 1;

    // Tir : Total de points de tir sur tous les tours
    // Somme de tous les scores de tir
    const totalTir = historiqueEpreuve.reduce((sum, t) => sum + t.scoreTir, 0);
    const pourcentageTir = (totalTir / (nbTours * 5)) * 100;
    let noteTir = 0;
    // Barème : 21 = 5pts, 19 = 4pts, etc.
    if (totalTir >= 21) noteTir = 5;
    else if (totalTir >= 20) noteTir = 4.5;
    else if (totalTir >= 19) noteTir = 4;
    else if (totalTir >= 18) noteTir = 3.5;
    else if (totalTir >= 17) noteTir = 3;
    else if (totalTir >= 16) noteTir = 2.5;
    else if (totalTir >= 15) noteTir = 2;
    else if (totalTir >= 14) noteTir = 1.5;
    else if (totalTir >= 13) noteTir = 1;
    else noteTir = 0.5;

    let medaillePerf = nbTours >= 8 ? "DIAMANT" : nbTours >= 7 ? "PLATINE" : nbTours >= 6 ? "OR" : nbTours >= 5 ? "ARGENT" : "BRONZE";
    let medailleRegul = ecartMax < 10 ? "DIAMANT" : ecartMax <= 15 ? "PLATINE" : ecartMax <= 20 ? "OR" : ecartMax <= 25 ? "ARGENT" : "BRONZE";
    let medailleTir = pourcentageTir >= 85 ? "DIAMANT" : pourcentageTir >= 75 ? "PLATINE" : pourcentageTir >= 65 ? "OR" : pourcentageTir >= 55 ? "ARGENT" : "BRONZE";

    afficherResultatsFinaux(nbTours, notePerf, medaillePerf, ecartMax, medailleRegul, noteRegul, totalTir, noteTir, medailleTir);
};

/**
 * Enregistre le choix de l'élève pour l'auto-évaluation
 * @param {HTMLElement} element - Le bouton cliqué
 * @param {string} categorie - 'intensite', 'durer' ou 'lucidite'
 * @param {string} valeur - Le texte du ressenti
 */
function selectAudit(element, categorie, valeur) {
    // Désélectionner les boutons du même groupe
    const parent = element.parentElement;
    parent.querySelectorAll('.btn-audit').forEach(btn => btn.classList.remove('selected'));

    // Sélectionner le bouton cliqué
    element.classList.add('selected');

    // Stocker la valeur dans l'objet global
    autoEval[categorie] = valeur;
}

// Fonction déclenchée par le bouton "Voir mon Bilan"
const validerRessentis = async() => {
    // Vérifier que tous les ressentis ont été sélectionnés
    if (!autoEval.intensite || !autoEval.durer || !autoEval.lucidite) {
        confirmation("Veuillez répondre aux 3 questions de ressenti avant de voir le bilan.");
        return;
    }

    // Fermer la modale des ressentis
    const modalRessenti = document.getElementById("modal-ressenti");
    modalRessenti.style.display = "none";
    modalRessenti.classList.remove("show");

    terminerEpreuve();
};

// Fonction déclenchée par le bouton "Terminer l'épreuve"
const declencherFinEpreuve = async () => {
    // On demande confirmation d'abord
    const confirmationFin = await demanderConfirmation("Voulez-vous vraiment terminer l'épreuve ?");

    if (confirmationFin) {
        arreter(); // On stoppe le chrono immédiatement

        if (historiqueEpreuve.length === 0) {
            confirmation("Aucune donnée enregistrée pour cette épreuve.");
            return;
        }

        // On affiche la modale de ressenti
        const modalRessenti = document.getElementById("modal-ressenti");
        modalRessenti.style.display = "flex";
        setTimeout(() => modalRessenti.classList.add("show"), 10);
    }
};

const afficherResultatsFinaux = (nbTours, notePerf, medaillePerf, ecartMax, medailleRegul, noteRegul, totalTir, noteTir, medailleTir) => {
    // Calcul de la note totale sur 15
    const noteFinale = (parseFloat(notePerf) + parseFloat(noteRegul) + parseFloat(noteTir)).toFixed(1);

    // fonction utilitaire pour mettre à jour le texte d'un élément par son ID
    const majTexte = (id, texte) => {
        const el = document.getElementById(id);
        if (el) el.innerHTML = texte;
    };

    // Performance : Nombre de tours parcourus
    majTexte("res-tours", `<strong>Nombre de tours : </strong>${nbTours} tours`);
    majTexte("note-perf", `<strong>Note de la performance : </strong> <span>${notePerf} / 5</span>`);

    // Régularité : Écart maximal entre chaque tour de course
    majTexte("res-ecart", `<strong>Écart maximal de course : </strong>${ecartMax}s`);
    majTexte("note-regul", `<strong>Note de la régularité : </strong> <span>${noteRegul} / 5</span>`);

    // Efficacité Tir : Somme des réussites sur l'ensemble de l'épreuve
    majTexte("res-tir", `<strong>Total des réussites tir : </strong>${totalTir} / ${nbTours * 5}`);
    majTexte("note-tir", `<strong>Note du tir : </strong><span>${noteTir} / 5</span>`);

    // Note Finale sur 20
    const elNoteTotale = document.getElementById("note-totale");
    if (elNoteTotale) elNoteTotale.textContent = noteFinale;

    // Gestion de la médaille
    // Couleurs associées à chaque médaille pour un affichage plus visuel
    const couleurs = { "diamant": "#1456DB", "platine": "#b9f2ff", "or": "#ffd700", "argent": "#c0c0c0", "bronze": "#cd7f32" };
    const mDisplayReg = document.getElementById("medaille-display-reg");
    if (mDisplayReg && medailleRegul) {
        mDisplayReg.textContent = "Médaille : " + medailleRegul;
        mDisplayReg.style.backgroundColor = couleurs[medailleRegul.toLowerCase()] || "#ccc";
        mDisplayReg.style.padding = "20px";
        mDisplayReg.style.borderRadius = "12px";
        mDisplayReg.style.fontSize = "1.4rem";
    }
    const mDisplayTir = document.getElementById("medaille-display-tir");
    if (mDisplayTir && medailleTir) {
        mDisplayTir.textContent = "Médaille : " + medailleTir;
        mDisplayTir.style.backgroundColor = couleurs[medailleTir.toLowerCase()] || "#ccc";
        mDisplayTir.style.padding = "20px";
        mDisplayTir.style.borderRadius = "12px";
        mDisplayTir.style.fontSize = "1.4rem";
    }
    const mDisplayPerf = document.getElementById("medaille-display-perf");
    if (mDisplayPerf && medaillePerf) {
        mDisplayPerf.textContent = "Médaille : " + medaillePerf;
        mDisplayPerf.style.backgroundColor = couleurs[medaillePerf.toLowerCase()] || "#ccc";
        mDisplayPerf.style.padding = "20px";
        mDisplayPerf.style.borderRadius = "12px";
        mDisplayPerf.style.fontSize = "1.4rem";
    }

    // Affichage de la modale
    const modalBilan = document.getElementById("modal-bilan");
    if (modalBilan) {
        modalBilan.style.display = "flex";
        setTimeout(() => modalBilan.classList.add("show"), 10);
    } else {
        // Si la modale n'existe pas encore, on utilise une alerte de secours pour voir les résultats
        alert(`Bilan : Perf ${notePerf}/5, Régul ${noteRegul}/5, Tir ${noteTir}/5. Total : ${noteFinale}/15`);
    }
};

/**
 * Récupère les infos VMA du coureur actif et affiche son parcours coloré (Spécifique 6ème)
 */
const afficherParcoursVMA = () => {
    const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));
    //On récupère le niveau, par défaut 6eme pour ce fichier
    const niveau = localStorage.getItem("niveau") || "6eme";

    const displayZone = document.getElementById("vma-result-display");
    const badgeZone = document.getElementById("badge-parcours");

    if (displayZone && badgeZone) {
        displayZone.style.display = "block";

        if (coureur && coureur.vma && coureur.vma > 0) {
            const vma = parseFloat(coureur.vma);
            let badge = "";
            let parcours = "";

            // --- LOGIQUE BARÈME 6ÈME ---
            if (vma <= 9) {
                badge = "bg-jaune"; parcours = "Coupelles Jaunes (250m)";
            } else if (vma <= 10.5) {
                badge = "bg-vert"; parcours = "Plots Verts (275m)";
            } else if (vma <= 11.5) {
                badge = "bg-bleu"; parcours = "Coupelles Bleues (300m)";
            } else {
                badge = "bg-rouge"; parcours = "Coupelles Rouges (350m)";
            }

            //Mise à jour de l'interface
            badgeZone.textContent = parcours;
            badgeZone.className = "parcours-badge " + badge;
            badgeZone.style.backgroundColor = "";
            badgeZone.style.color = "white";

            console.log(`Affichage Parcours 6ème - VMA: ${vma}`);
        } else {
            badgeZone.textContent = "Test VMA non réalisé";
            badgeZone.className = "parcours-badge";
            badgeZone.style.backgroundColor = "#989Ca0";
            badgeZone.style.color = "white";
        }
    }
};
// Appeler la fonction au chargement de la page
document.addEventListener("DOMContentLoaded", afficherParcoursVMA);

/** Confirmation pour le retour à la séance. */
const retourSeance = () => {
    demanderConfirmation("Retourner à la séance ? Toutes les données de cette épreuve seront perdues.").then(confirmation => {
        if (confirmation) {
            window.location.href = "../pages/seance.html";
        }
    });
}

/* Ajout des écouteurs d'événements pour les boutons. */
startBtn.addEventListener("click", demarrer);
stopBtn.addEventListener("click", arreter);
resetBtn.addEventListener("click", reset);
enregistrerBtn.addEventListener("click", enregistrer);
supprimerBtn.addEventListener("click", supprimer);
btnValiderTir.addEventListener("click", validerTourEtTir);
terminerBtn.addEventListener("click", declencherFinEpreuve);
btnVoirBilan.addEventListener("click", validerRessentis);