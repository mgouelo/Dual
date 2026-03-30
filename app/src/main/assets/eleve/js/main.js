const container = document.getElementById("buttons");
const typeClasse = document.getElementById("typeClasse");
const retourClasseBtn = document.getElementById("retourClasseBtn");
const btnRetourBinomes = document.getElementById("btnRetourBinomes");

let eleve1 = null;

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
        const eleves = await response.json();

        if(btnRetourBinomes) btnRetourBinomes.style.display = "block";

        container.innerHTML = "";
        container.classList.remove("flex-column");
        typeClasse.style.display = "block";

        let texteTitre = (eleve1 === null) ? "Qui utilise la tablette ?" : "Binôme avec " + eleve1.nomComplet + " : Sélectionnez le partenaire";
        typeClasse.innerHTML = "<h2>" + texteTitre + "</h2>";

        // Filtrage pour ne pas afficher le 1er élève s'il est déjà choisi
        let listeAffichee = (eleve1 !== null) ? eleves.filter(e => e.nomComplet !== eleve1.nomComplet) : eleves;

        listeAffichee.forEach(eleve => {
            const btn = document.createElement("button");
            btn.className = "button";
            btn.textContent = eleve.nomComplet;

            btn.onclick = function() {
                if (eleve1 === null) {
                    eleve1 = eleve;
                    localStorage.setItem("eleve1", JSON.stringify(eleve));
                    chargerElevesDeLaClasse(nomClasse);
                } else {
                    localStorage.setItem("eleve2", JSON.stringify(eleve));
                    localStorage.setItem("active_index", "0");

                    //On saute le choix du niveau
                    window.location.href = "pages/seance.html";
                }
            };
            container.appendChild(btn);
        });

        //Gestion du bouton de retour "Changer le 1er élève"
        retourClasseBtn.innerHTML = "";
        if (eleve1 !== null) {
            const btnRetour = document.createElement("button");
            btnRetour.className = "button btn-back";
            btnRetour.textContent = "⬅ Changer le 1er élève";
            btnRetour.onclick = function() {
                eleve1 = null;
                chargerElevesDeLaClasse(nomClasse);
            };
            retourClasseBtn.appendChild(btnRetour);
        }

    } catch (error) {
        container.innerHTML = "<p>Erreur lors du chargement des élèves.</p>";
    }
}

//On lance la fonction principale au chargement de la page index
window.onload = initialiserTablette;