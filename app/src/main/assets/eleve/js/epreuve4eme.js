/* === INITIALISATION DES ÉLÉMENTS === */
let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let stopBtn = document.getElementById("stop");
let startBtn = document.getElementById("start");
let btnPrincipal = document.getElementById("btn-principal");

const modal = document.getElementById("custom-confirm");
const modalNormal = document.getElementById("confirm-normal");
const confirmOk = document.getElementById("confirm-ok");
const confirmOkNormal = document.getElementById("confirm-ok-normal");
const confirmCancel = document.getElementById("confirm-cancel");

const btnValiderTir = document.getElementById("valider-tir");
const terminerBtn = document.getElementById("terminer");
const btnVoirBilan = document.getElementById("btnVoirBilan");

const consigne = document.getElementById("consigne-etape");

const liste = document.getElementById("listeTours");
const msgAttente = document.getElementById("msg-attente");

const titreTir = document.getElementById("titre-tir");
const modalTir = document.getElementById("modal-tir");

const scoreTemp = document.getElementById("score-temporaire");

const blockTerminerEpreuve = document.getElementById("blockTerminerEpreuve");

const btnAnnuler = document.getElementById("btn-annuler");

/* === VARIABLES GLOBALES === */
let minutes = 0; // On part de 0 pour le sprint 4ème
let secondes = 0;
let millisecondes = 0;
let tempsEcouleSession = 0; //Temps total accumulé en millisecondes
let dateDepart = null;      //Moment précis où on a cliqué sur Start
let timeout;
let estArrete = true;

// Stockage pour l'analyse finale
let autoEval = {intensite: "", durer: "", lucidite: ""};
let tempsDebutTir = { min: 0, sec: 0 };

// Suivi de la progression dans les étapes et les points de passage
let etapeActuelle = 0;
let pointsPassage = { A: 0, B: 0, C: 0, D: 0, E: 0 };
let tirsData = { serie1: 0, serie2: 0 };

/* === MOTEUR DU CHRONOMÈTRE (PROGRESSIF) === */
/* Cette fonction gère le déroulement du temps en montant (Sprint). */
const defilerTemps = () => {
    if (estArrete) return;

    // Calcul du temps réel écoulé depuis le dernier "Start"
    const maintenant = Date.now();
    const difference = maintenant - dateDepart;
    const totalMs = tempsEcouleSession + difference;

    // Conversion pour l'affichage
    let totalSecondes = Math.floor(totalMs / 1000);
    let msAffiche = Math.floor((totalMs % 1000) / 10);
    let sAffiche = totalSecondes % 60;
    let mAffiche = Math.floor(totalSecondes / 60);

    // Affichage formaté
    chrono.textContent =
        mAffiche.toString().padStart(2, '0') + ":" +
        sAffiche.toString().padStart(2, '0') + ":" +
        msAffiche.toString().padStart(2, '0');

    // On stocke les valeurs dans tes anciennes variables pour ne pas casser le reste de ton code
    minutes = mAffiche;
    secondes = sAffiche;
    millisecondes = totalMs % 1000;

    timeout = setTimeout(defilerTemps, 10);
};

/* Démarre le chronomètre si il est arrêté. */
const demarrer = () => {
    if (estArrete) {
        estArrete = false;
        dateDepart = Date.now(); //On fige l'heure de départ
        defilerTemps();
        if(startBtn) startBtn.style.display = "none";
        if(stopBtn) stopBtn.style.display = "block";
    }
};

/* Arrête le chronomètre si il est en cours. */
const arreter = async()=> {
    const confirmationAction = await demanderConfirmation("Mettre en pause l'épreuve ?");
    if (confirmationAction) {
        if (!estArrete) {
            estArrete = true;
            clearTimeout(timeout);

            //On sauvegarde le temps parcouru depuis le dernier "Start"
            tempsEcouleSession += (Date.now() - dateDepart);

            if(startBtn) startBtn.style.display = "block";
            if(stopBtn) stopBtn.style.display = "none";
        }
    }
};

/* Arrête le chronomètre si il est en cours. */
const arreterEpreuve = () => {
    if (!estArrete) {
        estArrete = true;
        clearTimeout(timeout);
        // On enregistre définitivement le temps écoulé jusqu'ici
        tempsEcouleSession += (Date.now() - dateDepart);
    }
};

