/* ============================================================================
   mfish — Simulateur WhatsApp : logique de l'interface (aucun script inline).
   - Envoie le message au webhook (POST /simulateur/api/messages, jeton CSRF).
   - Poll les reponses de l'IA (GET /simulateur/api/conversations/{id}/messages).
   ========================================================================== */

(function () {
    'use strict';

    var API_MESSAGES = '/simulateur/api/messages';
    var API_CONVERSATIONS = '/simulateur/api/conversations';
    var DELAI_POLLING_MS = 1500;
    var DELAI_ABSENCE_REPONSE_MS = 45000;

    var conversationId = genererConversationId();
    var derniereTaille = 0;
    var enAttente = false;
    var minuteurAbsence = null;

    function genererConversationId() {
        return 'sim-' + Date.now().toString(36) + '-' +
            Math.random().toString(36).slice(2, 8);
    }

    function csrf() {
        var jeton = document.querySelector('meta[name="_csrf"]');
        var entete = document.querySelector('meta[name="_csrf_header"]');
        return {
            token: jeton ? jeton.content : '',
            header: entete ? entete.content : 'X-CSRF-TOKEN'
        };
    }

    function $ (selecteur) {
        return document.querySelector(selecteur);
    }

    function zoneMessages() {
        return $('#sim-messages');
    }

    function ajouterBulle(contenu, type) {
        var zone = zoneMessages();
        var vide = zone.querySelector('.sim-vide');
        if (vide) {
            vide.remove();
        }
        var bulle = document.createElement('div');
        bulle.className = 'bulle ' + type;
        bulle.textContent = contenu;
        zone.appendChild(bulle);
        zone.scrollTop = zone.scrollHeight;
    }

    function afficherEcrit() {
        $('#sim-typing').classList.remove('cache');
    }

    function cacherEcrit() {
        $('#sim-typing').classList.add('cache');
    }

    function armerAbsenceReponse() {
        clearTimeout(minuteurAbsence);
        minuteurAbsence = setTimeout(function () {
            if (!enAttente) {
                return;
            }
            enAttente = false;
            cacherEcrit();
            ajouterBulle('Pas de reponse : verifier DEEPSEEK_API_KEY et les logs (logs/app.log).',
                'systeme');
        }, DELAI_ABSENCE_REPONSE_MS);
    }

    function desarmerAbsenceReponse() {
        clearTimeout(minuteurAbsence);
    }

    function poller() {
        fetch(API_CONVERSATIONS + '/' + encodeURIComponent(conversationId) + '/messages', {
            credentials: 'same-origin'
        })
            .then(function (reponse) {
                if (!reponse.ok) {
                    throw new Error('polling en echec');
                }
                return reponse.json();
            })
            .then(function (messages) {
                if (!Array.isArray(messages)) {
                    return;
                }
                if (messages.length > derniereTaille) {
                    for (var i = derniereTaille; i < messages.length; i++) {
                        ajouterBulle(messages[i].contenu, 'entrant');
                    }
                    derniereTaille = messages.length;
                    if (enAttente) {
                        enAttente = false;
                        cacherEcrit();
                        desarmerAbsenceReponse();
                    }
                }
            })
            .catch(function () {
                /* erreur silencieuse : le prochain poll reessaiera */
            });
    }

    function envoyer(contenu) {
        contenu = (contenu || '').trim();
        if (!contenu) {
            return;
        }
        var telephone = $('#sim-champ-telephone').value.trim();
        var nom = $('#sim-champ-nom').value.trim() || 'Client Test';
        if (!telephone) {
            ajouterBulle('Renseignez un numero de telephone.', 'systeme');
            return;
        }

        ajouterBulle(contenu, 'sortant');
        enAttente = true;
        afficherEcrit();
        armerAbsenceReponse();

        var entetes = { 'Content-Type': 'application/json' };
        var jeton = csrf();
        if (jeton.token) {
            entetes[jeton.header] = jeton.token;
        }

        fetch(API_MESSAGES, {
            method: 'POST',
            credentials: 'same-origin',
            headers: entetes,
            body: JSON.stringify({
                conversationId: conversationId,
                telephone: telephone,
                nom: nom,
                contenu: contenu
            })
        })
            .then(function (reponse) {
                if (!reponse.ok) {
                    throw new Error('envoi en echec');
                }
            })
            .catch(function () {
                enAttente = false;
                cacherEcrit();
                desarmerAbsenceReponse();
                ajouterBulle("Impossible d'envoyer le message. Verifier CHATWOOT_WEBHOOK_SECRET et les logs.",
                    'systeme');
            });
    }

    function nouvelleConversation() {
        conversationId = genererConversationId();
        derniereTaille = 0;
        enAttente = false;
        cacherEcrit();
        desarmerAbsenceReponse();
        $('#sim-conversation-id').textContent = conversationId;
        var zone = zoneMessages();
        zone.innerHTML = '';
        var vide = document.createElement('p');
        vide.className = 'sim-vide';
        vide.textContent = 'Aucun message pour l\'instant.';
        zone.appendChild(vide);
    }

    document.addEventListener('DOMContentLoaded', function () {
        $('#sim-conversation-id').textContent = conversationId;

        var formulaire = $('#sim-formulaire');
        formulaire.addEventListener('submit', function (evenement) {
            evenement.preventDefault();
            var saisie = $('#sim-saisie');
            envoyer(saisie.value);
            saisie.value = '';
            saisie.focus();
        });

        $('#sim-nouvelle').addEventListener('click', nouvelleConversation);

        $('#sim-exemples').addEventListener('click', function (evenement) {
            var puce = evenement.target.closest('[data-message]');
            if (!puce) {
                return;
            }
            envoyer(puce.getAttribute('data-message'));
        });

        setInterval(poller, DELAI_POLLING_MS);
        poller();
    });
})();

