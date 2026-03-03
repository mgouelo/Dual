let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let stopBtn = document.getElementById("stop");
let startBtn = document.getElementById("start");
let vitesseAff = document.getElementById("vitesse");
let distanceAff = document.getElementById("distance");
let resultat = document.getElementById("resultat");
let parcours = document.getElementById("parcours");
let resumeBtn = document.getElementById("resume");
const modal = document.getElementById("custom-confirm");
const confirmOk = document.getElementById("confirm-ok");
const confirmCancel = document.getElementById("confirm-cancel");

let minutes = 0;
let secondes = 0;
let millisecondes = 0;
let timeout;
let estArrete = true;
let tempsEcoule = 0; // en secondes
let currentIndex = 0;

/// Données extraites de VMA.pdf
// tempsCumule est converti en secondes (ex: 1.30 = 90 secondes)
let tableVma = [
    // Palier 8 km/h
    { vitesse: 8, vma: 7.00, distance: 50, tempsCumule: 23 },
    { vitesse: 8, vma: 7.19, distance: 100, tempsCumule: 45 },
    { vitesse: 8, vma: 7.38, distance: 150, tempsCumule: 68 },
    { vitesse: 8, vma: 7.56, distance: 200, tempsCumule: 90 },
    { vitesse: 8, vma: 7.75, distance: 250, tempsCumule: 113 },
    { vitesse: 8, vma: 8.00, distance: 300, tempsCumule: 135 },
    // Palier 9 km/h
    { vitesse: 9, vma: 8.17, distance: 350, tempsCumule: 155 },
    { vitesse: 9, vma: 8.33, distance: 400, tempsCumule: 175 },
    { vitesse: 9, vma: 8.50, distance: 450, tempsCumule: 195 },
    { vitesse: 9, vma: 8.67, distance: 500, tempsCumule: 215 },
    { vitesse: 9, vma: 8.83, distance: 550, tempsCumule: 235 },
    { vitesse: 9, vma: 9.00, distance: 600, tempsCumule: 255 },
    // Palier 10 km/h
    { vitesse: 10, vma: 9.15, distance: 650, tempsCumule: 273 },
    { vitesse: 10, vma: 9.30, distance: 700, tempsCumule: 291 },
    { vitesse: 10, vma: 9.45, distance: 750, tempsCumule: 309 },
    { vitesse: 10, vma: 9.60, distance: 800, tempsCumule: 327 },
    { vitesse: 10, vma: 9.75, distance: 850, tempsCumule: 345 },
    { vitesse: 10, vma: 9.90, distance: 900, tempsCumule: 363 },
    { vitesse: 10, vma: 10.00, distance: 950, tempsCumule: 381 },
    // Palier 11 km/h
    { vitesse: 11, vma: 10.14, distance: 1000, tempsCumule: 397 },
    { vitesse: 11, vma: 10.27, distance: 1050, tempsCumule: 414 },
    { vitesse: 11, vma: 10.41, distance: 1100, tempsCumule: 430 },
    { vitesse: 11, vma: 10.55, distance: 1150, tempsCumule: 446 },
    { vitesse: 11, vma: 10.68, distance: 1200, tempsCumule: 463 },
    { vitesse: 11, vma: 10.82, distance: 1250, tempsCumule: 479 },
    { vitesse: 11, vma: 10.95, distance: 1300, tempsCumule: 496 },
    { vitesse: 11, vma: 11.00, distance: 1350, tempsCumule: 512 },
    // Palier 12 km/h
    { vitesse: 12, vma: 11.13, distance: 1400, tempsCumule: 527 },
    { vitesse: 12, vma: 11.25, distance: 1450, tempsCumule: 542 },
    { vitesse: 12, vma: 11.38, distance: 1500, tempsCumule: 557 },
    { vitesse: 12, vma: 11.50, distance: 1550, tempsCumule: 572 },
    { vitesse: 12, vma: 11.63, distance: 1600, tempsCumule: 587 },
    { vitesse: 12, vma: 11.75, distance: 1650, tempsCumule: 602 },
    { vitesse: 12, vma: 11.88, distance: 1700, tempsCumule: 617 },
    { vitesse: 12, vma: 12.00, distance: 1750, tempsCumule: 632 },
    // Palier 13 km/h
    { vitesse: 13, vma: 12.12, distance: 1800, tempsCumule: 646 },
    { vitesse: 13, vma: 12.23, distance: 1850, tempsCumule: 660 },
    { vitesse: 13, vma: 12.35, distance: 1900, tempsCumule: 673 },
    { vitesse: 13, vma: 12.46, distance: 1950, tempsCumule: 687 },
    { vitesse: 13, vma: 12.58, distance: 2000, tempsCumule: 701 },
    { vitesse: 13, vma: 12.69, distance: 2050, tempsCumule: 715 },
    { vitesse: 13, vma: 12.81, distance: 2100, tempsCumule: 729 },
    { vitesse: 13, vma: 12.92, distance: 2150, tempsCumule: 743 },
    { vitesse: 13, vma: 13.00, distance: 2200, tempsCumule: 757 },
    // Palier 14 km/h
    { vitesse: 14, vma: 13.11, distance: 2250, tempsCumule: 769 },
    { vitesse: 14, vma: 13.21, distance: 2300, tempsCumule: 782 },
    { vitesse: 14, vma: 13.32, distance: 2350, tempsCumule: 795 },
    { vitesse: 14, vma: 13.43, distance: 2400, tempsCumule: 808 },
    { vitesse: 14, vma: 13.54, distance: 2450, tempsCumule: 821 },
    { vitesse: 14, vma: 13.64, distance: 2500, tempsCumule: 834 },
    { vitesse: 14, vma: 13.75, distance: 2550, tempsCumule: 847 },
    { vitesse: 14, vma: 13.86, distance: 2600, tempsCumule: 859 },
    { vitesse: 14, vma: 13.96, distance: 2650, tempsCumule: 872 },
    { vitesse: 14, vma: 14.00, distance: 2700, tempsCumule: 885 },
    // Palier 15 km/h
    { vitesse: 15, vma: 14.10, distance: 2750, tempsCumule: 897 },
    { vitesse: 15, vma: 14.20, distance: 2800, tempsCumule: 909 },
    { vitesse: 15, vma: 14.30, distance: 2850, tempsCumule: 921 },
    { vitesse: 15, vma: 14.40, distance: 2900, tempsCumule: 933 },
    { vitesse: 15, vma: 14.50, distance: 2950, tempsCumule: 945 },
    { vitesse: 15, vma: 14.60, distance: 3000, tempsCumule: 957 },
    { vitesse: 15, vma: 14.70, distance: 3050, tempsCumule: 969 },
    { vitesse: 15, vma: 14.80, distance: 3100, tempsCumule: 981 },
    { vitesse: 15, vma: 14.90, distance: 3150, tempsCumule: 993 },
    { vitesse: 15, vma: 15.00, distance: 3200, tempsCumule: 1005 },
    // Palier 16 km/h
    { vitesse: 16, vma: 15.09, distance: 3250, tempsCumule: 1016 },
    { vitesse: 16, vma: 15.19, distance: 3300, tempsCumule: 1028 },
    { vitesse: 16, vma: 15.28, distance: 3350, tempsCumule: 1039 },
    { vitesse: 16, vma: 15.38, distance: 3400, tempsCumule: 1050 },
    { vitesse: 16, vma: 15.47, distance: 3450, tempsCumule: 1061 },
    { vitesse: 16, vma: 15.56, distance: 3500, tempsCumule: 1073 },
    { vitesse: 16, vma: 15.66, distance: 3550, tempsCumule: 1084 },
    { vitesse: 16, vma: 15.75, distance: 3600, tempsCumule: 1095 },
    { vitesse: 16, vma: 15.84, distance: 3650, tempsCumule: 1106 },
    { vitesse: 16, vma: 15.94, distance: 3700, tempsCumule: 1118 },
    { vitesse: 16, vma: 16.00, distance: 3750, tempsCumule: 1129 }
];