/* Réinitialise le chronomètre après confirmation. */
/* Réinitialise l'épreuve après confirmation (Version 4ème corrigée) */
const reset = async() => {
    const confirmationAction = await demanderConfirmation("Réinitialiser l'épreuve ?");

    if (confirmationAction) {
        //Arrêt du moteur de rendu
        estArrete = true;
        if (timeout) clearTimeout(timeout);

        //RESET DU SYSTÈME DE TEMPS RÉEL (Crucial pour Date.now)
        tempsEcouleSession = 0;
        dateDepart = null;

        //Reset des variables de temps classiques
        minutes = 0;
        secondes = 0;
        millisecondes = 0;
        chrono.textContent = "00:00:00";

        //Réinitialisation des données de l'épreuve
        etapeActuelle = 0;
        pointsPassage = { A: 0, B: 0, C: 0, D: 0, E: 0 };
        tirsData = { serie1: 0, serie2: 0 };
        autoEval = {intensite: "", durer: "", lucidite: ""};

        //Remise à zéro de l'interface (Bouton et Consigne)
        if (btnPrincipal) {
            btnPrincipal.style.display = "block";
            btnPrincipal.textContent = "DÉMARRER";
            btnPrincipal.className = "button-green"; // Retour au vert
        }

        if (consigne) {
            consigne.textContent = "Prêt pour le départ ?";
        }

        if (btnAnnuler) {
            btnAnnuler.style.display = "none";
        }

        //Cacher les contrôles de secours et le bouton terminer
        const buttonTraining = document.getElementById("buttonTraining");
        if (buttonTraining) buttonTraining.style.display = "none";

        if (startBtn) startBtn.style.display = "none";
        if (stopBtn) stopBtn.style.display = "none";
        if (terminerBtn) terminerBtn.style.display = "none";

        //Nettoyage de la liste des tours et remise du message d'attente
        if (liste) {
            liste.innerHTML = "";
            const p = document.createElement("p");
            p.id = "msg-attente";
            p.textContent = "En attente de l'arrivée au premier pas de tir...";
            liste.appendChild(p);
            // On s'assure que la référence globale est mise à jour
            window.msgAttente = p;
        }

        console.log("Épreuve 4ème réinitialisée avec succès.");
    }
};

/**
 * Reset sans redemander confirmation (utilisé par la fonction annuler)
 */
const resetCompletSansDemander = () => {
    estArrete = true;
    clearTimeout(timeout);
    tempsEcouleSession = 0;
    dateDepart = null;
    minutes = secondes = millisecondes = 0;
    chrono.textContent = "00:00:00";

    // Réinitialisation des données de l'épreuve
    etapeActuelle = 0;
    pointsPassage = { A: 0, B: 0, C: 0, D: 0, E: 0 };
    tirsData = { serie1: 0, serie2: 0 };
    autoEval = {intensite: "", durer: "", lucidite: ""};

    // Remise à zéro de l'interface (Bouton et Consigne)
    btnPrincipal.style.display = "block";
    btnPrincipal.textContent = "DÉMARRER";
    btnPrincipal.className = "button-green"; // On s'assure qu'il redevient vert
    consigne.textContent = "Prêt pour le départ ?";
    btnAnnuler.style.display = "none";

    // Cacher les contrôles de secours au reset
    document.getElementById("buttonTraining").style.display = "none";
    startBtn.style.display = "none";
    stopBtn.style.display = "none";

    // Nettoyage de la liste des tours et remise du message d'attente
    liste.innerHTML = "";
    const p = document.createElement("p");
    p.id = "msg-attente";
    p.textContent = "En attente de l'arrivée au premier pas de tir...";
    liste.appendChild(p);

    // On redéfinit la variable globale msgAttente pour les prochains clics
    window.msgAttente = msgAttente
};

/* === GESTION DU TIR (VISUEL) === */
/* Colore le bouton de score sélectionné dans la modale. */
function setScoreTir(valeur) {
    document.getElementById("score-temporaire").value = valeur;
    const boutons = document.querySelectorAll('.btn-score');
    boutons.forEach(btn => {
        btn.classList.remove('selected');
        if(parseInt(btn.textContent) === valeur) {
            btn.classList.add('selected');
        }
    });
}

