// ─── SÉLECTION DES ÉLÉMENTS ─────────────────────────────────────────────────
const btnNouveauTir = document.getElementById("btn-nouveau-tir");
const btnAnnulerTir = document.getElementById("btn-annuler-tir");
const btnEnvoyer    = document.getElementById("btn-envoyer");
const btnRetourSeance = document.getElementById("retourSeance");

const modalTir      = document.getElementById("modal-tir");
const titreTir      = document.getElementById("titre-tir");
const scoreTemp     = document.getElementById("score-temporaire");
const validerBtn    = document.getElementById("valider-tir");

const resultatsBox  = document.getElementById("resultats-6eme");
const listeTirs     = document.getElementById("liste-tirs");
const affichageTotal = document.getElementById("total");
const displayMedaille = document.getElementById("medaille-display");

const modalConfirm = document.getElementById("custom-confirm");
const confirmOk = document.getElementById("confirm-ok");
const confirmCancel = document.getElementById("confirm-cancel");

// Variable pour suivre le nombre de séries ajoutées
let series = [];
const MAX_SERIES = 20;

/**
 * Ouvre la modale pour saisir un nouveau tir
 */
const ouvrirModale = () => {
    // Vérifie si le nombre maximum de séries a été atteint avant d'ouvrir la modale
    if (series.length >= MAX_SERIES) {
        alert("Tu as atteint le nombre maximum de séries.");
        return;
    }
    titreTir.textContent = `Tir Série n°${series.length + 1}`;

    // Reset visuel
    document.querySelectorAll('.btn-score').forEach(b => b.classList.remove('selected'));
    scoreTemp.value = "-1";

    modalTir.style.display = "flex";
    setTimeout(() => modalTir.classList.add("show"), 10);
};

/**
 * Colore le bouton cliqué dans la modale
 */
function setScoreTir(valeur) {
    scoreTemp.value = valeur;
    const boutons = document.querySelectorAll('.btn-score');
    boutons.forEach(btn => {
        btn.classList.remove('selected');
        if(parseInt(btn.textContent) === valeur) {
            btn.classList.add('selected');
        }
    });
}
window.setScoreTir = setScoreTir;

/**
 * Valide le tir depuis la modale et l'ajoute à la liste
 */
const validerTir = () => {
    const score = parseInt(scoreTemp.value);

    if (score === -1 || isNaN(score)) {
        alert("Tu dois sélectionner un score avant de valider !");
        return;
    }

    // Ajout au tableau
    series.push(score);

    // Fermeture de la modale
    modalTir.style.display = "none";
    modalTir.classList.remove("show");

    actualiserAffichage();
};

/**
 * Supprime la dernière série saisie (en cas d'erreur)
 */
const annulerDernierTir = async() => {
    const confirmationAction = await demanderConfirmation("Voulez-vous vraiment annuler le dernier tir ?");

    if (confirmationAction) {
        if (series.length > 0) {
            series.pop();
            actualiserAffichage();
        }
    }
};

/**
 * Calcule le total des tirs réussis et affiche les résultats, y compris la médaille obtenue
 */
const actualiserAffichage = () => {
    if (series.length === 0) {
        resultatsBox.style.display = "none";
        btnEnvoyer.style.display = "none";
        btnAnnulerTir.style.display = "none";
        return;
    }

    resultatsBox.style.display = "block";
    btnAnnulerTir.style.display = "inline-block";
    btnEnvoyer.style.display = "block";

    let html = '<div style="display: flex; flex-direction: column; gap: 10px;">';
    let total = 0;

    // Affichage des tirs les uns sous les autres
    series.forEach((score, index) => {
        total += score;
        let couleurScore = score >= 4 ? "#27ae60" : (score >= 2 ? "#f39c12" : "#e74c3c");

        html += `
        <div style="display: flex; justify-content: space-between; align-items: center; background-color: #f8f9fa; padding: 12px 15px; border-radius: 8px; border-left: 5px solid #303586;">
            <div style="font-weight: bold; font-size: 1.1rem;">Série ${index + 1}</div>
            <span style="background-color: ${couleurScore}; color: white; padding: 4px 12px; border-radius: 12px; font-weight: bold; font-size: 1.1rem;">${score} / 5</span>
        </div>`;
    });
    html += '</div>';
    listeTirs.innerHTML = html;

    // Calcul du Total et de la Médaille
    const maxPossible = series.length * 5;
    affichageTotal.textContent = `${total} / ${maxPossible}`;

    const pct = (total / maxPossible) * 100;
    let medaille = "BRONZE", couleur = "#cd7f32";

    if (pct >= 90)      { medaille = "DIAMANT"; couleur = "#1456DB"; }
    else if (pct >= 80) { medaille = "PLATINE"; couleur = "#b9f2ff"; }
    else if (pct >= 70) { medaille = "OR";      couleur = "#ffd700"; }
    else if (pct >= 60) { medaille = "ARGENT";  couleur = "#c0c0c0"; }

    displayMedaille.textContent = "Médaille : " + medaille;
    displayMedaille.style.cssText = `background-color:${couleur}; padding:15px; border-radius:12px; font-size:1.4rem;`;
};

