let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let stopBtn = document.getElementById("stop");
let startBtn = document.getElementById("start");
let enregistrerBtn = document.getElementById("enregistrer");
let supprimerBtn = document.getElementById("supprimer");
let enregistrerSessionBtn = document.getElementById("btn-envoyer");
const modal = document.getElementById("custom-confirm");
const confirmOk = document.getElementById("confirm-ok");
const confirmCancel = document.getElementById("confirm-cancel");
const btnRetourSeance = document.getElementById("retourSeance");

// Variables globales pour la gestion du temps et des tours
let timeout;
let estArrete = true;
let dateDepart = null;
let tempsEcoule = 0;
let tourActuel = 1;

/**
 * Fait défiler le temps du chronomètre en calculant le temps total écoulé depuis le départ, et met à jour l'affichage du chronomètre.
 * Tant que le chronomètre n'est pas arrêté, elle se rappelle elle-même toutes les 10ms pour continuer à mettre à jour le temps affiché.
 */
const defilerTemps = () => {
    if (estArrete) return;
    const totalMs  = tempsEcoule + (Date.now() - dateDepart);
    const totalSec = Math.floor(totalMs / 1000);
    const m  = Math.floor(totalSec / 60);
    const s  = totalSec % 60;
    const ms = Math.floor((totalMs % 1000) / 10);
    chrono.textContent =
        String(m).padStart(2,'0') + ':' +
        String(s).padStart(2,'0') + ':' +
        String(ms).padStart(2,'0');
    timeout = setTimeout(defilerTemps, 10);
};

/** Démarre le chronomètre si il est arrêté. */
const demarrer = () => {
    if (estArrete) {
        estArrete = false;
        dateDepart = Date.now();
        defilerTemps();
    }
};

/** Arrête le chronomètre si il est en cours. */
const arreter = () => {
    if (!estArrete) {
        estArrete = true;
        clearTimeout(timeout);
        tempsEcoule += Date.now() - dateDepart;
    }
};

/**
 * Réinitialise le chronomètre et les tours après confirmation de l'utilisateur.
 * @returns {Promise<void>} une promesse qui se résout lorsque la réinitialisation est terminée, avec gestion de la confirmation de l'utilisateur.
 */
const reset = async() => {
    const confirmation = await demanderConfirmation("Réinitialiser le chronomètre ?");

    if (confirmation) {
        estArrete = true;
        clearTimeout(timeout);
        tempsEcoule = 0;
        dateDepart = null;
        chrono.textContent = "00:00:00";
    }

    // Réinitialiser les tours
    const listeTours = document.getElementById("listeTours");
    if (listeTours) {
        listeTours.innerHTML = "";
        tourActuel = 1;
    }
};

/**
 * Affiche une fenêtre de confirmation personnalisée avec le message fourni, et retourne une promesse qui se résout avec true si l'utilisateur confirme, ou false s'il annule.
 * @param message Le message à afficher dans la boîte de confirmation
 * @returns {Promise<unknown>} Une promesse qui se résout en true si l'utilisateur confirme, ou false s'il annule
 */