/* === SYSTÈME DE MODALES === */
/* Boîte de confirmation avec OK / Annuler. */
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

/* Boîte d'alerte simple avec OK. */
const confirmation = (message) => {
    document.getElementById("confirm-message-normal").textContent = message;
    modalNormal.style.display = "flex";
    setTimeout(() => { modalNormal.classList.add("show"); }, 10);

    return new Promise((resolve) => {
        confirmOkNormal.onclick = () => {
            modalNormal.classList.remove("show");
            setTimeout(() => { modalNormal.style.display = "none"; }, 300);
            resolve(true);
        };
    });
};

/* === AUTO-ÉVALUATION === */
/* Enregistre le choix de ressenti de l'élève. */
function selectAudit(element, categorie, valeur) {
    const parent = element.parentElement;
    parent.querySelectorAll('.btn-audit').forEach(btn => btn.classList.remove('selected'));
    element.classList.add('selected');
    autoEval[categorie] = valeur;
}

/**
 * Récupère les infos VMA du coureur actif et affiche son parcours coloré (Spécifique 4ème)
 */
const afficherParcoursVMA = () => {
    const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));
    // On force le niveau 4ème ici ou on le récupère
    const niveau = localStorage.getItem("niveau") || "4eme";

    const displayZone = document.getElementById("vma-result-display");
    const badgeZone = document.getElementById("badge-parcours");

    if (displayZone && badgeZone) {
        displayZone.style.display = "block";

        if (coureur && coureur.vma && coureur.vma > 0) {
            const vma = parseFloat(coureur.vma);
            let badge = "";
            let parcours = "";

            // --- LOGIQUE BARÈME 4ÈME (Complet) ---
            if (vma <= 10) {
                badge = "bg-jaune"; parcours = "Coupelles Jaunes (250m)";
            } else if (vma <= 11) {
                badge = "bg-vert"; parcours = "Plots Verts (275m)";
            } else if (vma <= 12) {
                badge = "bg-bleu"; parcours = "Coupelles Bleues (300m)";
            } else if (vma <= 13) {
                badge = "bg-bleu"; parcours = "Plots Bleus (325m)";
            } else if (vma <= 14) {
                badge = "bg-rouge"; parcours = "Coupelles Rouges (350m)";
            } else if (vma <= 15) {
                badge = "bg-rouge"; parcours = "Plots Rouges (375m)";
            } else {
                badge = "bg-noir"; parcours = "Grand Tour (400m)";
            }

            // Mise à jour de l'interface
            badgeZone.textContent = parcours;
            badgeZone.className = "parcours-badge " + badge;
            badgeZone.style.backgroundColor = "";
            badgeZone.style.color = "white";

            console.log(`Affichage Parcours 4ème - VMA: ${vma}`);
        } else {
            badgeZone.textContent = "Test VMA non réalisé";
            badgeZone.className = "parcours-badge";
            badgeZone.style.backgroundColor = "#989Ca0";
            badgeZone.style.color = "white";
        }
    }
};

/**
 * Gère la progression de l'épreuve en fonction de l'étape actuelle, en enregistrant les temps de passage et en affichant les consignes adaptées.
 * Chaque clic sur le bouton principal fait avancer l'épreuve à l'étape suivante, avec des actions spécifiques à chaque point de passage (A, B, C, D, E) et
 * l'ouverture des modales de tir aux étapes correspondantes. À la fin de l'épreuve, le chronomètre s'arrête et le bouton principal disparaît pour laisser place au bilan.
 */
