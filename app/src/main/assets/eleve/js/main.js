const container = document.getElementById("buttons");
const typeClasse = document.getElementById("typeClasse");
const retourClasseBtn = document.getElementById("retourClasseBtn");
const btnRetourBinomes = document.getElementById("btnRetourBinomes");

let elevesClasse = []; // stock les élèves de toute la classe
let eleve1 = null;
let eleve2 = null;

//ÉTAPE 1 : Connexion avec le professeur
/**
 * Fonction principale qui initialise la tablette en se connectant au serveur pour connaître la séance en cours, mémorise les choix du professeur et affiche les élèves de la classe correspondante
 * @returns {Promise<void>} Une promesse qui se résout lorsque l'initialisation est terminée, avec gestion des erreurs et mise à jour de l'interface en conséquence
 */
async function initialiserTablette() {
    try {
        //On demande au serveur quelle est la séance en cours
        const response = await fetch('/api/seance/active');

        if (!response.ok) {
            container.classList.add("flex-column");
            container.innerHTML = "<h2>En attente du professeur...</h2><p style='text-align:center;'>Aucune séance n'est actuellement lancée sur l'appareil du professeur.</p>";
            return;
        }

        const seance = await response.json();

        //On mémorise les choix du professeur en local
        localStorage.setItem("seance_type", seance.type);
        localStorage.setItem("seance_classe", seance.classe);

        //Déduction automatique du niveau (Si le nom de la classe contient 4 ou 3 -> mode 4ème, sinon 6ème)
        if (seance.classe.includes("4") || seance.classe.includes("3")) {
            localStorage.setItem("niveau", "4eme");
        } else {
            localStorage.setItem("niveau", "6eme");
        }

        //On saute l'écran de sélection de classe et on affiche directement les élèves
        chargerElevesDeLaClasse(seance.classe);

    } catch (error) {
        console.error("Erreur:", error);
        container.innerHTML = "<p>Erreur de connexion au serveur. Vérifiez le réseau.</p>";
    }
}

//ÉTAPE 2 : Choix du binôme
/**
 * Affiche la liste des élèves de la classe donnée, permet de sélectionner le 1er élève puis le 2ème élève pour former un binôme, et gère l'affichage en fonction du choix effectué
 * @param nomClasse Le nom de la classe dont on veut afficher les élèves (ex: "6A", "4B", etc.)
 * @returns {Promise<void>} Une promesse qui se résout lorsque le chargement des élèves est terminé, avec gestion des erreurs et mise à jour de l'interface en conséquence
 */
async function chargerElevesDeLaClasse(nomClasse) {
    try {
        const response = await fetch(`/api/eleves/par-classe/${nomClasse}`);
        elevesClasse = await response.json(); // On sauvegarde la liste pour la recherche

        if(btnRetourBinomes) btnRetourBinomes.style.display = "block";

        container.classList.remove("flex-column");
        typeClasse.style.display = "block";
        typeClasse.innerHTML = "<h2>Formation du binôme</h2><p style='color: var(--blue);'>Recherchez et sélectionnez les deux élèves.</p>";

        // injection de la nouvelle interface de recherche
        container.innerHTML = `
            <div class="search-section">
                <div class="search-container">
                    <label for="searchEleve1" class="search-label">Élève 1</label>
                    <input type="text" id="searchEleve1" class="search-input" placeholder="Taper un prénom ou nom..." autocomplete="off">
                    <div id="suggestions1" class="suggestions-box"></div>
                </div>

                <div class="search-container" style="margin-top: 25px;">
                    <label for="searchEleve2" class="search-label">Élève 2</label>
                    <input type="text" id="searchEleve2" class="search-input" placeholder="Taper un prénom ou nom..." autocomplete="off">
                    <div id="suggestions2" class="suggestions-box"></div>
                </div>

                <button id="btnValiderBinome" class="button btn-eval" style="margin-top: 35px; opacity: 0.5; pointer-events: none;">Valider le binôme</button>
            </div>
        `;

        // init des barres de recherche
        setupAutocomplete("searchEleve1", "suggestions1", 1);
        setupAutocomplete("searchEleve2", "suggestions2", 2);

        // action du bouton de validation
        document.getElementById("btnValiderBinome").addEventListener("click", validerBinome);

        // vide le bouton de retour intermédiaire devenu inutile
        retourClasseBtn.innerHTML = "";

    } catch (error) {
        console.error("Erreur:", error);
        container.innerHTML = "<p>Erreur lors du chargement des élèves.</p>";
    }
}

