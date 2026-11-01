# Modèles WhatsApp (templates) à soumettre à Meta

Ces modèles sont **obligatoires** pour envoyer des messages hors de la fenêtre de service
de 24 h. Ils sont de **catégorie `utility`** (transactionnel, non promotionnel) et en
**français**. Les variables sont notées `{{1}}`, `{{2}}`, etc. (positionnelles).

> Soumettre chaque modèle dans **Meta Business Suite → WhatsApp Manager → Modèles de
> message**, en choisissant la catégorie **Utility** et la langue **Français (fr)**.

## 1. `confirmation_commande`

- **Catégorie** : Utility
- **Langue** : Français (`fr`)
- **Texte** : `Bonjour {{1}}, votre commande n°{{2}} a bien été enregistrée pour une livraison le {{3}}. Merci !`
- **Variables** :
  - `{{1}}` : prénom du client
  - `{{2}}` : numéro de commande
  - `{{3}}` : date de livraison souhaitée

## 2. `depart_livraison`

- **Catégorie** : Utility
- **Langue** : Français (`fr`)
- **Texte** : `Bonjour {{1}}, votre livreur est en route. Arrivée estimée : {{2}}.`
- **Variables** :
  - `{{1}}` : prénom du client
  - `{{2}}` : plage horaire d'arrivée (ex. « entre 18h et 18h30 »)

## 3. `livraison_effectuee`

- **Catégorie** : Utility
- **Langue** : Français (`fr`)
- **Texte** : `Bonjour {{1}}, votre commande a été livrée. Merci de votre confiance !`
- **Variables** :
  - `{{1}}` : prénom du client

## 4. `echec_livraison`

- **Catégorie** : Utility
- **Langue** : Français (`fr`)
- **Texte** : `Bonjour {{1}}, nous n'avons pas pu vous livrer ({{2}}). Nous allons vous recontacter.`
- **Variables** :
  - `{{1}}` : prénom du client
  - `{{2}}` : motif (ex. « absent au domicile »)

## Template de test Meta (déjà approuvé, aucun envoi en masse)

- **Nom** : `hello_world`
- **Catégorie** : Utility (fourni par Meta)
- **Langue** : `en_US`
- **Texte** : `Hello World`
- **Variables** : aucune

C'est le **seul** template utilisable sans approbation préalable, utilisé par le test
d'envoi sortant contrôlé (mission : un seul message réel).
