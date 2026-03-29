// On récupère le niveau stocké (6eme ou 4eme)
const niveau = localStorage.getItem("niveau") || "6eme";
const btnEval = document.getElementById("btn-eval");
const btnTir = document.getElementById("btn-tir")

// Redirection pour l'Épreuve d'Évaluation
btnEval.onclick = () => {
    if (niveau === "6eme") {
        window.location.href = "epreuve6eme.html";

    } else {
        window.location.href = "epreuve4eme.html";
    }
};

// Redirection pour l'Entraînement au Tir
if (btnTir) {
    btnTir.onclick = () => {
        if (niveau === "6eme") {
            // Renvoie vers le tir 6ème
            window.location.href = "tir6eme.html";
        } else {
            // Renvoie vers le tir 4ème
            window.location.href = "tir4eme.html";
        }
    };
}

// Récupération des données du binôme
let e1 = JSON.parse(localStorage.getItem("eleve1"));
let e2 = JSON.parse(localStorage.getItem("eleve2"));

// On récupère l'index (0 pour élève1, 1 pour élève2)
let indexActif = localStorage.getItem("active_index");
if (indexActif === null) {
    indexActif = "0"; // Par défaut le premier élève choisi court
}
indexActif = parseInt(indexActif);

/**
 * Met à jour l'interface pour afficher les informations du coureur actif
 * - Affiche le nom complet et le genre du coureur actif
 * - Sauvegarde l'objet du coureur actif dans le localStorage pour les pages suivantes
 * - Met à jour le badge parcours en fonction de la VMA du coureur actif
 */
function actualiserInterface() {
    let nomAffiche = document.getElementById("current-name");
    let genreAffiche = document.getElementById("current-gender");
    let coureur;

    if (indexActif === 0) {
        coureur = e1;
    } else {
        coureur = e2;
    }

    nomAffiche.textContent = coureur.nomComplet;
    genreAffiche.textContent = "(" + coureur.genre + ")";

    // On sauvegarde l'objet du coureur actuel pour les pages de tir/course
    localStorage.setItem("coureur_actif_objet", JSON.stringify(coureur));

    // Mise à jour immédiate du badge parcours
    afficherParcoursVMA();
}

// 3. Fonction d'inversion (liée au bouton du Hub)
window.inverserRoles = function() {
    if (indexActif === 0) {
        indexActif = 1;
    } else {
        indexActif = 0;
    }

    localStorage.setItem("active_index", indexActif);
    actualiserInterface();
};

// Initialisation au chargement de la page
actualiserInterface();