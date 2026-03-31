document.addEventListener("DOMContentLoaded", () => {

    const conteneurTirs = document.getElementById("tirs");
    const btnAjouter   = document.getElementById("ajouter-tir");
    const btnSupprimer = document.getElementById("supprimer-tir");
    const btnEnvoyer   = document.getElementById("btn-envoyer");

    let nombreDeSeries = 0;
    const MAX_SERIES = 20;

    // ─── CARTE DE SÉRIE ───────────────────────────────────────────────────────

    const creerCarte = (numero) => {
        const carte = document.createElement("div");
        carte.className = "serie-card";
        carte.dataset.serie = numero;
        carte.innerHTML = `
            <span class="serie-label">Série ${numero}</span>
            <input type="number" id="tir${numero}" min="0" max="5"
                placeholder="0–5" autocomplete="off"/>
        `;
        const input = carte.querySelector("input");
        input.addEventListener("input", () => {
            const v = parseInt(input.value);
            if (!isNaN(v)) input.value = Math.min(Math.max(v, 0), 5);
            calculerResultats();
        });
        return carte;
    };

    // ─── AJOUTER / SUPPRIMER ──────────────────────────────────────────────────

    const ajouterSerie = () => {
        if (nombreDeSeries >= MAX_SERIES) return;
        nombreDeSeries++;
        conteneurTirs.appendChild(creerCarte(nombreDeSeries));
        conteneurTirs.querySelector(`#tir${nombreDeSeries}`)?.focus();
        calculerResultats();
    };

    const supprimerSerie = () => {
        if (nombreDeSeries === 0) return;
        const carte = conteneurTirs.querySelector(`.serie-card[data-serie="${nombreDeSeries}"]`);
        if (carte) carte.remove();
        nombreDeSeries--;
        calculerResultats();
    };

    // ─── CALCUL DES RÉSULTATS ─────────────────────────────────────────────────

    const calculerResultats = () => {
        const resultatsDiv = document.getElementById("resultats");
        const display      = document.getElementById("medaille-display");
        let total = 0, seriesCompletes = 0;

        for (let i = 1; i <= nombreDeSeries; i++) {
            const champ = document.getElementById(`tir${i}`);
            if (!champ) continue;
            const v = parseInt(champ.value);
            if (!isNaN(v) && champ.value !== "") { total += v; seriesCompletes++; }
        }

        if (nombreDeSeries === 0) {
            resultatsDiv.style.display = "none";
            if (btnEnvoyer) btnEnvoyer.style.display = "none";
            return;
        }

        resultatsDiv.style.display = "block";
        const maxPossible = nombreDeSeries * 5;
        document.getElementById("total").textContent = `${total} / ${maxPossible}`;

        if (seriesCompletes === nombreDeSeries) {
            const pct = (total / maxPossible) * 100;
            let medaille = "BRONZE", couleur = "#cd7f32";
            if (pct >= 90)      { medaille = "DIAMANT"; couleur = "#1456DB"; }
            else if (pct >= 80) { medaille = "PLATINE"; couleur = "#b9f2ff"; }
            else if (pct >= 70) { medaille = "OR";      couleur = "#ffd700"; }
            else if (pct >= 60) { medaille = "ARGENT";  couleur = "#c0c0c0"; }

            display.textContent = "Médaille : " + medaille;
            display.style.cssText = `background-color:${couleur}; padding:20px; border-radius:12px; font-size:1.6rem;`;
            localStorage.setItem("tir_total", total);
            localStorage.setItem("tir_medaille", medaille);

            if (btnEnvoyer) {
                btnEnvoyer.style.display = "block";
                btnEnvoyer.style.margin = "0 auto";
                btnEnvoyer.style.marginTop = "20px";
                btnEnvoyer.onclick = () => {
                    btnEnvoyer.disabled = true;
                    btnEnvoyer.textContent = "Envoi en cours...";
                    envoyerResultatAuServeur(total, medaille, nombreDeSeries);
                };
            }
        } else {
            display.textContent = `Saisie en cours (${seriesCompletes}/${nombreDeSeries} séries remplies)`;
            display.style.backgroundColor = "transparent";
            if (btnEnvoyer) btnEnvoyer.style.display = "none";
        }
    };

    // ─── ENVOI AU SERVEUR ─────────────────────────────────────────────────────

    async function envoyerResultatAuServeur(total, medaille, nbSeries) {
        const coureur = JSON.parse(localStorage.getItem("coureur_actif_objet"));
        if (!coureur?.nomComplet) {
            alert("Identité élève introuvable.");
            btnEnvoyer.disabled = false;
            btnEnvoyer.textContent = "Envoyer les résultats";
            return;
        }

        const parts = coureur.nomComplet.trim().split(" ");
        const prenom = parts[0] || "";
        const nom    = parts.slice(1).join(" ") || "";

        const nbTirsReussi = [];
        for (let i = 1; i <= nbSeries; i++) {
            const champ = document.getElementById(`tir${i}`);
            nbTirsReussi.push(champ ? (parseInt(champ.value) || 0) : 0);
        }

        const request = {
            prenom,
            nom,
            distance:        coureur.vma_distance ? parseFloat(coureur.vma_distance) : 0,
            nbTours:         0,
            nbTirsReussi,
            tempsAuPasDeTir: [],
            tempsAuTour:     []
        };

        try {
            const response = await fetch("/api/biathlon", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(request)
            });
            if (response.ok) {
                btnEnvoyer.textContent = "Résultats transmis !";
                btnEnvoyer.style.backgroundColor = "#7f8c8d";
                setTimeout(() => { window.location.href = "../index.html"; }, 1500);
            } else {
                const err = await response.text();
                alert("Erreur serveur : " + err);
                btnEnvoyer.disabled = false;
                btnEnvoyer.textContent = "Envoyer les résultats";
            }
        } catch (error) {
            console.error("Erreur réseau :", error);
            btnEnvoyer.textContent = "Erreur de connexion";
            btnEnvoyer.disabled = false;
        }
    }

    // ─── INIT ─────────────────────────────────────────────────────────────────

    ajouterSerie();
    btnAjouter.addEventListener("click", ajouterSerie);
    btnSupprimer.addEventListener("click", supprimerSerie);
});