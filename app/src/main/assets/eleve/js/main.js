const container = document.getElementById("buttons");
const typeClasse = document.getElementById("typeClasse");
const retourClasseBtn = document.getElementById("retourClasseBtn");
const btnRetourBinomes = document.getElementById("btnRetourBinomes");

/**
 * Classe pour charger et afficher toutes les classes (étape 1)
 */
async function chargerClasses() {
    try {
        const response = await fetch('/api/classes/all');
        const classes = await response.json();
        btnRetourBinomes.style.display = "none"; // Masquer le bouton de retour du binôme

        // On ajoute la classe spécifique pour l'alignement vertical
        container.classList.add("flex-column");
        container.innerHTML = "<h2>Sélectionnez votre classe</h2>";

        if (classes.length === 0) {
            container.innerHTML += "<p>Aucune classe disponible.</p>";
            return;
        }

        classes.forEach(nomClasse => {
            const btn = document.createElement("button");
            btn.className = "button";
            btn.textContent = nomClasse;

            //Au clic, on passe à l'affichage des élèves de la classe
            btn.onclick = () => chargerElevesDeLaClasse(nomClasse);

            container.appendChild(btn);
        });
    } catch (error) {
        console.error("Erreur:", error);
        container.innerHTML = "<p>Erreur de connexion au serveur.</p>";
    }
}

let eleve1 = null; // Stocke le premier élève choisi
/**
 * Classe pour charger les élèves de la classe sélectionnée (étape 2)
 */
async function chargerElevesDeLaClasse(nomClasse) {
    try {
        const response = await fetch(`/api/eleves/par-classe/${nomClasse}`);
        const eleves = await response.json();
        btnRetourBinomes.style.display = "block"; // Afficher le bouton de retour du binôme

        // On vide le conteneur pour afficher les élèves
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

    } catch (error) {
        container.innerHTML = "<p>Erreur lors du chargement des élèves.</p>";
    }
}

// Lancement au chargement de la page
window.onload = chargerClasses;