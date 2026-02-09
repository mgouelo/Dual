let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let startBtn = document.getElementById("start");
let enregistrerBtn = document.getElementById("enregistrer");
const modal = document.getElementById("custom-confirm");
const confirmOk = document.getElementById("confirm-ok");
const confirmCancel = document.getElementById("confirm-cancel");

let minutes = 0;
let secondes = 0;
let millisecondes = 0;
let timeout;
let estArrete = true;
let tourActuel = 1;

const defilerTemps = () => {
    if (estArrete) return;

    millisecondes += 10;

    if (millisecondes === 1000) {
        millisecondes = 0;
        secondes++;
    }

    if (secondes === 60) {
        secondes = 0;
        minutes++;
    }

    // Initialiser les variables pour l'affichage
    let m = minutes;
    let s = secondes;
    let ms = Math.floor(millisecondes / 10);

    // Ajouter un 0 si inférieur à 10
    if(m < 10) {
        m = "0" + m;
    }

    if(s < 10) {
        s = "0" + s;
    }

    if(ms < 10) {
        ms = "0" + ms;
    }

    chrono.textContent = `${m}:${s}:${ms}`;

    timeout = setTimeout(defilerTemps, 10);
};

const demarrer = () => {
    if (estArrete) {
        estArrete = false;
        defilerTemps();
    }
};

const reset = async() => {
    const confirmation = await demanderConfirmation("Réinitialiser le chronomètre ?");

    if (confirmation) {
        estArrete = true;
        clearTimeout(timeout);
        minutes = secondes = millisecondes = 0;
        chrono.textContent = "00:00:00";
    }

    // Réinitialiser les tours
    const listeTours = document.getElementById("listeTours");
    if (listeTours) {
        listeTours.innerHTML = "";
        tourActuel = 1;
    }
};

const demanderConfirmation = (message) => {
    document.getElementById("confirm-message").textContent = message;

    modal.style.display = "flex";
    // Petit timeout pour laisser le navigateur appliquer le display:flex
    // avant de lancer l'animation CSS
    setTimeout(() => {
        modal.classList.add("show");
    }, 10);

    return new Promise((resolve) => {
        confirmOk.onclick = () => {
            modal.classList.remove("show");
            setTimeout(() => { modal.style.display = "none"; }, 300);
            resolve(true);
        };
        confirmCancel.onclick = () => {
            modal.classList.remove("show");
            setTimeout(() => { modal.style.display = "none"; }, 300);
            resolve(false);
        };
    });
};

const enregistrer = () => {
    if(chrono.textContent != "00:00:00"){
        const listeTours = document.getElementById("listeTours"); // conteneur de tous les tours
        const nouveauTour = document.createElement("span");
        nouveauTour.innerHTML = `Tour ${tourActuel}: ${chrono.textContent}<br><br> `;
        listeTours.appendChild(nouveauTour);

        tourActuel++;
    }
};

let currentSerie = 1;
const inputTir = document.getElementById('input-tir');
const numSerieDisplay = document.getElementById('num-serie');

// Gestion des btn + et - pour changer de série
document.getElementById('ajoutTir').addEventListener('click', () => {
    if (inputTir.value < 5) {
        inputTir.value = parseInt(inputTir.value) + 1;
    }
});

document.getElementById('retirerTir').addEventListener('click', () => {
    if (inputTir.value > 0) {
        inputTir.value = parseInt(inputTir.value) - 1;
    }
});

// // Validation de la série en cours
// const validerTourBtn = document.getElementById('valider-serie').addEventListener('click', () => {
//     const score = inputTir.value;
//
// });

startBtn.addEventListener("click", demarrer);
resetBtn.addEventListener("click", reset);
enregistrerBtn.addEventListener("click", enregistrer);