const gestionnaireEtape = () => {
    const tempsActuel = chrono.textContent;
    const secondesTotales = (minutes * 60) + secondes;

    switch(etapeActuelle) {
        case 0: // TOP DÉPART
            demarrer();
            btnAnnuler.style.display = "block";
            buttonTraining.style.display = "flex";
            consigne.textContent = "Course : 2 tours";
            btnPrincipal.textContent = "ARRIVÉE TIR 1 (A)";
            etapeActuelle = 1;
            blockTerminerEpreuve.style.display = "block";
            // Reset de la sélection visuelle pour le prochain tour (les boutons de score)
            document.querySelectorAll('.btn-score').forEach(b => b.classList.remove('selected'));
            break;

        case 1: // POINT A : Arrivée Pas de Tir 1
            // Sécurité : on vérifie que le point A n'est pas déjà enregistré
            if (pointsPassage.A === 0) {
                pointsPassage.A = secondesTotales;
                ajouterAlaListe("A - Arrivée Tir 1", tempsActuel);
                ouvrirModaleTir(1); // On ouvre la saisie du score
            }
            break;

        case 2: // POINT B : Sortie Pas de Tir 1
            if (pointsPassage.B === 0) {
                pointsPassage.B = secondesTotales;
                ajouterAlaListe("B - Sortie Tir 1", tempsActuel);
                consigne.textContent = "Course : 2 tours";
                btnPrincipal.textContent = "ARRIVÉE TIR 2 (C)";
                etapeActuelle = 3;
                // Reset de la sélection visuelle pour le prochain tour (les boutons de score)
                document.querySelectorAll('.btn-score').forEach(b => b.classList.remove('selected'));
            }
            break;

        case 3: // POINT C : Arrivée Pas de Tir 2
            // Sécurité : on vérifie que le point C n'est pas déjà enregistré
            if (pointsPassage.C === 0) {
                pointsPassage.C = secondesTotales;
                ajouterAlaListe("C - Arrivée Tir 2", tempsActuel);
                ouvrirModaleTir(2);
            }
            break;

        case 4: // POINT D : Sortie Pas de Tir 2
            if (pointsPassage.D === 0) {
                pointsPassage.D = secondesTotales;
                ajouterAlaListe("D - Sortie Tir 2", tempsActuel);
                consigne.textContent = "Sprint Final : 2 tours !";
                btnPrincipal.textContent = "LIGNE D'ARRIVÉE (E)";
                etapeActuelle = 5;
            }
            break;

        case 5: // POINT E : Arrivée finale
            // Sécurité : évite d'enregistrer plusieurs fois la fin de course
            if (pointsPassage.E === 0) {
                pointsPassage.E = secondesTotales;
                ajouterAlaListe("E - FIN DE COURSE", tempsActuel);
                arreterEpreuve();
                btnPrincipal.style.display = "none";
                consigne.textContent = "Épreuve terminée !";
                etapeActuelle = 6; // Marqueur de fin
                terminerBtn.style.display = "block";
            }
            break;
    }
};

/**
 * Affiche visuellement le point figé dans la liste
 */
function ajouterAlaListe(label, temps) {
    // On enlève de la liste le msg depart si elle ne contient pas encore de "tour-item"
    if (msgAttente) {
        msgAttente.remove();
    }

    // Sécurité supplémentaire : si la liste contient du texte brut, on vide
    if (liste.querySelectorAll('.tour-item').length === 0) {
        liste.innerHTML = "";
    }

    const item = document.createElement("div");
    item.className = "tour-item";
    item.innerHTML = `<strong>${label} :</strong> <span class="badge-tir">${temps}</span>`;
    item.style.marginBottom = "10px";
    liste.appendChild(item);
}

/**
 * Ouvre la modale et prépare la saisie pour le tir
 */
function ouvrirModaleTir(numSerie) {
    titreTir.textContent = `Tir Série n°${numSerie}`;
    modalTir.style.display = "flex";
    modalTir.classList.add("show");
}

/**
 * Valide le tir et annonce les pénalités
 */
