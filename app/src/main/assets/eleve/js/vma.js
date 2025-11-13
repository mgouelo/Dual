let chrono = document.getElementById("chrono");
let resetBtn = document.getElementById("reset");
let stopBtn = document.getElementById("stop");
let startBtn = document.getElementById("start");
let vitesseAff = document.getElementById("vitesse");
let distanceAff = document.getElementById("distance");
let resultat = document.getElementById("resultat");

let minutes = 0;
let secondes = 0;
let millisecondes = 0;
let timeout;
let estArrete = true;
let tempsEcoule = 0; // en secondes
let currentIndex = 0;

// Tableau fourni par l'ecole
let tableVma = [
  { vitesse: 8, distance: 50, tempsCumule: 23 },
  { vitesse: 8, distance: 100, tempsCumule: 45 },
  { vitesse: 8, distance: 150, tempsCumule: 68 },
  { vitesse: 8, distance: 200, tempsCumule: 90 },
  { vitesse: 8, distance: 250, tempsCumule: 113 },
  { vitesse: 8, distance: 300, tempsCumule: 135 },
  { vitesse: 9, distance: 350, tempsCumule: 155 },
  { vitesse: 9, distance: 400, tempsCumule: 175 },
  { vitesse: 9, distance: 450, tempsCumule: 195 },
  { vitesse: 9, distance: 500, tempsCumule: 215 },
  { vitesse: 9, distance: 550, tempsCumule: 235 },
  { vitesse: 9, distance: 600, tempsCumule: 255 },
  { vitesse: 10, distance: 650, tempsCumule: 273 },
  { vitesse: 10, distance: 700, tempsCumule: 291 },
  { vitesse: 10, distance: 750, tempsCumule: 309 },
  { vitesse: 10, distance: 800, tempsCumule: 327 },
  { vitesse: 10, distance: 850, tempsCumule: 345 },
  { vitesse: 10, distance: 900, tempsCumule: 363 },
  { vitesse: 10, distance: 950, tempsCumule: 381 },
  { vitesse: 11, distance: 1000, tempsCumule: 397 },
  { vitesse: 11, distance: 1050, tempsCumule: 414 },
  { vitesse: 11, distance: 1100, tempsCumule: 430 },
  { vitesse: 11, distance: 1150, tempsCumule: 446 },
  { vitesse: 11, distance: 1200, tempsCumule: 463 },
  { vitesse: 11, distance: 1250, tempsCumule: 479 },
  { vitesse: 11, distance: 1300, tempsCumule: 496 },
  { vitesse: 11, distance: 1350, tempsCumule: 512 },
  { vitesse: 12, distance: 1400, tempsCumule: 527 },
  { vitesse: 12, distance: 1450, tempsCumule: 542 },
  { vitesse: 12, distance: 1500, tempsCumule: 557 },
  { vitesse: 12, distance: 1550, tempsCumule: 572 },
  { vitesse: 12, distance: 1600, tempsCumule: 587 },
  { vitesse: 12, distance: 1650, tempsCumule: 602 },
  { vitesse: 12, distance: 1700, tempsCumule: 617 },
  { vitesse: 12, distance: 1750, tempsCumule: 632 },
  { vitesse: 13, distance: 1800, tempsCumule: 646 },
  { vitesse: 13, distance: 1850, tempsCumule: 660 },
  { vitesse: 13, distance: 1900, tempsCumule: 673 },
  { vitesse: 13, distance: 1950, tempsCumule: 687 },
  { vitesse: 13, distance: 2000, tempsCumule: 701 },
  { vitesse: 13, distance: 2050, tempsCumule: 715 },
  { vitesse: 13, distance: 2100, tempsCumule: 729 },
  { vitesse: 13, distance: 2150, tempsCumule: 743 },
  { vitesse: 13, distance: 2200, tempsCumule: 757 },
  { vitesse: 14, distance: 2250, tempsCumule: 769 },
  { vitesse: 14, distance: 2300, tempsCumule: 782 },
  { vitesse: 14, distance: 2350, tempsCumule: 795 },
  { vitesse: 14, distance: 2400, tempsCumule: 808 },
  { vitesse: 14, distance: 2450, tempsCumule: 821 },
  { vitesse: 14, distance: 2500, tempsCumule: 834 },
  { vitesse: 14, distance: 2550, tempsCumule: 847 },
  { vitesse: 14, distance: 2600, tempsCumule: 859 },
  { vitesse: 14, distance: 2650, tempsCumule: 872 },
  { vitesse: 14, distance: 2700, tempsCumule: 885 },
  { vitesse: 15, distance: 2750, tempsCumule: 897 },
  { vitesse: 15, distance: 2800, tempsCumule: 909 },
  { vitesse: 15, distance: 2850, tempsCumule: 921 },
  { vitesse: 15, distance: 2900, tempsCumule: 933 },
  { vitesse: 15, distance: 2950, tempsCumule: 945 },
  { vitesse: 15, distance: 3000, tempsCumule: 957 },
  { vitesse: 15, distance: 3050, tempsCumule: 969 },
  { vitesse: 15, distance: 3100, tempsCumule: 981 },
  { vitesse: 15, distance: 3150, tempsCumule: 993 },
  { vitesse: 15, distance: 3200, tempsCumule: 1005 },
  { vitesse: 16, distance: 3250, tempsCumule: 1016 },
  { vitesse: 16, distance: 3300, tempsCumule: 1028 },
  { vitesse: 16, distance: 3350, tempsCumule: 1039 },
  { vitesse: 16, distance: 3400, tempsCumule: 1050 },
  { vitesse: 16, distance: 3450, tempsCumule: 1061 },
  { vitesse: 16, distance: 3500, tempsCumule: 1073 },
  { vitesse: 16, distance: 3550, tempsCumule: 1084 },
  { vitesse: 16, distance: 3600, tempsCumule: 1095 },
  { vitesse: 16, distance: 3650, tempsCumule: 1106 },
  { vitesse: 16, distance: 3700, tempsCumule: 1118 },
  { vitesse: 16, distance: 3750, tempsCumule: 1129 },
  { vitesse: 17, distance: 3800, tempsCumule: 1140 },
  { vitesse: 17, distance: 3850, tempsCumule: 1150 },
  { vitesse: 17, distance: 3900, tempsCumule: 1161 },
  { vitesse: 17, distance: 3950, tempsCumule: 1171 },
  { vitesse: 17, distance: 4000, tempsCumule: 1182 },
  { vitesse: 17, distance: 4050, tempsCumule: 1192 },
  { vitesse: 17, distance: 4100, tempsCumule: 1203 },
  { vitesse: 17, distance: 4150, tempsCumule: 1214 },
  { vitesse: 17, distance: 4200, tempsCumule: 1224 },
  { vitesse: 17, distance: 4250, tempsCumule: 1235 },
  { vitesse: 17, distance: 4300, tempsCumule: 1245 },
  { vitesse: 17, distance: 4350, tempsCumule: 1256 },
  { vitesse: 18, distance: 4400, tempsCumule: 1266 },
  { vitesse: 18, distance: 4450, tempsCumule: 1276 },
  { vitesse: 18, distance: 4500, tempsCumule: 1286 },
  { vitesse: 18, distance: 4550, tempsCumule: 1296 },
  { vitesse: 18, distance: 4600, tempsCumule: 1306 },
  { vitesse: 18, distance: 4650, tempsCumule: 1316 },
  { vitesse: 18, distance: 4700, tempsCumule: 1326 },
  { vitesse: 18, distance: 4750, tempsCumule: 1336 },
  { vitesse: 18, distance: 4800, tempsCumule: 1346 },
  { vitesse: 18, distance: 4850, tempsCumule: 1356 },
  { vitesse: 18, distance: 4900, tempsCumule: 1366 },
  { vitesse: 18, distance: 4950, tempsCumule: 1376 },
  { vitesse: 19, distance: 5000, tempsCumule: 1385 },
  { vitesse: 19, distance: 5050, tempsCumule: 1395 },
  { vitesse: 19, distance: 5100, tempsCumule: 1404 },
  { vitesse: 19, distance: 5150, tempsCumule: 1414 },
  { vitesse: 19, distance: 5200, tempsCumule: 1423 },
  { vitesse: 19, distance: 5250, tempsCumule: 1433 },
  { vitesse: 19, distance: 5300, tempsCumule: 1442 },
  { vitesse: 19, distance: 5350, tempsCumule: 1452 },
  { vitesse: 19, distance: 5400, tempsCumule: 1461 },
  { vitesse: 19, distance: 5450, tempsCumule: 1471 },
  { vitesse: 19, distance: 5500, tempsCumule: 1480 },
  { vitesse: 19, distance: 5550, tempsCumule: 1490 },
  { vitesse: 19, distance: 5600, tempsCumule: 1499 },
  { vitesse: 20, distance: 5650, tempsCumule: 1508 },
  { vitesse: 20, distance: 5700, tempsCumule: 1517 },
  { vitesse: 20, distance: 5750, tempsCumule: 1526 },
  { vitesse: 20, distance: 5800, tempsCumule: 1535 },
  { vitesse: 20, distance: 5850, tempsCumule: 1544 },
  { vitesse: 20, distance: 5900, tempsCumule: 1553 },
  { vitesse: 20, distance: 5950, tempsCumule: 1562 },
  { vitesse: 20, distance: 6000, tempsCumule: 1571 },
  { vitesse: 20, distance: 6050, tempsCumule: 1580 },
  { vitesse: 20, distance: 6100, tempsCumule: 1589 },
  { vitesse: 20, distance: 6150, tempsCumule: 1598 },
  { vitesse: 20, distance: 6200, tempsCumule: 1607 },
  { vitesse: 20, distance: 6250, tempsCumule: 1616 },
  { vitesse: 20, distance: 6300, tempsCumule: 1625 },
];