// Récupération de l'élève qui court
const coureurActif = JSON.parse(localStorage.getItem("coureur_actif_objet"));
const niveau = localStorage.getItem("niveau") || "6eme";
const nomCoureurActif = document.getElementById("nom-eleve-vma");
const vmaActuelleAff = document.getElementById("vma-actuelle");

// Afficher le nom du coureur sur la page de test
if (coureurActif && nomCoureurActif) {
    nomCoureurActif.textContent = coureurActif.nomComplet;

    // Affichage de la VMA actuelle
    if (coureurActif.vma && coureurActif.vma > 0) {
        vmaActuelleAff.textContent = coureurActif.vma.toFixed(1);
    } else {
        vmaActuelleAff.textContent = "N/A";
    }
}

const defilerTemps = () => {
    if (estArrete) return;

    millisecondes += 10;
    tempsEcoule += 0.01;

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

    if (currentIndex < tableVma.length) {
        if (tempsEcoule >= tableVma[currentIndex].tempsCumule) {
            vitesseAff.textContent = tableVma[currentIndex].vitesse.toFixed(1);
            distanceAff.textContent = `${tableVma[currentIndex].distance}`;
            currentIndex++;
        }
    }

    timeout = setTimeout(defilerTemps, 10);
};

const btnSaveVma = document.getElementById("btn-save-vma");
let vmaTemporaire = 0; // Pour stocker la valeur avant validation
let parcoursTexteTemporaire = "";
let badgeTemporaire = "";