/**
 * Envoie les résultats du tir au serveur via une requête POST
 * @param total Le total des tirs réussis calculé à partir des champs de saisie
 * @param medaille La médaille obtenue en fonction du pourcentage de réussite (BRONZE, ARGENT, OR, PLATINE, DIAMANT)
 * @param nbSeries Le nombre de séries de tir saisies, utilisé pour construire le tableau des tirs réussis
 * @returns {Promise<void>} Une promesse qui se résout lorsque la requête est terminée, avec gestion des erreurs et mise à jour de l'interface en conséquence
 */
async function envoyerResultatAuServeur() {
    // 1. Demande de confirmation
    const confirmationAction = await demanderConfirmation("Mettre fin à la session et envoyer les données ?");

    if (confirmationAction) {

        // Sécurité : on vérifie qu'il y a bien des tirs à envoyer
        if (series.length === 0) {
            alert("Aucun tir enregistré.");
            return;
        }

        // Récupération de l'identité du coureur actif depuis le localStorage
        const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));

        // Vérification de la présence de l'identité du coureur avant de continuer
        if (!coureur?.nomComplet) {
            alert("Identité élève introuvable.");
            return;
        }

        // Extraction du prénom et du nom à partir du nom complet du coureur
        const parts = coureur.nomComplet.trim().split(" ");
        const prenom = parts[0] || "";
        const nom    = parts.slice(1).join(" ") || "";

        // Construction du tableau des tirs réussis à partir des champs de saisie
        // L'OBJET REQUEST (Super simplifié grâce au tableau `series`)
        const request = {
            prenom,
            nom,
            distance:        coureur.vma_distance ? parseFloat(coureur.vma_distance) : 0,
            nbTours:         0,
            nbTirsReussi:    series,                 // On envoie direct le tableau de scores !
            tempsAuPasDeTir: series.map(() => 0),    // On génère autant de "0" qu'il y a de séries
            tempsAuTour:     []
        };

        const btnEnvoyer = document.getElementById("btn-envoyer");
        if (btnEnvoyer) {
            btnEnvoyer.disabled = true;
            btnEnvoyer.textContent = "Envoi en cours...";
        }

        // Envoi de la requête au serveur avec gestion des erreurs et mise à jour de l'interface en fonction de la réponse
        try {
            const response = await fetch("/api/biathlon", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(request)
            });

            if (response.ok) {
                afficherToast("Résultat envoyé au professeur");
                if (btnEnvoyer) {
                    btnEnvoyer.textContent = "Résultats transmis !";
                    btnEnvoyer.style.backgroundColor = "#7f8c8d";
                    // Retour au Hub
                    setTimeout(() => { window.location.href = "seance.html"; }, 1500);
                }

                // On vide l'écran au bout d'une seconde pour que l'élève suivant puisse s'entraîner
                setTimeout(() => {
                    series = []; // On vide la mémoire
                    actualiserAffichage(); // Ça va cacher le bilan

                    if (btnEnvoyer) {
                        btnEnvoyer.disabled = false;
                        btnEnvoyer.textContent = "Envoyer les résultats";
                        btnEnvoyer.style.backgroundColor = "#27ae60"; // Retour au vert
                    }
                }, 1500);

            } else {
                const err = await response.text();
                alert("Erreur serveur : " + err);
                if (btnEnvoyer) {
                    btnEnvoyer.disabled = false;
                    btnEnvoyer.textContent = "Envoyer les résultats";
                }
            }
        } catch (error) {
            console.error("Erreur réseau :", error);
            if (btnEnvoyer) {
                btnEnvoyer.textContent = "Erreur de connexion";
                btnEnvoyer.disabled = false;
            }
        }
    }
}

/**
 * Affiche une boîte de confirmation avec un message personnalisé et retourne une promesse qui se résout en fonction du choix de l'utilisateur (OK ou Annuler)
 * @param message Le message à afficher dans la boîte de confirmation
 * @returns {Promise<unknown>} Une promesse qui se résout avec true si l'utilisateur clique sur OK, ou false s'il clique sur Annuler, après la fermeture de la boîte de confirmation avec une animation fluide
 */
const demanderConfirmation = (message) => {
    document.getElementById("confirm-message").textContent = message;
    modalConfirm.style.display = "flex";
    setTimeout(() => { modalConfirm.classList.add("show"); }, 10);

    return new Promise((resolve) => {
        confirmOk.onclick = () => {
            modalConfirm.classList.remove("show");
            setTimeout(() => { modalConfirm.style.display = "none"; }, 300);
            resolve(true);
        };
        confirmCancel.onclick = () => {
            modalConfirm.classList.remove("show");
            setTimeout(() => { modalConfirm.style.display = "none"; }, 300);
            resolve(false);
        };
    });
};

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

// Écouteurs d'événements pour les boutons d'ajout et de suppression de séries
btnNouveauTir.addEventListener("click", ouvrirModale);
validerBtn.addEventListener("click", validerTir);
btnAnnulerTir.addEventListener("click", annulerDernierTir);
btnEnvoyer.addEventListener("click", envoyerResultatAuServeur);
if (btnRetourSeance) btnRetourSeance.addEventListener("click", retourSeance);