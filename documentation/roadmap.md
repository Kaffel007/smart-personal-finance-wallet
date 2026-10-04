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

### 4B Dashboard — Implementee et testee Angular

- DashboardSummary et DashboardService relies a GET /api/dashboard/summary?year=YYYY&month=MM.
- Periode courante du navigateur, selection mois/annee, limites 2000..2100.
- Revenus, depenses, solde et transactions ; budgets mensuels et epargne globale.
- Progression fournie par le backend, depassements visibles, barres plafonnees a 100 %.
- Format francais avec suffixe neutre centralise : aucune devise effective definie.
- Responsive, accessibilite, loading, erreur/retry et etats sans donnees.
- Build production reussi ; 29 tests Angular reussis, dont 13 nouveaux tests dashboard.
- Validation navigateur reelle du dashboard encore a faire.
- Backend, migrations, MySQL et dependances inchanges.

### Interfaces metier — Non commencees

Categories, transactions, budgets et objectifs d'epargne : services, formulaires, listes et filtres.
Integration complete frontend/backend a valider progressivement, notamment les parcours reels et CORS.

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

La phase 4B s'arrete ici. Aucun commit ni push pour cette phase ; aucune phase 4C commencee.