const afficherResultatVMA = () => {
    // Définitions des parcours pour chaque niveau
    const coupellesJaunes = "Parcours : (coupelles jaunes – 250m)"
    const coupellesBleues = "Parcours : (coupelles bleues – 300m)"
    const coupellesRouges = "Parcours : (coupelles rouges – 350m)"
    const plotsVerts = "Parcours : (plots verts – 275m)"
    const plotsBleus = "Parcours : (plots bleus – 325m)"
    const plotsRouges = "Parcours : (plots rouges – 375m)"
    const grandTour = "Parcours : (grand tour – 400m)"

    // On prend la VMA du palier précédent (car le coureur n'a pas réussi à tenir le palier actuel)
    const vmaReelle = tableVma[Math.max(0, currentIndex - 1)].vma;
    vmaTemporaire = Math.ceil(vmaReelle * 2) / 2; // On stocke la vma temporaire arrondie au 0.5 supérieur pour validation

    // Coupelles jaunes : 9.5 ou 10
    if (vmaTemporaire <= 9.5 || vmaTemporaire === 10) {
        badgeTemporaire = "bg-jaune";
        parcoursTexteTemporaire = coupellesJaunes;

        // Plots verts : 10.5 ou 11
    } else if (vmaTemporaire === 10.5 || vmaTemporaire === 11) {
        badgeTemporaire = "bg-vert";
        parcoursTexteTemporaire = plotsVerts;

        // Coupelles bleues : 11.5 ou 12
    } else if (vmaTemporaire === 11.5 || vmaTemporaire === 12) {
        badgeTemporaire = "bg-bleu";
        parcoursTexteTemporaire = coupellesBleues;

        // Plots bleus : 12.5 ou 13
    } else if (vmaTemporaire === 12.5 || vmaTemporaire === 13) {
        badgeTemporaire = "bg-bleu";
        parcoursTexteTemporaire = plotsBleus;

        // Coupelles rouges : 13.5 ou 14
    } else if (vmaTemporaire === 13.5 || vmaTemporaire === 14) {
        badgeTemporaire = "bg-rouge";
        parcoursTexteTemporaire = coupellesRouges;

        // Plots rouges : 14.5 ou 15
    } else if (vmaTemporaire === 14.5 || vmaTemporaire === 15) {
        badgeTemporaire = "bg-rouge";
        parcoursTexteTemporaire = plotsRouges;

        // Grand tour : > 15
    } else {
        badgeTemporaire = "bg-noir";
        parcoursTexteTemporaire = grandTour;
    }

    // Génération du HTML de la carte de résultat
    resultat.innerHTML = `
        <div class="vma-card">
            <p class="vma-label">VMA réelle : ${vmaReelle.toFixed(2)} km/h</p>
            <span class="vma-main-val">${vmaTemporaire.toFixed(1)} km/h</span>
            <div class="parcours-badge ${badgeTemporaire}">${parcoursTexteTemporaire}</div>
        </div>
    `;

    btnSaveVma.style.display = "inline-block";
}

