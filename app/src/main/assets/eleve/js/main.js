// Binomes temporaires pour démonstration
const binomes = [
    { id: "1", noms: "Binôme 1" },
    { id: "2", noms: "Binôme 2" },
    { id: "3", noms: "Binôme 3" },
    { id: "4", noms: "Binôme 4" }
];

const initialiserPortail = () => {
    // On récupère l'élément ici pour être sûr que le DOM est chargé
    const containerBoutons = document.getElementById("buttons");

    // Vérification de sécurité pour éviter l'erreur "null"
    if (!containerBoutons) return;

    // On attend 1.5 seconde (1500ms) avant d'afficher les boutons
    setTimeout(() => {
        containerBoutons.innerHTML = ""; // On efface le texte de chargement

        binomes.forEach(b => {
            const btn = document.createElement("button");
            // On applique la classe .student définie dans ton CSS actuel
            btn.className = "button";
            btn.textContent = b.noms;

            btn.onclick = () => {
                // Sauvegarde du choix
                localStorage.setItem("binome_actif", b.noms);
                localStorage.setItem("binome_id", b.id);

                // Redirection vers la page de séance
                window.location.href = "pages/seance.html";
            };

            containerBoutons.appendChild(btn);
        });
    }, 1500);
};

window.onload = initialiserPortail;