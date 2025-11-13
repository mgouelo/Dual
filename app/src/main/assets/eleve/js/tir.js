document.addEventListener("DOMContentLoaded", () => {
  const nombreDeTirs = 6;

  function calculerResultats() {
    let total = 0;
    let compteTirs = 0; // nombre de tirs réellement saisis

    for (let i = 1; i <= nombreDeTirs; i++) {
      const valeurStr = document.getElementById(`tir${i}`).value;
      const valeur = parseFloat(valeurStr);

      if (!isNaN(valeur) && valeurStr !== "") {
        total += valeur;
        compteTirs++;
      }
    }

    let moyenne = 0;
    if (compteTirs > 0) {
      moyenne = total / compteTirs;
    }

    const resultatsDiv = document.getElementById("resultats");
    resultatsDiv.style.display = "block";

    document.getElementById("total").textContent = total.toFixed(1);
    document.getElementById("moyenne").textContent = moyenne.toFixed(2);
  }

  for (let i = 1; i <= nombreDeTirs; i++) {
    const inputTir = document.getElementById(`tir${i}`);
    if (inputTir) {
      inputTir.addEventListener("input", calculerResultats);
    }
  }
});
