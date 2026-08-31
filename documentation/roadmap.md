# Roadmap

Statuts possibles : `Planifie`, `En cours`, `Termine`.

## 0. Cadrage et structure

- Objectif : creer la structure generale du depot et la documentation initiale.
- Principales taches : creer les dossiers principaux, ajouter les fichiers de cadrage, definir les regles permanentes.
- Resultat attendu : depot organise et pret pour les futures initialisations.
- Criteres de validation : arborescence conforme, aucun projet technique initialise, aucun secret reel enregistre.
- Statut initial : Termine.

## 1. Initialisation du backend Spring Boot

- Objectif : initialiser le backend Java Spring Boot.
- Principales taches : creer le projet Spring Boot, configurer Maven, preparer la configuration par variables d'environnement.
- Resultat attendu : backend minimal demarrable.
- Criteres de validation : compilation Maven reussie et demarrage local possible.
- Statut initial : Termine.

## 2. Authentification et securite

- Objectif : mettre en place l'inscription, la connexion et la securite JWT.
- Principales taches : configurer Spring Security, BCrypt, JWT, roles `USER` et `ADMIN`.
- Resultat attendu : API securisee avec authentification fonctionnelle.
- Criteres de validation : inscription, connexion et acces aux routes protegees verifies.
- Avancement : sous-phase `AppUser`, `Role`, repository et migration Flyway V1 terminee et validee avec H2 et MySQL 8 ; inscription backend avec BCrypt terminee et validee avec H2 et MySQL 8 ; verification des identifiants avec `PasswordEncoder.matches()` terminee et validee avec H2 et MySQL 8 ; generation JWT HS256 terminee et validee avec H2 et MySQL 8 ; authentification Bearer JWT et protection de `GET /api/auth/me` terminees et validees avec H2 et MySQL 8 ; sous-phase 2F de consolidation auth/security et bootstrap ADMIN securise terminee et validee avec H2 et MySQL 8. Le bootstrap ADMIN, le login ADMIN, le JWT, `GET /api/auth/me` et l'idempotence apres redemarrage ont ete verifies sur MySQL 8.
- Statut initial : Termine.

## 3. Profil utilisateur

- Objectif : permettre la gestion du profil et des preferences.
- Principales taches : endpoints profil, devise principale, modification du mot de passe.
- Resultat attendu : utilisateur capable de consulter et modifier ses informations.
- Criteres de validation : isolation des donnees utilisateur et validation des champs.
- Statut initial : Planifie.

## 4. Categories et modes de paiement

- Objectif : gerer les categories et modes de paiement globaux ou personnels.
- Principales taches : CRUD categories personnelles, categories globales admin, modes de paiement globaux.
- Resultat attendu : referentiels metier disponibles pour les transactions.
- Criteres de validation : seuls les admins gerent les valeurs globales, les utilisateurs gerent leurs valeurs personnelles.
- Statut initial : Planifie.

## 5. Transactions

- Objectif : gerer les revenus et depenses.
- Principales taches : CRUD `FinancialTransaction`, filtres par type, categorie, date et montant.
- Resultat attendu : historique financier exploitable.
- Criteres de validation : montant strictement positif, type obligatoire, categorie obligatoire, isolation par utilisateur.
- Statut initial : Planifie.

## 6. Budgets

- Objectif : suivre les budgets mensuels et par categorie.
- Principales taches : CRUD budget, calcul depense/restant, seuils d'alerte.
- Resultat attendu : suivi budgetaire mensuel.
- Criteres de validation : unicite categorie/periode, calculs corrects, alertes generees.
- Statut initial : Planifie.

## 7. Objectifs d'epargne

- Objectif : suivre les objectifs et contributions.
- Principales taches : CRUD objectifs, ajout de contributions, progression, fermeture automatique.
- Resultat attendu : objectifs d'epargne suivis dans l'application.
- Criteres de validation : contribution non negative, objectif termine lorsque la cible est atteinte.
- Statut initial : Planifie.

## 8. Dashboard utilisateur

