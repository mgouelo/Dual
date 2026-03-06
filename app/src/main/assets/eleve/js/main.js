const container = document.getElementById("buttons");
const typeClasse = document.getElementById("typeClasse");
const retourClasseBtn = document.getElementById("retourClasseBtn");
const btnRetourBinomes = document.getElementById("btnRetourBinomes");

/**
 * Transforme "6A" en "6e A" ou "3Lisbonne" en "3e Lisbonne" pour l'affichage web
 */
async function chargerClasses() {
    try {
        const response = await fetch('/api/classes/all');
        const classes = await response.json();
        btnRetourBinomes.style.display = "none"; // Masquer le bouton de retour du binôme

        // On ajoute la classe spécifique pour l'alignement vertical
        container.classList.add("flex-column");
        container.innerHTML = "<h2>Sélectionnez votre classe</h2>";
function formaterNomClasse(nomBrut) {
    if (!nomBrut || nomBrut.length < 2) return nomBrut;

    const premierCaractere = nomBrut.charAt(0);

    // Vérifie si le premier caractère est un chiffre (0-9)
    if (!/^\d$/.test(premierCaractere)) {
        return nomBrut;
    }

    const reste = nomBrut.substring(1).trim();
    return `${premierCaractere}e ${reste}`;
}

/**
 * Supprime les accents d'une chaîne de caractères ("Adèle" -> "Adele")
 */
function enleverAccents(str) {
    if (!str) return "";
    return str.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
}

// Lancement au chargement de la page
window.onload = () => {
    // On lit l'URL (ex: http://serveur/?classe=3A)
    const urlParams = new URLSearchParams(window.location.search);
    const classePredefinie = urlParams.get('classe');

    if (classePredefinie) {
        chargerElevesDeLaClasse(classePredefinie);
    } else {
        container.innerHTML = "<p>Erreur : Aucune classe détectée. Veuillez scanner le QR Code du professeur.</p>";
    }
};

let eleve1 = null; // Stocke le premier élève choisi
/**
 * Charge les élèves de la classe et affiche les champs de recherche
 */
async function chargerElevesDeLaClasse(nomClasse) {
    try {
        const response = await fetch(`/api/eleves/par-classe/${encodeURIComponent(nomClasse)}`);
        const eleves = await response.json();

        btnRetourBinomes.style.display = "block"; // Afficher le bouton de retour du binôme

        // On vide le conteneur pour afficher les élèves
        // On vide l'écran de chargement
        container.innerHTML = "";
        container.classList.remove("flex-column");
        typeClasse.style.display = "block";

        // Gestion du titre en fonction de la sélection en cours
        let texteTitre = "";
        if (eleve1 === null) {
            texteTitre = "Qui utilise la tablette ?";
        } else {
            texteTitre = "Binôme avec " + eleve1.nomComplet + " : Sélectionnez le partenaire";
        }
        typeClasse.innerHTML = "<h2>" + texteTitre + "</h2>";

        // Filtrage de la liste
        let listeAffichee = [];
        if (eleve1 !== null) {
            // Si le premier élève est choisi, on filtre pour ne pas l'afficher
            for (let i = 0; i < eleves.length; i++) {
                if (eleves[i].nomComplet !== eleve1.nomComplet) {
                    listeAffichee.push(eleves[i]);
                }
            }
        } else {
            // Sinon on affiche tout le monde
            listeAffichee = eleves;
        }

        // Création des boutons
        listeAffichee.forEach(eleve => {
            const btn = document.createElement("button");
            btn.className = "button";
            btn.textContent = eleve.nomComplet;

            btn.onclick = function() {
                if (eleve1 === null) {
                    // Sélection du premier élève
                    eleve1 = eleve;
                    localStorage.setItem("eleve1", JSON.stringify(eleve));
                    chargerElevesDeLaClasse(nomClasse);
                } else {
                    // Sélection du second élève
                    localStorage.setItem("eleve2", JSON.stringify(eleve));
                    localStorage.setItem("active_index", "0");
                    window.location.href = "pages/choix_niveau.html";
                }
            };
            container.appendChild(btn);
        });

        // Gestion du bouton retour
        retourClasseBtn.innerHTML = "";
        const btnRetour = document.createElement("button");
        btnRetour.className = "button btn-back";

        if (eleve1 !== null) {
            btnRetour.textContent = "⬅ Changer le 1er élève";
        } else {
            btnRetour.textContent = "⬅ Retour aux classes";
        }

        btnRetour.onclick = function() {
            if (eleve1 !== null) {
                eleve1 = null;
                chargerElevesDeLaClasse(nomClasse);
            } else {
                chargerClasses();
            }
        };
        retourClasseBtn.appendChild(btnRetour);

        // formatage pour le titre
        typeClasse.innerHTML = `<h2>Classe ${formaterNomClasse(nomClasse)}</h2><p>Constituez votre binôme (Recherche par prénom) :</p>`;

        // Variables pour stocker les vrais objets élève sélectionnés
        let eleve1Selectionne = null;
        let eleve2Selectionne = null;

        // génère une barre de recherche
        function creerChampRecherche(id, placeholder, onSelectCallback) {
            const wrapper = document.createElement('div');
            wrapper.className = 'search-container';

            const input = document.createElement('input');
            input.type = 'text';
            input.id = id;
            input.placeholder = placeholder;
            input.className = 'input-search';
            input.autocomplete = "off"; // empêche l'historique des entrées navigateur (Sinon la tablette pourrait proposer le nom de l'élève qui avait la tablette au cours d'avant)

            const suggestionsDiv = document.createElement('div');
            suggestionsDiv.className = 'suggestions-list';

            // Quand l'élève tape au clavier
            input.addEventListener('input', function() {
                // récupère la saisie, on met en minuscule, on enlève les espaces et les accents
                const valeurSaisie = enleverAccents(this.value.toLowerCase().trim());
                suggestionsDiv.innerHTML = ''; // On vide

                if (!valeurSaisie) return;

                // filtre en enlevant aussi les accents des prénoms de la liste
                const resultats = eleves.filter(el => {
                    const prenomFiltre = enleverAccents(el.prenom.toLowerCase());
                    return prenomFiltre.startsWith(valeurSaisie);
                });

                resultats.forEach(eleve => {
                    const item = document.createElement('div');
                    item.className = 'suggestion-item';

                    // affiche le vrai prénom (avec accents) à l'écran
                    item.textContent = `${eleve.prenom} ${eleve.nom}`;

                    // Clic sur une suggestion
                    item.addEventListener('click', function() {
                        input.value = item.textContent; // Affiche dans le champ
                        suggestionsDiv.innerHTML = '';  // Ferme la liste
                        onSelectCallback(eleve);        // Sauvegarde l'élève en mémoire
                    });

                    suggestionsDiv.appendChild(item);
                });
            });

            // Fermer les suggestions si on clique dans le vide
            document.addEventListener('click', function(e) {
                if (e.target !== input) suggestionsDiv.innerHTML = '';
            });

            wrapper.appendChild(input);
            wrapper.appendChild(suggestionsDiv);
            return wrapper;
        }
        // ---------------------------------------------------

        // création et ajout des deux champs
        const champEleve1 = creerChampRecherche('eleve1', 'Prénom Élève 1...', (eleve) => { eleve1Selectionne = eleve; });
        const champEleve2 = creerChampRecherche('eleve2', 'Prénom Élève 2...', (eleve) => { eleve2Selectionne = eleve; });

        container.appendChild(champEleve1);
        container.appendChild(document.createElement('br'));
        container.appendChild(champEleve2);

        // création du bouton de validation (il réutilise ta classe "button")
        const btnValider = document.createElement('button');
        btnValider.textContent = "Valider le binôme";
        btnValider.className = "button";
        btnValider.style.marginTop = "25px";

        // logique de validation
        btnValider.onclick = () => {
            if (!eleve1Selectionne || !eleve2Selectionne) {
                alert("Veuillez rechercher et sélectionner les deux élèves dans la liste.");
                return;
            }
            if (eleve1Selectionne.prenom === eleve2Selectionne.prenom && eleve1Selectionne.nom === eleve2Selectionne.nom) {
                alert("Un élève ne peut pas faire équipe avec lui-même !");
                return;
            }

            // stocke dans le cache du navigateur
            localStorage.setItem("eleve1_prenom", eleve1Selectionne.prenom);
            localStorage.setItem("eleve1_nom", eleve1Selectionne.nom);
            localStorage.setItem("eleve2_prenom", eleve2Selectionne.prenom);
            localStorage.setItem("eleve2_nom", eleve2Selectionne.nom);

            // stocke le nom de classe brut (ex: "6A") pour pouvoir requêter la BDD plus tard
            localStorage.setItem("eleve_classe", nomClasse);

            //  page suivante a modifier après
            window.location.href = "pages/choix_niveau.html";
        };

        container.appendChild(document.createElement('br'));
        container.appendChild(btnValider);

    } catch (error) {
        console.error("Erreur:", error);
        container.innerHTML = "<p>Erreur lors de la récupération des élèves.</p>";
    }
}