const validerTir4eme = () => {
    // Vérification : un score a-t-il été sélectionné ?
    const estSelectionne = document.querySelector('.btn-score.selected');

    if (!estSelectionne) {
        confirmation("Tu dois sélectionner ton nombre de cibles avant de valider !");
        return; // On arrête la fonction ici
    }

    // Si ok, on récupère le score
    const score = parseInt(scoreTemp.value);
    const fautes = 5 - score;

    if (etapeActuelle === 1) tirsData.serie1 = score;
    else tirsData.serie2 = score;

    // Fermeture et suite du processus
    modalTir.style.display = "none";
    modalTir.classList.remove("show");

    if (fautes > 0) {
        confirmation(`Tu as fait ${fautes} faute(s). Effectue tes ${fautes} tour(s) de pénalité avant de cliquer sur Sortie.`);
    } else {
        confirmation("ZÉRO FAUTE ! Clique directement sur Sortie.");
    }

    // Mise à jour de l'étape
    if (etapeActuelle === 1) {
        btnPrincipal.textContent = "SORTIE TIR 1 (B)";
        consigne.textContent = "Pénalités en cours...";
        etapeActuelle = 2;
    } else {
        btnPrincipal.textContent = "SORTIE TIR 2 (D)";
        consigne.textContent = "Pénalités en cours...";
        etapeActuelle = 4;
    }
};

/* Valide les ressentis avant d'afficher le bilan spécifique 4ème. */
const validerRessentis = async() => {
    if (!autoEval.intensite || !autoEval.durer || !autoEval.lucidite) {
        confirmation("Veuillez répondre aux 3 questions de ressenti.");
        return;
    }
    const modalRessenti = document.getElementById("modal-ressenti");
    modalRessenti.style.display = "none";
    modalRessenti.classList.remove("show");

    // Lancement du calcul final 4ème
    terminerEpreuve4eme();
};

/**
 * Calcule le bilan final avec la distance personnalisée récupérée du parcours
 */
const terminerEpreuve4eme = () => {
    const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));

    // Récupération des données avec valeurs de secours pour éviter le "NaN"
    const vmaRef = (coureur && coureur.vma) ? parseFloat(coureur.vma) : 10;
    const genreEleve = coureur ? coureur.genre : "M"; // "M" par défaut
    const distanceTour = (coureur && coureur.vma_distance) ? parseInt(coureur.vma_distance) : 250;

    // --- CALCUL INTENSITÉ (% VMA) ---
    const tempsTotalSec = pointsPassage.E; // Temps final figé à l'arrivée

    // Calcul de la distance : 6 tours de piste + pénalités (30m par cible ratée)
    const fautesTotales = (10 - (tirsData.serie1 + tirsData.serie2));
    const distanceTotaleM = (distanceTour * 6) + (fautesTotales * 30);

    // Calcul vitesse en km/h : (Distance / Temps) * 3.6
    const vitesseRealiseeKmh = (distanceTotaleM / tempsTotalSec) * 3.6;
    const pourcentageVMA = (vitesseRealiseeKmh / vmaRef) * 100;

    // Barème Intensité (sur 4 pts)
    let noteIntensite = 0;
    if (pourcentageVMA > 110) noteIntensite = 4;
    else if (pourcentageVMA >= 106) noteIntensite = 3.5;
    else if (pourcentageVMA >= 101) noteIntensite = 3;
    else if (pourcentageVMA >= 96)  noteIntensite = 2.5;
    else if (pourcentageVMA >= 91)  noteIntensite = 2;
    else if (pourcentageVMA >= 86)  noteIntensite = 1.5;
    else if (pourcentageVMA >= 81)  noteIntensite = 1;
    else if (pourcentageVMA >= 76)  noteIntensite = 0.5;
    else noteIntensite = 0;

    // EFFICIENCE TIR (Le nouveau barème croisé)
    // Temps cumulé passé sur le pas de tir (A->B + C->D)
    const tempsTirTotal = (pointsPassage.B - pointsPassage.A) + (pointsPassage.D - pointsPassage.C);
    const scoreTirTotal = tirsData.serie1 + tirsData.serie2;
    const noteEfficience = calculerNoteEfficience(tempsTirTotal, scoreTirTotal);

    // --- POINTS VMA (sur 2 pts) ---
    const noteVmaPoints = calculerNoteVMA(vmaRef, genreEleve);

    afficherResultats4eme(pourcentageVMA.toFixed(1), noteIntensite, vitesseRealiseeKmh, scoreTirTotal, tempsTirTotal, noteEfficience, vmaRef, noteVmaPoints);};

/**
 * Calcule la note d'efficience au tir par croisement (Moyenne Temps/Réussite)
 * @param {number} secondes - Temps total passé au tir (A->B + C->D)
 * @param {number} reussite - Nombre total de cibles (0 à 10)
 * @returns {number} Note sur 6
 */