const enregistrerNouvelleVMA = async () => {
    if (vmaTemporaire === 0 || !coureurActif) return;

    const message = `Confirmer l'enregistrement de ${vmaTemporaire.toFixed(1)} km/h pour ${coureurActif.nomComplet} ?`;
    const confirmation = await demanderConfirmation(message);

    if (confirmation) {
        // Mise à jour de l'objet local
        coureurActif.vma = vmaTemporaire;
        coureurActif.vma_badge = badgeTemporaire; // Stocke "bg-bleu", "bg-jaune", etc.
        coureurActif.vma_parcours = parcoursTexteTemporaire; // Stocke le nom du parcours

        // Sauvegarde pour le Hub (séance actuelle)
        localStorage.setItem("coureur_actif_objet", JSON.stringify(coureurActif));

        // Mise à jour du binôme (eleve1 ou eleve2)
        const activeIndex = localStorage.getItem("active_index");
        const key = (activeIndex === "0") ? "eleve1" : "eleve2";
        localStorage.setItem(key, JSON.stringify(coureurActif));

        // Sauvegarde permanente en BDD
        await sauvegarderVmaServeur(coureurActif.id_eleve || coureurActif.id, vmaTemporaire, coureurActif.classe);

        // Feedback visuel et nettoyage
        btnSaveVma.style.display = "none";
        resultat.innerHTML += `<p style="color: #27ae60; font-weight: bold; margin-top: 10px;">VMA enregistrée avec succès !</p>`;
    }
};

// Ajouter l'écouteur d'événement pour le bouton
btnSaveVma.addEventListener("click", enregistrerNouvelleVMA);

const demarrer = () => {
    if (estArrete) {
        estArrete = false;
        resultat.textContent = "";
        parcours.textContent = "";
        defilerTemps();
    }
};

const arreter = () => {
    if (!estArrete) {
        estArrete = true;
        clearTimeout(timeout);

        // Calcul de la VMA réelle atteinte (en km/h) si le coureur n'atteint même pas le premier palier
        if (currentIndex === 0) {
            currentIndex = 1;
        }

        // On appelle la fonction qui centralise calcul et affichage
        afficherResultatVMA();

        // On fige l'interface
        startBtn.style.display = "none";
        stopBtn.style.display = "none";
        resumeBtn.style.display = "inline-block";
    }
};

const reset = async() => {
    const confirmation = await demanderConfirmation("Réinitialiser le chronomètre ?");

    if (confirmation) {
        estArrete = true;
        clearTimeout(timeout);
        minutes = secondes = millisecondes = tempsEcoule = 0;
        currentIndex = 0;
        chrono.textContent = "00:00:00";
        vitesseAff.textContent = tableVma[0].vitesse.toFixed(1);
        distanceAff.textContent = "0";
        resultat.textContent = "";
        parcours.textContent = "";
        resumeBtn.style.display = "none";
        btnSaveVma.style.display = "none";
        // Réaffichage des boutons
        startBtn.style.display = "inline-block";
        stopBtn.style.display = "inline-block";
    }
};

const reprendre = async () => {
    const confirmation = await demanderConfirmation("Voulez-vous vraiment reprendre le test ?");

    if (confirmation) {
        estArrete = false;

        // On cache le résultat temporaire et le bouton enregistrer
        resultat.textContent = "";
        btnSaveVma.style.display = "none";

        // On réaffiche les boutons normaux
        resumeBtn.style.display = "none";
        startBtn.style.display = "none"; // Optionnel : on garde le Stop uniquement
        stopBtn.style.display = "inline-block";

        // On relance le défilement
        defilerTemps();
    }
};

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

/**
 * Envoie la VMA calculée au serveur pour la sauvegarder dans la base de données.
 * @param eleveId L'identifiant de l'élève pour lequel on sauvegarde la VMA
 * @param vmaValeur La valeur de la VMA à sauvegarder (en km/h)
 * @returns {Promise<void>} Une promesse qui se résout lorsque la requête est terminée
 */
async function sauvegarderVmaServeur(eleveId, vmaValeur, nomClasse) {
    try {
        const response = await fetch('/api/eleves/update-vma', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                id: eleveId,
                vma: vmaValeur
            })
        });

        if (response.ok) {
            console.log("VMA synchronisée avec la base de données.");

            // Actualiser l'affichage de la VMA sur la page après sauvegarde
            if (vmaActuelleAff) {
                vmaActuelleAff.textContent = vmaValeur.toFixed(1);
                vmaActuelleAff.style.color = "#27ae60";
            }
        } else {
            console.error("Échec de la sauvegarde serveur.");
        }
    } catch (error) {
        console.error("Erreur réseau :", error);
    }
}

startBtn.addEventListener("click", demarrer);
stopBtn.addEventListener("click", arreter);
resetBtn.addEventListener("click", reset);
resumeBtn.addEventListener("click", reprendre);