- Objectif : afficher les indicateurs financiers principaux.
- Principales taches : solde, revenus du mois, depenses du mois, epargne, graphiques, dernieres transactions.
- Resultat attendu : vue synthetique des finances personnelles.
- Criteres de validation : indicateurs coherents avec les donnees en base.
- Statut initial : Planifie.

## 9. Administration

- Objectif : fournir un espace admin securise.
- Principales taches : liste utilisateurs, recherche, blocage/deblocage, categories globales, statistiques, journal admin.
- Resultat attendu : supervision de la plateforme.
- Criteres de validation : admin sans acces aux mots de passe ni modification des transactions privees.
- Statut initial : Planifie.

## 10. Notifications et recommandations statistiques

- Objectif : generer des alertes et recommandations simples.
- Principales taches : alertes budget, alertes objectifs, depenses inhabituelles par regles statistiques.
- Resultat attendu : premieres fonctionnalites intelligentes sans modele ML avance.
- Criteres de validation : recommandations seulement avec historique suffisant.
- Statut initial : Planifie.

## 11. Export CSV

- Objectif : exporter les transactions.
- Principales taches : export CSV avec filtres et controle d'acces.
- Resultat attendu : fichier CSV exploitable.
- Criteres de validation : donnees limitees a l'utilisateur connecte.
- Statut initial : Planifie.

## 12. Initialisation du frontend Angular

- Objectif : initialiser l'application Angular.
- Principales taches : creation du projet, structure core/shared/features, Angular Material, configuration de base.
- Resultat attendu : frontend demarrable.
- Criteres de validation : installation reussie et application locale accessible.
- Statut initial : Planifie.

## 13. Authentification frontend

- Objectif : connecter l'interface aux endpoints d'authentification.
- Principales taches : pages login/register, stockage token, guards, interceptor JWT.
- Resultat attendu : parcours d'authentification complet.
- Criteres de validation : connexion, deconnexion et protection des routes fonctionnelles.
- Statut initial : Planifie.

## 14. Interfaces utilisateur

- Objectif : developper les vues utilisateur.
- Principales taches : dashboard, transactions, budgets, objectifs, profil, notifications.
- Resultat attendu : espace utilisateur utilisable.
- Criteres de validation : workflows principaux testables depuis l'interface.
- Statut initial : Planifie.

## 15. Interfaces administrateur

- Objectif : developper les vues admin.
- Principales taches : utilisateurs, categories globales, modes de paiement, statistiques, journal.
- Resultat attendu : espace admin utilisable.
- Criteres de validation : routes admin protegees et actions controlees.
- Statut initial : Planifie.

## 16. Integration frontend/backend

- Objectif : stabiliser les echanges entre Angular et Spring Boot.
- Principales taches : services Angular, DTO coherents, gestion d'erreurs, tests manuels.
- Resultat attendu : application web connectee au backend.
- Criteres de validation : principaux parcours fonctionnels de bout en bout.
- Statut initial : Planifie.

## 17. Module Machine Learning Python

- Objectif : initialiser le module Python avance.
- Principales taches : structure FastAPI, preparation des donnees, premieres fonctions d'analyse.
- Resultat attendu : service ML minimal pret pour integration.
- Criteres de validation : API FastAPI demarrable avec endpoints de test.
- Statut initial : Planifie.

## 18. Integration Spring Boot/FastAPI

- Objectif : connecter le backend au service ML.
- Principales taches : client REST, appels prediction/anomalies/recommandations, gestion indisponibilite service.
- Resultat attendu : backend capable d'exploiter les resultats ML.
- Criteres de validation : integration testee avec service ML local.
- Statut initial : Planifie.

## 19. Tests, securite et optimisation

- Objectif : renforcer la qualite globale.
- Principales taches : tests unitaires/integration, verification securite, optimisation requetes et performances.
- Resultat attendu : application plus fiable et robuste.
- Criteres de validation : tests verts et points de securite critiques verifies.
- Statut initial : Planifie.

## 20. Documentation finale et preparation de la demonstration

- Objectif : preparer la livraison PFA.
- Principales taches : README final, guide installation, scenarios de demonstration, limites et perspectives.
- Resultat attendu : projet pret a presenter.
- Criteres de validation : documentation claire et demonstration reproductible.
- Statut initial : Planifie.
