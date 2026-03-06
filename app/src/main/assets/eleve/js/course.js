let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let stopBtn = document.getElementById("stop");
let startBtn = document.getElementById("start");
let enregistrerBtn = document.getElementById("enregistrer");
let supprimerBtn = document.getElementById("supprimer");
const modal = document.getElementById("custom-confirm");
const confirmOk = document.getElementById("confirm-ok");
const confirmCancel = document.getElementById("confirm-cancel");

let minutes = 0;
let secondes = 0;
let millisecondes = 0;
let timeout;
let estArrete = true;
let tourActuel = 1;

/* Cette fonction gère le déroulement du temps.
Elle s'appelle elle-même toutes les 10ms tant que le chronomètre n'est pas arrêté. */
const defilerTemps = () => {
    if (estArrete) return;

    millisecondes += 10;

    if (millisecondes === 1000) {
        millisecondes = 0;
        secondes++;
    }

    if (secondes === 60) {
        secondes = 0;
        minutes++;
    }

    // Initialiser les variables pour l'affichage
    let m = minutes;
    let s = secondes;
    let ms = Math.floor(millisecondes / 10);

    // Ajouter un 0 si inférieur à 10
    if(m < 10) {
        m = "0" + m;
    }

    if(s < 10) {
        s = "0" + s;
    }

    if(ms < 10) {
        ms = "0" + ms;
    }

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
        minutes = secondes = millisecondes = 0;
        chrono.textContent = "00:00:00";
    }

    // Réinitialiser les tours
    const listeTours = document.getElementById("listeTours");
    if (listeTours) {
        listeTours.innerHTML = "";
        tourActuel = 1;
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

/* Enregistre le temps actuel dans la liste des tours si le chronomètre n'est pas à zéro. */
const enregistrer = () => {
    if(chrono.textContent != "00:00:00"){
        const listeTours = document.getElementById("listeTours"); // conteneur de tous les tours
        const nouveauTour = document.createElement("span");
        nouveauTour.innerHTML = `Tour ${tourActuel}: ${chrono.textContent}<br><br> `;
        listeTours.appendChild(nouveauTour);
        tourActuel++;
    }
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

/**
 * Récupère les infos VMA du coureur actif et affiche son parcours coloré
 */
const afficherParcoursVMA = () => {
    const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));
    const displayZone = document.getElementById("vma-result-display");
    const badgeZone = document.getElementById("badge-parcours");

    if (displayZone && badgeZone) {
        // On rend le bloc visible dans tous les cas pour guider l'élève
        displayZone.style.display = "block";

        if (coureur && coureur.vma_badge && coureur.vma_parcours) {
            // Données présentes -> Affichage du parcours coloré
            badgeZone.textContent = coureur.vma_parcours;
            badgeZone.className = "parcours-badge " + coureur.vma_badge;
            badgeZone.style.backgroundColor = ""; // Reset du style inline
        } else {
            // Pas de données -> Message d'alerte gris neutre
            badgeZone.textContent = "Test VMA non réalisé";
            badgeZone.className = "parcours-badge";
            badgeZone.style.backgroundColor = "#989Ca0";
            badgeZone.style.color = "white";
        }
    }
};
// Appeler la fonction au chargement de la page
document.addEventListener("DOMContentLoaded", afficherParcoursVMA);

/* Ajout des écouteurs d'événements pour les boutons. */
startBtn.addEventListener("click", demarrer);
stopBtn.addEventListener("click", arreter);
resetBtn.addEventListener("click", reset);
enregistrerBtn.addEventListener("click", enregistrer);
supprimerBtn.addEventListener("click", supprimer);
