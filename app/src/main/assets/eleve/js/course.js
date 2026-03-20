let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let stopBtn = document.getElementById("stop");
let startBtn = document.getElementById("start");
let enregistrerBtn = document.getElementById("enregistrer");
let supprimerBtn = document.getElementById("supprimer");
let enregistrerSessionBtn = document.getElementById("enregistrer-session");
const modal = document.getElementById("custom-confirm");
const confirmOk = document.getElementById("confirm-ok");
const confirmCancel = document.getElementById("confirm-cancel");

let timeout;
let estArrete = true;
let dateDepart = null;
let tempsEcoule = 0;
let tourActuel = 1;

/* Cette fonction gère le déroulement du temps.
Elle s'appelle elle-même toutes les 10ms tant que le chronomètre n'est pas arrêté. */
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

/* Démarre le chronomètre si il est arrêté. */
const demarrer = () => {
    if (estArrete) {
        estArrete = false;
        dateDepart = Date.now();
        defilerTemps();
    }
};

/* Arrête le chronomètre si il est en cours. */
const arreter = () => {
    if (!estArrete) {
        estArrete = true;
        clearTimeout(timeout);
        tempsEcoule += Date.now() - dateDepart;
    }
};

/* Réinitialise le chronomètre et les tours après confirmation de l'utilisateur. */
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
enregistrerSessionBtn.addEventListener("click", async () => {
    await envoyerCourseAuServeur();
    alert("Session complète envoyée au serveur !");
});

async function envoyerCourseAuServeur() {

    const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));

    if (!coureur?.nomComplet) {
        alert("Identité élève introuvable.");
        return;
    }

    const parts = coureur.nomComplet.trim().split(" ");
    const prenom = parts[0] || "";
    const nom = parts.slice(1).join(" ") || "";

    if (!prenom || !nom) {
        alert("Identité invalide.");
        return;
    }

    const dateSeance = new Date().toISOString();

    const listeTours = document.querySelectorAll("#listeTours span");

    if (listeTours.length === 0) {
        alert("Aucun tour enregistré.");
        return;
    }

    const tempsAuTour = Array.from(listeTours).map(span => {

        const texte = span.textContent.split(": ")[1]; // "mm:ss:ms"

        if (!texte) return 0;

        const [m, s, cs] = texte.split(":").map(Number);

        // votre affichage est en centièmes (00-99)
        // conversion correcte vers millisecondes
        return (m * 60000) + (s * 1000) + (cs * 10);
    });

    const request = {
        prenom,
        nom,
        distance: coureur.vma_distance ? parseFloat(coureur.vma_distance) : 0,
        nbTours: tempsAuTour.length,
        nbTirsReussi: [],
        tempsAuPasDeTir: [],
        tempsAuTour
    };

    try {

        const response = await fetch("/api/biathlon", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });

        if (!response.ok) {
            const err = await response.text();
            console.error("Erreur serveur :", err);
            alert("Erreur lors de l'envoi.");
            return;
        }

        alert("Session envoyée avec succès.");

        setTimeout(() => {
            window.location.href = "seance.html";
        }, 1000);

    } catch (e) {
        console.error("Erreur fetch course :", e);
        alert("Erreur réseau.");
    }
}