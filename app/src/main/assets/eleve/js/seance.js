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

    // --- 2. TRANSFORMATION DU BOUTON ROUGE (SORTI DE CREERBLOC) ---
    const boutonRougeRetour = document.querySelector(".btn-back, a[href*='choix_niveau']");
    const modalConfirm = document.getElementById("custom-confirm");
    const confirmOk = document.getElementById("confirm-ok");
    const confirmCancel = document.getElementById("confirm-cancel");

    if (boutonRougeRetour) {
        boutonRougeRetour.textContent = "Changer de binôme";

        if (boutonRougeRetour.tagName.toLowerCase() === 'a') {
            boutonRougeRetour.href = "#";
        }

        boutonRougeRetour.onclick = async (e) => {
            e.preventDefault();
            // On appelle demanderConfirmation (définie plus bas ou déplacée en haut)
            const confirmation = await demanderConfirmation("Voulez-vous vraiment changer de binôme ? Les données non enregistrées seront perdues.");

            if (confirmation) {
                localStorage.removeItem("eleve1");
                localStorage.removeItem("eleve2");
                localStorage.removeItem("active_index");
                window.location.href = "../index.html";
            }
        };
    }

    // On cherche l'endroit où injecter notre nouveau cadre propre
    const conteneurPrincipal = boutonRougeRetour ? boutonRougeRetour.parentElement : document.body;

    // --- 3. RECONSTRUCTION DYNAMIQUE ---
    /**
     * Crée un bloc de mode (Test VMA, Épreuve Finale ou Entraînement) avec les boutons correspondants
     * @param titreAction Le titre principal du bloc (ex: "Initialisation", "Entraînement")
     * @param boutonsAction Un tableau d'objets représentant les boutons principaux
     * @param boutonSuivi (Optionnel) Un objet représentant le bouton d'historique pour générer la section "Suivi"
     */
    const creerBloc = (titreAction, boutonsAction, boutonSuivi = null) => {
        const group = document.createElement("div");
        group.className = "mode-group";

        // --- PARTIE HAUTE : ACTION PRINCIPALE ---
        const pAction = document.createElement("p");
        pAction.className = "mode-title";
        pAction.textContent = titreAction;
        group.appendChild(pAction);

        const btnGroup = document.createElement("div");
        btnGroup.className = "btn-group";

        boutonsAction.forEach(b => {
            const btn = document.createElement("button");
            btn.className = "button " + (b.classSupplementaire || "");
            btn.textContent = b.texte;
            btn.onclick = () => window.location.href = b.url;
            btnGroup.appendChild(btn);
        });

        group.appendChild(btnGroup);

        // --- PARTIE BASSE : SUIVI (HISTORIQUE) ---
        // Si un bouton de suivi a été passé, on génère la séparation et le bouton
        if (boutonSuivi) {
            const hr = document.createElement("hr");
            hr.className = "separator";
            group.appendChild(hr);

            const pSuivi = document.createElement("p");
            pSuivi.className = "mode-title";
            pSuivi.textContent = "Suivi";
            group.appendChild(pSuivi);

            const btnHist = document.createElement("button");
            btnHist.className = "button " + (boutonSuivi.classSupplementaire || "");
            btnHist.textContent = boutonSuivi.texte;
            btnHist.onclick = () => window.location.href = boutonSuivi.url;

            // Si aucune classe spécifique n'est passée, on applique le gris par défaut
            if (!boutonSuivi.classSupplementaire) {
                btnHist.style.background = "#989Ca0";
            }

            group.appendChild(btnHist);
        }

        if (boutonRougeRetour) {
            conteneurPrincipal.insertBefore(group, boutonRougeRetour);
        } else {
            conteneurPrincipal.appendChild(group);
        }
    };

    // --- 4. AFFICHAGE DU BON CADRE SELON LE PROFESSEUR ---
    if (typeSeance === "Test VMA") {
        creerBloc(
            "Initialisation",
            [
                { texte: "Faire le Test VMA", url: "vma.html", classSupplementaire: "btn-eval" }
            ]
        );
    }
    else if (typeSeance === "Épreuve Finale") {
        creerBloc(
            "Évaluation",
            [
                { texte: "Lancer l'Épreuve Finale", url: (niveau === "6eme") ? "epreuve6eme.html" : "epreuve4eme.html", classSupplementaire: "btn-eval" }
            ],
            // Le fameux 3ème paramètre qui déclenche l'affichage du Suivi
            { texte: "Voir l'historique", url: "historique.html", classSupplementaire: "btn-historique" }
        );
    }
    else {
        // C'est un Entraînement
        creerBloc(
            "Entraînement",
            [
                { texte: "Tir", url: (niveau === "6eme") ? "tir6eme.html" : "tir4eme.html" },
                { texte: "Course", url: "course.html" }
            ],
            // Le fameux 3ème paramètre qui déclenche l'affichage du Suivi
            { texte: "Voir l'historique", url: "historique.html", classSupplementaire: "btn-historique" }
        );
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

    actualiserInterface();

});
