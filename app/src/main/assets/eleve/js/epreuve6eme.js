let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let stopBtn = document.getElementById("stop");
let startBtn = document.getElementById("start");
const modal = document.getElementById("custom-confirm");
const confirmOk = document.getElementById("confirm-ok");
const confirmCancel = document.getElementById("confirm-cancel");
let enregistrerBtn = document.getElementById("enregistrer");
let supprimerBtn = document.getElementById("supprimer");
const btnValiderTir = document.getElementById("valider-tir");

let minutes = 20;
let secondes = 0;
let millisecondes = 0;
let timeout;
let estArrete = true;
let tourActuel = 1;

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
    if (minutes <= 0 && secondes <= 0 && millisecondes <= 0) {
        minutes = 0;
        secondes = 0;
        millisecondes = 0;
        estArrete = true;
        clearTimeout(timeout);
        chrono.textContent = "00:00:00";
        // Optionnel : déclencher le bilan final ici
        alert("Temps écoulé ! Fin de l'épreuve.");
        return;
    }

    // Affichage formaté
    let m = minutes < 10 ? "0" + minutes : minutes;
    let s = secondes < 10 ? "0" + secondes : secondes;
    let ms = Math.floor(millisecondes / 10);
    if (ms < 10) ms = "0" + ms;

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
        chrono.textContent = "20:00:00";
    }

    // Réinitialiser les tours
    const listeTours = document.getElementById("listeTours");
    if (listeTours) {
        listeTours.innerHTML = "";
        tourActuel = 1;
    }
};

/* Enregistre le temps et ouvre la saisie du tir */
const enregistrer = () => {
    // On vérifie que le chrono a tourné et n'est pas à 20:00:00 (compte à rebours)
    if(chrono.textContent !== "20:00:00" && !estArrete) {
        // On capture le temps actuel pour le tour
        const tempsCapture = chrono.textContent;

        // On prépare la modale de tir
        document.getElementById("titre-tir").textContent = `Score Tir - Tour ${tourActuel}`;
        document.getElementById("temps-tour-cache").value = tempsCapture; // On stocke le temps

        // On affiche la salve de tir (la modale)
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


// Variable pour stocker les scores de tirs pour le bilan final
let scoresTirs = [];

/**
 * Valide le tour en cours en enregistrant le score de tir et le temps, puis met à jour l'affichage de la liste des tours.
 * Cette fonction est appelée lorsque l'utilisateur clique sur le bouton de validation dans la modale de tir.
 */
const validerTourEtTir = () => {
    const score = parseInt(document.getElementById("score-temporaire").value);
    const tempsTour = document.getElementById("temps-tour-cache").value;
    const listeTours = document.getElementById("listeTours");

    // Création de l'affichage dans la liste
    const nouveauTour = document.createElement("div");
    nouveauTour.style.marginBottom = "10px";
    nouveauTour.innerHTML = `
        <strong>Tour ${tourActuel}</strong> : ${tempsTour} 
        <span class="badge-tir">${score}/5</span>
    `;

    listeTours.appendChild(nouveauTour);

    // Sauvegarde des données pour le calcul de la médaille
    scoresTirs.push(score);
    // Ici, tu pourrais aussi calculer l'écart pour la régularité

    // Fermeture de la modale et incrémentation
    document.getElementById("modal-tir").style.display = "none";
    tourActuel++;

    // Reset de la sélection visuelle pour le prochain tour
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

/* Ajout des écouteurs d'événements pour les boutons. */
startBtn.addEventListener("click", demarrer);
stopBtn.addEventListener("click", arreter);
resetBtn.addEventListener("click", reset);
enregistrerBtn.addEventListener("click", enregistrer);
supprimerBtn.addEventListener("click", supprimer);
btnValiderTir.addEventListener("click", validerTourEtTir);