const calculerNoteEfficience = (secondes, reussite) => {
    // --- Note selon le Temps de tir ---
    let noteTemps = 0;
    if (secondes <= 80) noteTemps = 6;      // 1'20 et -
    else if (secondes <= 85) noteTemps = 5.5; // 1'25
    else if (secondes <= 90) noteTemps = 5;   // 1'30
    else if (secondes <= 95) noteTemps = 4.5; // 1'35
    else if (secondes <= 100) noteTemps = 4;  // 1'40
    else if (secondes <= 105) noteTemps = 3.5; // 1'45
    else if (secondes <= 110) noteTemps = 3;   // 1'50
    else if (secondes <= 115) noteTemps = 2.5; // 1'55
    else if (secondes <= 120) noteTemps = 2;   // 2'
    else if (secondes <= 125) noteTemps = 1.5; // 2'05
    else if (secondes <= 130) noteTemps = 1;   // 2'10
    else noteTemps = 0.5;                      // 2'15 et +

    // --- Note selon la Réussite au tir ---
    let noteReussite = 0;
    if (reussite >= 8) noteReussite = 6;
    else if (reussite === 7) noteReussite = 5.5;
    else if (reussite === 6) noteReussite = 5;
    else if (reussite === 5) noteReussite = 4.5;
    else if (reussite === 4) noteReussite = 3.5; // Le tableau saute le 4
    else if (reussite === 3) noteReussite = 2.5; // Le tableau saute le 3
    else if (reussite === 2) noteReussite = 1.5;
    else if (reussite === 1) noteReussite = 1;
    else noteReussite = 0.5;

    // --- LE CROISEMENT (La moyenne des deux notes) ---
    let moyenne = (noteTemps + noteReussite) / 2;

    // --- ARRONDI AU 0.5 LE PLUS PROCHE ---
    // On multiplie par 2, on arrondit à l'entier, puis on divise par 2
    return Math.round(moyenne * 2) / 2;
};

/**
 * Calcule la note de VMA sur 2 points selon le genre
 * @param {number} vma - La VMA de l'élève
 * @param {string} genre - "M" (Garçon) ou "F" (Fille)
 * @returns {number} Note sur 2
 */
const calculerNoteVMA = (vma, genre) => {
    let note = 0;

    if (genre === "M") {
        // BARÈME GARÇONS
        if (vma >= 14.5) note = 2;
        else if (vma >= 14) note = 1.75;
        else if (vma >= 13.5) note = 1.5;
        else if (vma >= 13) note = 1.25;
        else if (vma >= 12.5) note = 1;
        else if (vma >= 12) note = 0.75;
        else if (vma >= 11.5) note = 0.5;
        else note = 0.25;
    } else {
        // BARÈME FILLES
        if (vma >= 12.5) note = 2;
        else if (vma >= 12) note = 1.75;
        else if (vma >= 11.5) note = 1.5;
        else if (vma >= 11) note = 1.25;
        else if (vma >= 10.5) note = 1;
        else if (vma >= 10) note = 0.75;
        else if (vma >= 9.5) note = 0.5;
        else note = 0.25;
    }

    return note;
};

/**
 * Affiche le bilan final spécifique au 4ème
 * @param {string} pourcentageVMA - % de VMA réalisé (ex: "102.5")
 * @param {number} noteIntensite - Note sur 4
 * @param {number} vitesseRealiseeKmh - Vitesse réelle en km/h
 * @param {number} reussiteTir - Nombre de cibles (0 à 10)
 * @param {number} tempsTirTotal - Temps cumulé des 2 salves en secondes
 * @param {number} noteEfficience - Note croisée sur 6
 * @param {number} vmaRef - VMA de référence
 * @param {number} noteVma - Note VMA sur 2
 */
