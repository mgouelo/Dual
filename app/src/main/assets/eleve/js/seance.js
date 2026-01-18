document.addEventListener("DOMContentLoaded", () => {
    // On récupère le niveau stocké (6eme ou 4eme)
    const niveau = localStorage.getItem("niveau") || "6eme";
    const btnEval = document.getElementById("btn-eval");
    const btnTir = document.getElementById("btn-tir")

    btnEval.onclick = () => {
        if (niveau === "6eme") {
            window.location.href = "epreuve6eme.html";

        } else {
            window.location.href = "epreuve4eme.html";
        }
    };

    // Redirection pour l'Entraînement au Tir
    if (btnTir) {
        btnTir.onclick = () => {
            if (niveau === "6eme") {
                // Renvoie vers le tir 6ème
                window.location.href = "tir.html";
            } else {
                // Renvoie vers le tir 4ème
                window.location.href = "tir_4eme.html";
            }
        };
    }
});