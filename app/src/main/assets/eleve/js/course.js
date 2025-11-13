let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let stopBtn = document.getElementById("stop");
let startBtn = document.getElementById("start");
let enregistrerBtn = document.getElementById("enregistrer");

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

const arreter = () => {
    if (!estArrete) {
        estArrete = true;
        clearTimeout(timeout);
    }
};

const reset = () => {
    estArrete = true;
    clearTimeout(timeout);
    minutes = secondes = millisecondes = 0;
    chrono.textContent = "00:00:00";
};

const enregistrer = () => {
    if(chrono.textContent != "00:00:00"){
        if (tourActuel <= 6) {
            const leTour = document.querySelector(`#tour${tourActuel}`);
            const leChrono = leTour.parentElement;

            leChrono.style.display = "block";
            leTour.textContent = chrono.textContent;
            
            tourActuel++;

            estArrete = true;
            clearTimeout(timeout);
            minutes = secondes = millisecondes = 0;
            chrono.textContent = "00:00:00";
        }
    }
};

startBtn.addEventListener("click", demarrer);
stopBtn.addEventListener("click", arreter);
resetBtn.addEventListener("click", reset);
enregistrerBtn.addEventListener("click", enregistrer);
