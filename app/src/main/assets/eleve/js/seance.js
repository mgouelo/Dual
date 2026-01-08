document.addEventListener("DOMContentLoaded", () => {
    // On récupère le niveau stocké (6eme ou 4eme)
    const niveau = localStorage.getItem("niveau") || "6eme";
    const btnEval = document.getElementById("btn-eval");

    btnEval.onclick = () => {
        if (niveau === "6eme") {
            window.location.href = "epreuve6eme.html";
        } else {
            window.location.href = "epreuve4eme.html";
        }
    };
});