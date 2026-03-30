document.addEventListener("DOMContentLoaded", async () => {
    // On récupère le niveau depuis le localStorage
    const niveau = localStorage.getItem("niveau") || "6eme";

// On demande TOUJOURS au serveur le type de séance en cours.
// Le paramètre ?t= force le navigateur Android à ne pas utiliser le cache.
    let typeSeance = "Entraînement";
    try {
        const rep = await fetch('/api/seance/active?t=' + Date.now(), {
            cache: "no-store",
            headers: { "Cache-Control": "no-cache" }
        });
        if (rep.ok) {
            const seance = await rep.json();
            typeSeance = seance.type;
            localStorage.setItem("seance_type", seance.type);
        } else {
            typeSeance = localStorage.getItem("seance_type") || "Entraînement";
        }
    } catch (e) {
        typeSeance = localStorage.getItem("seance_type") || "Entraînement";
    }

// --- 1. NETTOYAGE ABSOLU DE L'INTERFACE ---
// On détruit tous les cadres gris inutiles
    document.querySelectorAll(".mode-group, .separator").forEach(el => el.remove());

// --- 2. TRANSFORMATION DU BOUTON ROUGE ---
// On cible l'ancien bouton rouge "Changer de niveau"
    const boutonRougeRetour = document.querySelector(".btn-back, a[href*='choix_niveau']");

    if (boutonRougeRetour) {
        // On change son texte
        boutonRougeRetour.textContent = "Changer de binôme";

        // On désactive le lien HTML par défaut s'il y en a un<
        if (boutonRougeRetour.tagName.toLowerCase() === 'a') {
            boutonRougeRetour.href = "#";
        }

        // Au clic, on vide la mémoire des élèves et on retourne à l'accueil
        boutonRougeRetour.onclick = (e) => {
            e.preventDefault();
            localStorage.removeItem("eleve1");
            localStorage.removeItem("eleve2");
            localStorage.removeItem("active_index");
            window.location.href = "../index.html";
        };
    }

// On cherche l'endroit où injecter notre nouveau cadre propre
    const conteneurPrincipal = boutonRougeRetour ? boutonRougeRetour.parentElement : document.body;

// --- 3. RECONSTRUCTION DYNAMIQUE ---
    /**
     * Crée un bloc de mode (Test VMA, Épreuve Finale ou Entraînement) avec les boutons correspondants
     * @param titre Le titre du bloc (ex: "TEST VMA")
     * @param boutons Un tableau d'objets représentant les boutons à créer, avec les propriétés :
     */
    const creerBloc = (titre, boutons) => {
        const group = document.createElement("div");
        group.className = "mode-group";

        const h3 = document.createElement("h3");
        h3.className = "mode-title";
        h3.textContent = titre;
        group.appendChild(h3);

        const btnGroup = document.createElement("div");
        btnGroup.className = "btn-group";

        boutons.forEach(b => {
            const btn = document.createElement("button");
            btn.className = "button " + (b.classSupplementaire || "");
            btn.textContent = b.texte;
            btn.onclick = () => window.location.href = b.url;
            btnGroup.appendChild(btn);
        });

        group.appendChild(btnGroup);

        // On l'insère juste au-dessus du bouton rouge
        if (boutonRougeRetour) {
            conteneurPrincipal.insertBefore(group, boutonRougeRetour);
        } else {
            conteneurPrincipal.appendChild(group);
        }
    };

// --- 4. AFFICHAGE DU BON CADRE SELON LE PROFESSEUR ---
    if (typeSeance === "Test VMA") {
        creerBloc("TEST VMA", [
            { texte: "Démarrer le Test VMA", url: "vma.html", classSupplementaire: "btn-eval" }
        ]);
    }
    else if (typeSeance === "Épreuve Finale") {
        creerBloc("ÉVALUATION FINALE", [
            // Le bouton de l'épreuve (Vert)
            { texte: "Lancer l'Épreuve Finale", url: (niveau === "6eme") ? "epreuve6eme.html" : "epreuve4eme.html", classSupplementaire: "btn-eval" },
            // LE NOUVEAU BOUTON HISTORIQUE
            { texte: "Visualiser mon Historique", url: "historique.html", classSupplementaire: "btn-historique" }
        ]);
    }
    else {
        // C'est un Entraînement
        creerBloc("ENTRAÎNEMENT", [
            { texte: "Course d'Entraînement", url: "course.html" },
            { texte: "Entraînement au Tir", url: (niveau === "6eme") ? "tir.html" : "tir4eme.html" }
        ]);
    }

// --- 5. GESTION DU BINÔME ---
    let e1 = JSON.parse(localStorage.getItem("eleve1"));
    let e2 = JSON.parse(localStorage.getItem("eleve2"));
    let indexActif = parseInt(localStorage.getItem("active_index") || "0");

    /**
     * Met à jour l'interface pour afficher le nom et le genre du coureur actif, et stocke son identité dans le localStorage pour que les autres pages puissent y accéder
     */
    function actualiserInterface() {
        let nomAffiche = document.getElementById("current-name");
        let genreAffiche = document.getElementById("current-gender");
        let coureur = (indexActif === 0) ? e1 : e2;

        if (nomAffiche && genreAffiche && coureur) {
            nomAffiche.textContent = coureur.nomComplet;
            genreAffiche.textContent = "(" + coureur.genre + ")";
            localStorage.setItem("coureur_actif_objet", JSON.stringify(coureur));

            try {
                if (typeof afficherParcoursVMA === "function") afficherParcoursVMA();
            } catch (e) {}
        }
    }

    window.inverserRoles = function() {
        indexActif = (indexActif === 0) ? 1 : 0;
        localStorage.setItem("active_index", indexActif);
        actualiserInterface();
    };

    actualiserInterface();

});