/**
 * Fonction pour rendre la recherche + souple :
 * Supprime les accents, met en minuscules, et remplace les tirets/apostrophes par des espaces.
 */
function normaliserTexte(texte) {
    return texte.normalize("NFD")             // Sépare les lettres de leurs accents
                .replace(/[\u0300-\u036f]/g, "") // Supprime les accents
                .replace(/['\-]/g, " ")          // Remplace les apostrophes et tirets par des espaces
                .toLowerCase()                   // Tout en minuscules
                .trim();
}

/**
 * Configure la logique d'une barre de recherche
 */
function setupAutocomplete(inputId, suggestionsId, numEleve) {
    const input = document.getElementById(inputId);
    const suggestionsBox = document.getElementById(suggestionsId);

    input.addEventListener("input", function() {
        const requete = this.value;
        suggestionsBox.innerHTML = "";

        // Si l'élève modifie le champ manuellement alors on annule sa sélection pour le forcer à re-choisir
        if(numEleve === 1) eleve1 = null;
        if(numEleve === 2) eleve2 = null;
        verifierValidation();

        // recherche qu'à partir de 2 caractères tapés
        if (requete.length < 3) {
            suggestionsBox.style.display = "none";
            return;
        }

        const requeteNormalisee = normaliserTexte(requete);

        // filtrage des élèves
        const elevesFiltres = elevesClasse.filter(eleve => {
            // empeche de sélectionner le même élève 2 fois
            if (numEleve === 1 && eleve2 && eleve.id_eleve === eleve2.id_eleve) return false;
            if (numEleve === 2 && eleve1 && eleve.id_eleve === eleve1.id_eleve) return false;

            const nomCompletNormalise = normaliserTexte(eleve.nomComplet);
            return nomCompletNormalise.includes(requeteNormalisee);
        });

        // affichage des suggestions
        if (elevesFiltres.length > 0) {
            suggestionsBox.style.display = "block";
            elevesFiltres.forEach(eleve => {
                const div = document.createElement("div");
                div.className = "suggestion-item";
                div.textContent = eleve.nomComplet;
                div.onclick = function() {
                    // quand on clique sur une suggestion
                    input.value = eleve.nomComplet;
                    suggestionsBox.style.display = "none";
                    if(numEleve === 1) eleve1 = eleve;
                    if(numEleve === 2) eleve2 = eleve;
                    verifierValidation();
                };
                suggestionsBox.appendChild(div);
            });
        } else {
            suggestionsBox.style.display = "none";
        }
    });

    // cache la boîte de suggestions si on clique ailleurs sur l'écran
    document.addEventListener("click", function(e) {
        if (e.target !== input) {
            suggestionsBox.style.display = "none";
        }
    });
}

/**
 * active / désactive le bouton de validation selon si les 2 élèves sont bien sélectionnés
 */
function verifierValidation() {
    const btn = document.getElementById("btnValiderBinome");
    if (eleve1 !== null && eleve2 !== null) {
        btn.style.opacity = "1";
        btn.style.pointerEvents = "auto";
    } else {
        btn.style.opacity = "0.5";
        btn.style.pointerEvents = "none";
    }
}

/**
 * valide le binôme et passe à la page de séance
 */
function validerBinome() {
    if (eleve1 && eleve2) {
        localStorage.setItem("eleve1", JSON.stringify(eleve1));
        localStorage.setItem("eleve2", JSON.stringify(eleve2));
        localStorage.setItem("active_index", "0");
        window.location.href = "pages/seance.html";
    }
}

//On lance la fonction principale au chargement de la page index
window.onload = initialiserTablette;