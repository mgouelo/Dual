// ─── SÉLECTION DES ÉLÉMENTS ─────────────────────────────────────────────────
const conteneurTirs = document.getElementById("tirs");
const btnAjouter   = document.getElementById("ajouter-tir");
const btnSupprimer = document.getElementById("supprimer-tir");
const btnEnvoyer   = document.getElementById("btn-envoyer");
const btnRetourSeance = document.getElementById("retourSeance");


// Variable pour suivre le nombre de séries ajoutées
let nombreDeSeries = 0;
const MAX_SERIES = 20;

/**
 * Crée une carte de saisie pour une série de tir avec un champ de nombre
 * @param numero Le numéro de la série (1, 2, 3, etc.) pour l'affichage et l'identification du champ
 * @returns {HTMLDivElement} Un élément div contenant le label de la série et le champ de saisie pour le nombre de tirs réussis
 */
const creerCarte = (numero) => {
    const carte = document.createElement("div");
    carte.className = "serie-card";
    carte.dataset.serie = numero;
    carte.innerHTML = `
        <span class="serie-label">Série ${numero}</span>
        <input type="number" id="tir${numero}" min="0" max="5"
            placeholder="0–5" autocomplete="off"/>
    `;
    const input = carte.querySelector("input");
    input.addEventListener("input", () => {
        const v = parseInt(input.value);
        if (!isNaN(v)) input.value = Math.min(Math.max(v, 0), 5);
        calculerResultats();
    });
    return carte;
};

/**
 * Ajoute une nouvelle série de tir (champ de saisie) à l'interface
 * Limite le nombre de séries à MAX_SERIES pour éviter les abus
 * Met automatiquement le focus sur le nouveau champ ajouté
 * Recalcule les résultats après chaque ajout
 */
const ajouterSerie = () => {
    if (nombreDeSeries >= MAX_SERIES) return;
    nombreDeSeries++;
    conteneurTirs.appendChild(creerCarte(nombreDeSeries));
    conteneurTirs.querySelector(`#tir${nombreDeSeries}`)?.focus();
    calculerResultats();
};

/**
 * Supprime la dernière série de tir ajoutée (champ de saisie)
 * Ne fait rien si aucune série n'est présente
 * Recalcule les résultats après chaque suppression
 */
const supprimerSerie = () => {
    if (nombreDeSeries === 0) return;
    const carte = conteneurTirs.querySelector(`.serie-card[data-serie="${nombreDeSeries}"]`);
    if (carte) carte.remove();
    nombreDeSeries--;
    calculerResultats();
};

/**
 * Calcule le total des tirs réussis et affiche les résultats, y compris la médaille obtenue
 */
const calculerResultats = () => {
    const resultatsDiv = document.getElementById("resultats");
    const display      = document.getElementById("medaille-display");
    let total = 0, seriesCompletes = 0;

    for (let i = 1; i <= nombreDeSeries; i++) {
        const champ = document.getElementById(`tir${i}`);
        if (!champ) continue;
        const v = parseInt(champ.value);
        if (!isNaN(v) && champ.value !== "") { total += v; seriesCompletes++; }
    }

    if (nombreDeSeries === 0) {
        resultatsDiv.style.display = "none";
        if (btnEnvoyer) btnEnvoyer.style.display = "none";
        return;
    }

    resultatsDiv.style.display = "block";
    const maxPossible = nombreDeSeries * 5;
    document.getElementById("total").textContent = `${total} / ${maxPossible}`;

    if (seriesCompletes === nombreDeSeries) {
        const pct = (total / maxPossible) * 100;
        let medaille = "BRONZE", couleur = "#cd7f32";
        if (pct >= 90)      { medaille = "DIAMANT"; couleur = "#1456DB"; }
        else if (pct >= 80) { medaille = "PLATINE"; couleur = "#b9f2ff"; }
        else if (pct >= 70) { medaille = "OR";      couleur = "#ffd700"; }
        else if (pct >= 60) { medaille = "ARGENT";  couleur = "#c0c0c0"; }

        display.textContent = "Médaille : " + medaille;
        display.style.cssText = `background-color:${couleur}; padding:20px; border-radius:12px; font-size:1.6rem;`;
        localStorage.setItem("tir_total", total);
        localStorage.setItem("tir_medaille", medaille);

        if (btnEnvoyer) {
            btnEnvoyer.style.display = "block";
            btnEnvoyer.style.margin = "0 auto";
            btnEnvoyer.style.marginTop = "20px";
            btnEnvoyer.onclick = () => {
                btnEnvoyer.disabled = true;
                btnEnvoyer.textContent = "Envoi en cours...";
                envoyerResultatAuServeur(total, medaille, nombreDeSeries);
            };
        }
    } else {
        display.textContent = `Saisie en cours (${seriesCompletes}/${nombreDeSeries} séries remplies)`;
        display.style.backgroundColor = "transparent";
        if (btnEnvoyer) btnEnvoyer.style.display = "none";
    }
};