const afficherResultats4eme = (pourcentageVMA, noteIntensite, vitesseRealiseeKmh, reussiteTir, tempsTirTotal, noteEfficience, vmaRef, noteVma) => {
    // Fonction utilitaire pour mettre à jour le texte
    const majTexte = (id, texte) => {
        const el = document.getElementById(id);
        if (el) el.innerHTML = texte;
    };

    // --- CALCUL ET ARRONDI DU TOTAL ---
    // On additionne les valeurs numériques
    const sommeBrute = parseFloat(noteIntensite) + parseFloat(noteEfficience) + parseFloat(noteVma);
    const noteFinale = parseFloat(sommeBrute.toFixed(2));

    // Formatage du temps de tir (ex: 95s -> 1'35)
    const minTir = Math.floor(tempsTirTotal / 60);
    const secTir = tempsTirTotal % 60;
    const tempsTirFormate = `${minTir}'${secTir.toString().padStart(2, '0')}`;

    // 1. Vitesse (Intensité) : % de VMA réalisé
    majTexte("res-vitesse", `<strong>Intensité : </strong>${pourcentageVMA}% de ta VMA`);
    majTexte("detail-vitesse", `<strong>Ma vitesse : </strong>${vitesseRealiseeKmh.toFixed(1)} km/h`);
    majTexte("note-vitesse", `<strong>Note Intensité : </strong> <span>${noteIntensite} / 4</span>`);

    // 2. Efficience Tir : Le croisement Temps / Précision
    majTexte("res-tir-4eme", `<strong>Précision : </strong>${reussiteTir} cibles touchées`);
    majTexte("detail-temps-tir", `<strong>Temps des 2 salves : </strong>${tempsTirFormate}`);
    majTexte("note-tir-4eme", `<strong>Note Efficience : </strong> <span>${noteEfficience} / 6</span>`);

    // 3. VMA de référence : Points bonus basés sur le test initial
    majTexte("res-vma", `<strong>VMA de référence : </strong>${vmaRef} km/h`);
    majTexte("note-vma", `<strong>Note VMA : </strong> <span>${noteVma} / 2</span>`);

    // Note Finale sur 12
    const elNoteTotale = document.getElementById("note-totale");
    if (elNoteTotale) elNoteTotale.textContent = noteFinale;

    // --- Gestion des Médailles & Couleurs ---
    const couleurs = {
        "diamant": "#1456DB",
        "or": "#ffd700",
        "argent": "#c0c0c0",
        "bronze": "#cd7f32"
    };

    // Médaille Intensité (basée sur ton barème : Or dès 101%, Diamant +110%)
    const mDisplayIntensite = document.getElementById("medaille-display-intensite");
    if (mDisplayIntensite) {
        let medaille = "BRONZE";
        if (pourcentageVMA > 110) medaille = "DIAMANT";
        else if (pourcentageVMA >= 101) medaille = "OR";
        else if (pourcentageVMA >= 91) medaille = "ARGENT";

        mDisplayIntensite.textContent = "Médaille : " + medaille;
        mDisplayIntensite.style.backgroundColor = couleurs[medaille.toLowerCase()] || "#ccc";
        mDisplayIntensite.style.color = medaille === "OR" ? "black" : "white";
        mDisplayIntensite.style.padding = "20px";
        mDisplayIntensite.style.borderRadius = "12px";
        mDisplayIntensite.style.fontSize = "1.4rem";
    }

    // Médaille VMA (basée sur la note intrinsèque sur 2)
    const mDisplayVma = document.getElementById("medaille-display-vma");
    if (mDisplayVma) {
        let medaille = "BRONZE";
        if (noteVma === 2) medaille = "OR";
        else if (noteVma >= 1.5) medaille = "ARGENT";

        mDisplayVma.textContent = "Médaille : " + medaille;
        mDisplayVma.style.backgroundColor = couleurs[medaille.toLowerCase()] || "#ccc";
        mDisplayVma.style.color = medaille === "OR" ? "black" : "white";
        mDisplayVma.style.padding = "20px";
        mDisplayVma.style.borderRadius = "12px";
        mDisplayVma.style.fontSize = "1.4rem";
    }

    // Affichage de la modale
    const modalBilan = document.getElementById("modal-bilan");
    if (modalBilan) {
        modalBilan.style.display = "flex";
        setTimeout(() => modalBilan.classList.add("show"), 10);
    }
};

