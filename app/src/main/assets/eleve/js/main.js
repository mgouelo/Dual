const container = document.getElementById("buttons");

/**
 * Classe pour charger et afficher toutes les classes (étape 1)
 */
async function chargerClasses() {
    try {
        const response = await fetch('/api/classes/all');
        const classes = await response.json();

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

/**
 * Classe pour charger les élèves de la classe sélectionnée (étape 2)
 */
async function chargerElevesDeLaClasse(nomClasse) {
    try {
        const response = await fetch(`/api/eleves/par-classe/${nomClasse}`);
        const eleves = await response.json();

        container.innerHTML = `<h2>Classe ${nomClasse}</h2>`;

        eleves.forEach(nomComplet => {
            const btn = document.createElement("button");
            btn.className = "button";
            btn.textContent = nomComplet;

            btn.onclick = () => {
                //On stocke les infos pour les pages suivantes (Tir, Course)
                localStorage.setItem("eleve_identite", nomComplet);
                localStorage.setItem("eleve_classe", nomClasse);

                //Redirection vers le choix du niveau (6ème ou 4ème)
                window.location.href = "pages/choix_niveau.html";
            };

            container.appendChild(btn);
        });

        //Bouton pour revenir en arrière si on s'est trompé de classe
        const btnRetour = document.createElement("button");
        btnRetour.textContent = "⬅ Retour aux classes";
        btnRetour.className = "button btn-back";
        btnRetour.style.marginTop = "20px";
        btnRetour.onclick = chargerClasses;
        container.appendChild(btnRetour);

    } catch (error) {
        container.innerHTML = "<p>Erreur lors du chargement des élèves.</p>";
    }
}

// Lancement au chargement de la page
window.onload = chargerClasses;