let niveau6emeBtn = document.getElementById("niveau-6eme");
let niveau4emeBtn = document.getElementById("niveau-4eme");

niveau6emeBtn.addEventListener("click", () => choisirNiveau("6eme"));
niveau4emeBtn.addEventListener("click", () => choisirNiveau("4eme"));

function choisirNiveau(niveau) {

    // On stocke le choix pour que toutes les pages suivantes sachent quel mode utiliser
    localStorage.setItem("niveau", niveau);
    // Redirection vers la page de séance principale
    window.location.href = "seance.html";
}