/**
 * Permet d'annuler la dernière étape.
 * Si on est à la première étape, cela déclenche un reset complet.
 */
const annulerEtape = async () => {
    if (etapeActuelle === 0) return; // Sécurité

    // CAS SPÉCIAL : Première étape -> On propose un Reset complet
    if (etapeActuelle === 1) {
        const confirmReset = await demanderConfirmation("Annuler le départ et réinitialiser l'épreuve ?");
        if (confirmReset) {
            resetCompletSansDemander(); // On appelle une version directe du reset
        }
        return;
    }

    // CAS CLASSIQUE : On recule d'une étape
    const confirmationAction = await demanderConfirmation("Annuler la dernière étape ?");
    if (confirmationAction) {
        // Reset des données selon l'étape qu'on annule
        if (etapeActuelle === 6) {
            pointsPassage.E = 0;
            terminerBtn.style.display = "none";
        } else if (etapeActuelle === 5) {
            pointsPassage.D = 0;
        } else if (etapeActuelle === 3) {
            pointsPassage.B = 0;
        }

        etapeActuelle--;

        // Suppression visuelle du dernier élément
        if (liste.lastElementChild && liste.lastElementChild.className === "tour-item") {
            liste.lastElementChild.remove();
        }

        // Si on a tout supprimé, on doit remettre le message proprement
        if (liste.querySelectorAll('.tour-item').length === 0) {
            liste.innerHTML = '<p id="msg-attente">En attente de l\'arrivée au premier pas de tir...</p>';
        }

        // Remise en marche si necessaire
        btnPrincipal.style.display = "block";
        if (estArrete && etapeActuelle > 0) {
            estArrete = false;
            defilerTemps();
            startBtn.style.display = "none";
            stopBtn.style.display = "block";
        }

        actualiserInterfaceEtape();
    }
};

/** Met à jour le texte du bouton principal et la consigne en fonction de l'étape actuelle. */
const actualiserInterfaceEtape = () => {
    switch(etapeActuelle) {
        case 0:
            btnPrincipal.textContent = "DÉMARRER";
            consigne.textContent = "Prêt pour le départ ?";
            break;
        case 1:
            btnPrincipal.textContent = "ARRIVÉE TIR 1 (A)";
            consigne.textContent = "Course : 2 tours";
            break;
        case 2:
            btnPrincipal.textContent = "SORTIE TIR 1 (B)";
            consigne.textContent = "Pénalités en cours...";
            break;
        case 3:
            btnPrincipal.textContent = "ARRIVÉE TIR 2 (C)";
            consigne.textContent = "Course : 2 tours";
            break;
        case 4:
            btnPrincipal.textContent = "SORTIE TIR 2 (D)";
            consigne.textContent = "Pénalités en cours...";
            break;
        case 5:
            btnPrincipal.textContent = "LIGNE D'ARRIVÉE (E)";
            consigne.textContent = "Sprint Final : 2 tours !";
            break;
    }
};

/** Confirmation pour le retour à la séance. */
const retourSeance = () => {
    demanderConfirmation("Retourner à la séance ? Toutes les données de cette épreuve seront perdues.").then(confirmation => {
        if (confirmation) {
            window.location.href = "../pages/seance.html";
        }
    });
}

/* === GESTION DE LA FIN D'ÉPREUVE === */
terminerBtn.addEventListener("click", () => {
    demanderConfirmation("Terminer l'épreuve ?").then(confirmation => {
        if (confirmation) {
            // On ouvre la modale de ressenti (Audit)
            const modalRessenti = document.getElementById("modal-ressenti");
            if (modalRessenti) {
                modalRessenti.style.display = "flex";
                setTimeout(() => modalRessenti.classList.add("show"), 10);
            }
        }
    });
});

/* === ÉCOUTEURS D'ÉVÉNEMENTS === */
document.addEventListener("DOMContentLoaded", afficherParcoursVMA);
startBtn.addEventListener("click", demarrer);
stopBtn.addEventListener("click", arreter);
resetBtn.addEventListener("click", reset);
btnVoirBilan.addEventListener("click", validerRessentis);
btnValiderTir.addEventListener("click", validerTir4eme);
btnAnnuler.addEventListener("click", annulerEtape);