/**
 * Envoie les résultats du tir au serveur via une requête POST
 * @param total Le total des tirs réussis calculé à partir des champs de saisie
 * @param medaille La médaille obtenue en fonction du pourcentage de réussite (BRONZE, ARGENT, OR, PLATINE, DIAMANT)
 * @param nbSeries Le nombre de séries de tir saisies, utilisé pour construire le tableau des tirs réussis
 * @returns {Promise<void>} Une promesse qui se résout lorsque la requête est terminée, avec gestion des erreurs et mise à jour de l'interface en conséquence
 */
async function envoyerResultatAuServeur(total, medaille, nbSeries) {
    // Récupération de l'identité du coureur actif depuis le localStorage
    const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));

    // Vérification de la présence de l'identité du coureur avant de continuer
    if (!coureur?.nomComplet) {
        alert("Identité élève introuvable.");
        btnEnvoyer.disabled = false;
        btnEnvoyer.textContent = "Envoyer les résultats";
        return;
    }

    // Extraction du prénom et du nom à partir du nom complet du coureur
    const parts = coureur.nomComplet.trim().split(" ");
    const prenom = parts[0] || "";
    const nom    = parts.slice(1).join(" ") || "";

    // Construction du tableau des tirs réussis à partir des champs de saisie
    const nbTirsReussi = [];
    for (let i = 1; i <= nbSeries; i++) {
        const champ = document.getElementById(`tir${i}`);
        nbTirsReussi.push(champ ? (parseInt(champ.value) || 0) : 0);
    }

    // Construction de l'objet de requête à envoyer au serveur
    const request = {
        prenom,
        nom,
        distance:        coureur.vma_distance ? parseFloat(coureur.vma_distance) : 0,
        nbTours:         0,
        nbTirsReussi,
        tempsAuPasDeTir: [],
        tempsAuTour:     []
    };

    // Envoi de la requête au serveur avec gestion des erreurs et mise à jour de l'interface en fonction de la réponse
    try {
        const response = await fetch("/api/biathlon", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });
        if (response.ok) {
            btnEnvoyer.textContent = "Résultats transmis !";
            btnEnvoyer.style.backgroundColor = "#7f8c8d";
            setTimeout(() => { window.location.href = "../index.html"; }, 1500);
        } else {
            const err = await response.text();
            alert("Erreur serveur : " + err);
            btnEnvoyer.disabled = false;
            btnEnvoyer.textContent = "Envoyer les résultats";
        }
    } catch (error) {
        console.error("Erreur réseau :", error);
        btnEnvoyer.textContent = "Erreur de connexion";
        btnEnvoyer.disabled = false;
    }
}
// Initialisation de l'interface avec une série de tir par défaut
ajouterSerie();

/**
 * Affiche une boîte de confirmation avant de retourner à la page de session, pour éviter les pertes de données accidentelles
 * @returns {Promise<void>} Une promesse qui se résout lorsque l'utilisateur a pris une décision, avec redirection vers la page de session si il confirme, ou maintien sur la page actuelle s'il annule
 */
const retourSeance = async() =>{
    if (await demanderConfirmation("Abandonner la session en cours et retourner sur Session Biathlon ?")) {
        window.location.href = "../pages/seance.html";
    }
}

// Écouteurs d'événements pour les boutons d'ajout et de suppression de séries
btnAjouter.addEventListener("click", ajouterSerie);
btnSupprimer.addEventListener("click", supprimerSerie);
if(btnRetourSeance) btnRetourSeance.addEventListener("click", retourSeance);