const demanderConfirmation = (message) => {
    document.getElementById("confirm-message").textContent = message;

    modal.style.display = "flex";
    //Petit timeout pour laisser le navigateur appliquer le display:flex
    //avant de lancer l'animation CSS
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

/** Enregistre le temps actuel dans la liste des tours si le chronomètre n'est pas à zéro. */
const enregistrer = () => {
    if(chrono.textContent != "00:00:00"){
        const listeTours = document.getElementById("listeTours"); // conteneur de tous les tours
        const nouveauTour = document.createElement("span");
        nouveauTour.innerHTML = `<strong>Tour ${tourActuel}:</strong> ${chrono.textContent}<br><br> `;
        listeTours.appendChild(nouveauTour);

        tourActuel++;
    }
};

/** Supprime le dernier tour enregistré de la liste des tours si le chronomètre n'est pas à zéro. */
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
 * Logique harmonisée pour les classes de 4ème et 6ème
 */
const afficherParcoursVMA = () => {
    const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));
    const displayZone = document.getElementById("vma-result-display");
    const badgeZone = document.getElementById("badge-parcours");

    if (displayZone && badgeZone) {
        // On rend le bloc visible dans tous les cas pour guider l'élève
        displayZone.style.display = "block";

        if (coureur && coureur.vma && coureur.vma > 0) {
            const vma = parseFloat(coureur.vma);
            let badge = "";
            let parcours = "";

            // --- LOGIQUE BARÈME HARMONISÉE ---
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
            // On remplace les classes précédentes par la nouvelle classe de couleur
            badgeZone.className = "parcours-badge " + badge;
            badgeZone.style.color = "white";

            console.log(`Parcours harmonisé affiché - VMA: ${vma}`);
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

/**
 * Récupère les données du coureur actif et les temps au tour, puis envoie le tout au serveur via une requête POST.
 * @returns {Promise<void>} une promesse qui se résout lorsque l'envoi est terminé, avec gestion des erreurs et des réponses du serveur.
 */
async function envoyerCourseAuServeur() {
    const confirmationAction = await demanderConfirmation("Mettre fin à la session et envoyer les données ?");

    if (confirmationAction) {
        // Récupération des données du coureur actif
        const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));

        // Vérification de la présence des données nécessaires
        if (!coureur?.nomComplet) {
            alert("Identité élève introuvable.");
            return;
        }

        // Extraction du prénom et du nom à partir du nom complet
        const parts = coureur.nomComplet.trim().split(" ");
        const prenom = parts[0] || "";
        const nom = parts.slice(1).join(" ") || "";

        // Validation de base pour s'assurer que le prénom et le nom sont présents
        if (!prenom || !nom) {
            alert("Identité invalide.");
            return;
        }

        // Récupération des temps au tour depuis la liste affichée
        const listeTours = document.querySelectorAll("#listeTours span");

        // Validation pour s'assurer qu'il y a au moins un tour enregistré avant d'envoyer les données
        if (listeTours.length === 0) {
            alert("Aucun tour enregistré.");
            return;
        }

        // Transformation des temps au format "mm:ss:ms" en millisecondes pour l'envoi au serveur
        const tempsAuTour = Array.from(listeTours).map(span => {
            const texte = span.textContent.split(": ")[1]; //"mm:ss:ms"
            if (!texte) return 0;
            const [m, s, cs] = texte.split(":").map(Number);
            // votre affichage est en centièmes (00-99)
            // conversion correcte vers millisecondes
            return (m * 60000) + (s * 1000) + (cs * 10);
        });

        // Construction de l'objet de requête à envoyer au serveur
        const request = {
            prenom,
            nom,
            distance: coureur.vma_distance ? parseFloat(coureur.vma_distance) : 0,
            nbTours: tempsAuTour.length,
            nbTirsReussi: [],
            tempsAuPasDeTir: [],
            tempsAuTour
        };

        // Envoi de la requête au serveur avec gestion des erreurs et des réponses
        try {
            // Envoi de la requête POST à l'endpoint "/api/biathlon"
            const response = await fetch("/api/biathlon", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(request)
            });

            // Vérification de la réponse du serveur
            if (!response.ok) {
                const err = await response.text();
                console.error("Erreur serveur :", err);
                alert("Erreur lors de l'envoi.");
                return;
            }

            afficherToast("Résultat envoyé au professeur");

            // Redirection vers le hub de la séance
            window.location.href = "seance.html";

        } catch (e) {
            console.error("Erreur fetch course :", e);
            alert("Erreur réseau.");
        }
    }
}

/**
 * Affiche une boîte de confirmation avant de retourner à la page de session, pour éviter les pertes de données accidentelles
 * @returns {Promise<void>} Une promesse qui se résout lorsque l'utilisateur a pris une décision, avec redirection vers la page de session si il confirme, ou maintien sur la page actuelle s'il annule
 */
const retourSeance = async() => {
    if (await demanderConfirmation("Abandonner la session en cours et retourner sur Session Biathlon ?")) {
        window.location.href = "../pages/seance.html";
    }
};

/**
 * Affiche un toast de notification en bas de l'écran
 * @param {string} message - Le message à afficher dans le toast
 */
function afficherToast(message) {
    const toast = document.createElement("div");
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        bottom: 32px;
        left: 50%;
        transform: translateX(-50%);
        background: rgba(0,0,0,0.82);
        color: #fff;
        padding: 12px 24px;
        border-radius: 24px;
        font-size: 15px;
        font-weight: 500;
        z-index: 9999;
        pointer-events: none;
        opacity: 1;
        transition: opacity 0.5s ease;
    `;
    document.body.appendChild(toast);
    setTimeout(() => { toast.style.opacity = "0"; }, 2000);
    setTimeout(() => { toast.remove(); }, 2600);
}

/* Ajout des écouteurs d'événements pour les boutons. */
startBtn.addEventListener("click", demarrer);
stopBtn.addEventListener("click", arreter);
resetBtn.addEventListener("click", reset);
enregistrerBtn.addEventListener("click", enregistrer);
supprimerBtn.addEventListener("click", supprimer);
if (btnRetourSeance) btnRetourSeance.addEventListener("click", retourSeance);

/* Gestion de l'envoi de la session au serveur avec désactivation du bouton pendant l'opération pour éviter les doubles clics. */
enregistrerSessionBtn.addEventListener("click", async () => {
    // 1. On désactive le bouton pour éviter les doubles clics
    enregistrerSessionBtn.disabled = true;
    enregistrerSessionBtn.innerText = "Envoi en cours...";

    // 2. On attend que la fonction gère tout (erreurs ET succès)
    await envoyerCourseAuServeur();

    // 3. On réactive le bouton au cas où il y a eu une erreur et qu'il reste sur la page
    enregistrerSessionBtn.disabled = false;
    enregistrerSessionBtn.innerText = "Envoyer la session";
});