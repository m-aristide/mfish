/* ============================================================================
   mfish — scripts externes (aucun script inline, compatible CSP stricte).
   - Configuration htmx : transmission du jeton CSRF sur chaque requete.
   - Micro-interactions : bascule de menus/pans via attributs data-*.
   ========================================================================== */

(function () {
    'use strict';

    // --- 1. htmx : jeton CSRF (lit les meta injectees par le gabarit) ---------
    document.addEventListener('DOMContentLoaded', function () {
        var jeton = document.querySelector('meta[name="_csrf"]');
        var entete = document.querySelector('meta[name="_csrf_header"]');
        if (!jeton || !entete) {
            return;
        }
        document.body.addEventListener('htmx:configRequest', function (evenement) {
            evenement.detail.headers[entete.content] = jeton.content;
        });
    });

    // --- 2. Micro-interactions : [data-bascule] cible [id] ---------------------
    document.addEventListener('click', function (evenement) {
        var declencheur = evenement.target.closest('[data-bascule]');
        if (!declencheur) {
            return;
        }
        var cibleId = declencheur.getAttribute('data-bascule');
        var cible = document.getElementById(cibleId);
        if (cible) {
            cible.classList.toggle('cache');
        }
    });

    // --- 3. Fermeture des pans via [data-fermer] -------------------------------
    document.addEventListener('click', function (evenement) {
        var fermer = evenement.target.closest('[data-fermer]');
        if (!fermer) {
            return;
        }
        var cibleId = fermer.getAttribute('data-fermer');
        var cible = document.getElementById(cibleId);
        if (cible) {
            cible.classList.add('cache');
        }
    });

    // --- 4. Graphiques Chart.js a partir de [data-chart] (JSON) -----------------
    document.addEventListener('DOMContentLoaded', function () {
        if (typeof window.Chart === 'undefined') {
            return;
        }
        document.querySelectorAll('[data-chart]').forEach(function (conteneur) {
            var donnees;
            try {
                donnees = JSON.parse(conteneur.getAttribute('data-chart'));
            } catch (erreur) {
                return;
            }
            var canvas = document.createElement('canvas');
            conteneur.appendChild(canvas);
            new window.Chart(canvas, {
                type: 'bar',
                data: {
                    labels: donnees.map(function (d) { return d.nom; }),
                    datasets: [{
                        label: conteneur.getAttribute('data-chart-label') || '',
                        data: donnees.map(function (d) { return d.quantite; }),
                        backgroundColor: '#0F766E'
                    }]
                },
                options: {
                    responsive: true,
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: true } }
                }
            });
        });
    });
})();
