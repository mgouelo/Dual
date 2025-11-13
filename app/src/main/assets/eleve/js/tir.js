const calculerBtn = document.getElementById("calculer");

calculerBtn.addEventListener("click", () => {
  let total = 0;
  let nombreDeTirs = 6;

  for (let i = 1; i <= nombreDeTirs; i++) {
    let valeur = parseFloat(document.getElementById(`tir${i}`).value);
    if (isNaN(valeur)){
        valeur = 0;
    }
    total += valeur;
  }

  const moyenne = total / nombreDeTirs;

  document.getElementById("total").textContent = total.toFixed(1);
  document.getElementById("moyenne").textContent = moyenne.toFixed(2);
});
