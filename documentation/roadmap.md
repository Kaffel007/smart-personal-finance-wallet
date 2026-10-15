# Roadmap

Statuts : `Planifie`, `En cours`, `Termine`, `Non commence`.

## 0. Cadrage et structure — Termine

Structure du depot, documentation initiale et regles de travail.

## 1. Initialisation backend — Termine

Spring Boot, Java 21, Maven, MySQL 8 et Flyway. Configuration par variables d'environnement.

## 2. Authentification et securite backend — Termine

AppUser, roles USER/ADMIN, BCrypt, inscription, login JWT, Bearer et GET /api/auth/me.
Securite stateless, isolation utilisateur et bootstrap ADMIN securise.
Validation H2 et MySQL 8 effectuee lors des phases backend.

## 3. Modules backend MVP — Termine

| Sous-phase | Perimetre | Statut |
| --- | --- | --- |
| 3A Categories | CRUD personnel, type, unicite normalisee, isolation utilisateur, Flyway V2 | Termine |
| 3B Transactions | CRUD FinancialTransaction, filtres, BigDecimal, isolation, Flyway V3 | Termine |
| 3C Budgets | CRUD mensuel, categories EXPENSE, progression dynamique, Flyway V4 | Termine |
| 3D Savings Goals | CRUD, montants, date cible, progression dynamique, Flyway V5 | Termine |
| 3E Dashboard | Synthese mensuelle, agregations dynamiques, isolation utilisateur | Termine |

Ces modules ont ete valides sur H2 et MySQL 8 lors des phases precedentes.

## 4. Frontend Angular — En cours

### 4A Fondation et authentification — Terminee

- Angular 22.2.1 standalone, Router, HttpClient, Reactive Forms et RxJS.
- URL API centralisee : http://localhost:8080/api.
- Login/register, JWT localStorage, utilisateur courant, interceptor Bearer et guard.
- Layout responsive, logout et dashboard provisoire protege.
- Build production reussi ; 16 tests automatises reussis.
- Validation navigateur reelle et CORS dev terminee, confirmee par l'utilisateur. Le backend autorise localhost:4200 avec le profil dev.
- Aucun changement backend, aucune migration, aucun appel aux statistiques dashboard.

### 4B Dashboard — Terminee

- DashboardSummary et DashboardService relies a GET /api/dashboard/summary?year=YYYY&month=MM.
- Periode courante du navigateur, selection mois/annee, limites 2000..2100.
- Revenus, depenses, solde et transactions ; budgets mensuels et epargne globale.
- Progression fournie par le backend, depassements visibles, barres plafonnees a 100 %.
- Format francais avec suffixe neutre centralise : aucune devise effective definie.
- Responsive, accessibilite, loading, erreur/retry et etats sans donnees.
- Build production reussi ; 29 tests Angular reussis, dont 13 nouveaux tests dashboard.
- Validation navigateur reelle et correction de la synchronisation mois/annee terminees, confirmees par l'utilisateur.
- Backend, migrations, MySQL et dependances inchanges.

### 4C Transactions et categories — Terminee

- Routes privees /transactions et /categories, navigation et AuthGuard.
- CategoryService : CRUD et filtre type ; modeles conformes au backend.
- Categories : revenus/depenses, creation, renommage/type, suppression confirmee.
- Gestion claire des doublons 409, categories utilisees 409 et erreurs 404/general.
- TransactionService : CRUD, filtres type/categoryId combinables et reset.
- Formulaire reactif : categorie filtree par type visuel, montant positif, date non future, description.
- Payload sans userId ni type ; type derive par le backend uniquement.
- Creation/modification/suppression confirmee, feedback et actualisation des listes.
- Styles responsive, chargements, erreurs, etats vides et accessibilite.
- Aucune dependance ajoutee ; JWT/interceptor existants reutilises.
- Validation : 88 tests Angular reussis et build de production reussi.
- Validation navigateur reelle terminee, confirmee par l'utilisateur ; documentation, commit et push confirmes.
- Backend, migrations, MySQL et CORS inchanges.

### 4D Budgets UI — Terminee

- Route privee /budgets et lien MainLayout ; objectifs d'epargne accessibles depuis 4E.
- Modeles conformes aux DTO backend ; BudgetService CRUD et filtres year/month/categoryId.
- Periode courante du navigateur, mois francais, selection annee selon la convention Dashboard.
- Categories EXPENSE uniquement ; lien /categories si aucune categorie de depense.
- Formulaire reactif, validations backend, creation/modification et suppression confirmee.
- Apres enregistrement : periode du budget selectionnee, filtre categorie efface, liste rechargee.
- Montants et progression du backend ; barres plafonnees, pourcentage reel et restant negatif visibles.
- Doublon 409, erreurs metier/generiques, retry, feedback, chargement et blocage des doubles soumissions.
- Cards responsive, labels, focus visible, aides de validation et barres ARIA.
- Validation : 134 tests Angular reussis (46 nouveaux), build production et git diff --check reussis.
- Validation navigateur reelle terminee ; documentation, commit et push confirmes par l'utilisateur.
- Backend, logique Dashboard, migrations, MySQL, CORS et dependances inchanges.

### 4E Savings Goals UI — Implementee et testee Angular

- Route privee /savings-goals, AuthGuard, lien actif MainLayout sans badge A venir.
- Modeles conformes aux DTO backend ; SavingsGoalService CRUD /api/savings-goals.
- Payloads limites a name/targetAmount/savedAmount/targetDate ; JWT via interceptor.
- Formulaire reactif : nom non blanc de 120 caracteres maximum, cible positive, epargne positive ou nulle.
- Limites backend de 15 chiffres entiers et 4 decimales ; epargne initiale zero, facultative en creation, requise en modification.
- Date cible facultative, passee autorisee, affichage francais ou Aucune date cible.
- Creation/modification, suppression confirmee, actualisation backend et feedback.
- Progression backend, Objectif atteint des 100 %, barre plafonnee et restant negatif visible.
- Chargements, doubles soumissions bloquees, erreurs locales, retry et etat vide.
- Cards et formulaire responsive, labels, focus visible et progression ARIA.
- Validation : 181 tests Angular reussis (47 nouveaux), build production et git diff --check reussis.
- Validation navigateur reelle de 4E restante.
- Backend, Dashboard, migrations, MySQL, CORS et dependances inchanges.
- Aucun SavingsContribution, liaison automatique aux transactions, Admin, ML ou notification ajoute.
- Aucun commit ni push pour 4E ; aucune phase suivante commencee.

## Phases futures — Planifiees

- Referentiels globaux et modes de paiement : gestion admin et restrictions de roles.
- Contributions aux objectifs d'epargne et evolutions des modules metier.
- Administration : utilisateurs, blocage/deblocage, categories globales, parametres et journal.
  Aucun acces aux mots de passe ni modification des transactions privees par un administrateur.
- Notifications et recommandations statistiques : alertes budgets et anomalies par regles.
- Export CSV : donnees strictement limitees a l'utilisateur authentifie.
- Interfaces administrateur : routes protegees et parcours de supervision.
- Machine Learning Python/FastAPI : analyse, prediction et integration Spring Boot dans une phase avancee.
- Tests, securite et optimisation : parcours de bout en bout, isolation et performances.
- Documentation finale et preparation de la demonstration PFA.
- Flutter : perspective future uniquement.

La phase 4E s'arrete ici, implementee et testee Angular ; validation navigateur reelle restante.
Aucun commit ni push pour 4E ; aucune phase suivante commencee.