const defilerTemps = () => {
    if (estArrete) return;

    millisecondes += 10;
    tempsEcoule += 0.01;

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

    if (currentIndex < tableVma.length) {
        if (tempsEcoule >= tableVma[currentIndex].tempsCumule) {
            vitesseAff.textContent = tableVma[currentIndex].vitesse.toFixed(1);
            distanceAff.textContent = `${tableVma[currentIndex].distance}`;
            currentIndex++;
        }
    }

    timeout = setTimeout(defilerTemps, 10);
};

const demarrer = () => {
    if (estArrete) {
        estArrete = false;
        resultat.textContent = "";
        defilerTemps();
    }
};

const arreter = () => {
    if (!estArrete) {
        estArrete = true;
        clearTimeout(timeout);
        resultat.textContent = `VMA atteinte : ${tableVma[currentIndex].vitesse.toFixed(1)} km/h`;
    }
};

const reset = () => {
    estArrete = true;
    clearTimeout(timeout);
    minutes = secondes = millisecondes = tempsEcoule = 0;
    currentIndex = 0;
    chrono.textContent = "00:00:00";
    vitesseAff.textContent = tableVma[0].vitesse.toFixed(1);
    distanceAff.textContent = `${tableVma[0].distance}`;
    resultat.textContent = "";
};

startBtn.addEventListener("click", demarrer);
stopBtn.addEventListener("click", arreter);
resetBtn.addEventListener("click", reset);
