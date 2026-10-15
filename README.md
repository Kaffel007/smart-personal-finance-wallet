# Smart Personal Finance Wallet

## Description

Smart Personal Finance Wallet est une application web intelligente de gestion des finances personnelles. Elle permettra aux utilisateurs de suivre leurs revenus, leurs depenses, leurs budgets et leurs objectifs d'epargne, puis d'obtenir des alertes et recommandations financieres personnalisees.

Le projet est developpe progressivement dans le cadre du PFA. La premiere version est une application web Angular avec un backend Spring Boot. Le Machine Learning fait partie du projet et sera developpe dans une phase avancee, apres les fonctionnalites principales. Flutter est uniquement une perspective future.

## Objectifs

- Centraliser la gestion des revenus et depenses.
- Suivre les budgets mensuels et par categorie.
- Suivre les objectifs d'epargne.
- Visualiser les indicateurs financiers dans un dashboard.
- Alerter sur les budgets proches du depassement.
- Detecter progressivement les depenses inhabituelles.
- Fournir des recommandations financieres personnalisees.

## Acteurs

- `USER` : gere ses informations financieres personnelles.
- `ADMIN` : gere les comptes utilisateurs, bloque/debloque les comptes USER et consulte les statistiques de comptes de la plateforme. Il n'accede pas aux finances privees des autres utilisateurs.

## Fonctionnalites principales prevues

- Authentification, profil et preferences utilisateur.
- Gestion des categories globales et personnelles.
- Gestion des modes de paiement.
- Gestion des revenus et depenses avec recherche et filtres.
- Gestion des budgets.
- Gestion des objectifs d'epargne et contributions.
- Dashboard utilisateur avec graphiques et indicateurs.
- Notifications internes.
- Administration des utilisateurs, categories globales et parametres.
- Export CSV des transactions.
- Recommandations statistiques, puis module Machine Learning Python.

## Technologies prevues

- Frontend : Angular 22.2.1 standalone, TypeScript, HTML et CSS. Aucune bibliotheque UI ajoutee en phase 4A.
- Graphiques : Chart.js avec ng2-charts.
- Backend : Java 21, Spring Boot 3, API REST, Spring Data JPA, Spring Security, JWT, BCrypt, Maven.
- Base de donnees : MySQL 8, base `smart_finance_wallet`, utilisateur technique `finance_app`.
- Migrations : Flyway.
- Machine Learning : Python et FastAPI dans une phase avancee.

## Structure du depot

```text
smart-personal-finance-wallet/
├── backend/
├── frontend/
├── machine-learning/
├── documentation/
│   ├── roadmap.md
│   ├── architecture-decisions.md
│   └── domain-model-draft.md
├── .env.example
├── .gitignore
├── AGENTS.md
└── README.md
```

## Methode de developpement

Le projet sera developpe par petites phases validees une par une. Pour chaque phase, l'existant sera examine, les modifications seront annoncees, les fichiers importants seront expliques, les tests adaptes seront executes lorsque du code sera ajoute, puis l'etape sera arretee en attente de validation.

Les secrets reels ne doivent jamais etre enregistres dans le depot. Les donnees financieres doivent etre protegees cote backend et chaque utilisateur doit acceder uniquement a ses propres donnees.

## Suivi du projet

| Phase | Statut |
| --- | --- |
| 0. Cadrage et structure | Termine |
| 1. Initialisation backend | Termine |
| 2. Authentification et securite backend | Termine |
| 3A Categories / 3B Transactions / 3C Budgets | Termines |
| 3D Savings Goals / 3E Dashboard backend | Termines |
| 4. Frontend Angular utilisateur | Termine (MVP utilisateur confirme par l'utilisateur) |
| 4A Fondation et authentification | Terminee et validee navigateur |
| 4B Dashboard frontend | Terminee et validee navigateur |
| 4C Transactions et categories frontend | Terminee et validee navigateur |
| 4D Budgets UI | Terminee et validee navigateur |
| 4E Savings Goals UI | Terminee (MVP utilisateur confirme par l'utilisateur) |
| 5. Administration | En cours |
| 5A Backend Admin MVP | Implemente/teste automatiquement sur H2 |
| 5B Frontend Admin | Non commence |
| Machine Learning | Non commence ; prevu apres administration |
| Notifications et export | Planifies |

La feuille de route detaillee est disponible dans [documentation/roadmap.md](documentation/roadmap.md).

## Etat actuel

Le MVP utilisateur (backend et frontend des phases 1 a 4E) est confirme termine par l'utilisateur. La phase 5A Backend Admin est implementee et testee automatiquement sur H2 ; le frontend Admin et le ML ne sont pas commences. Le backend Spring Boot 3.5.16 est initialise avec Java 21.0.11. Le modele persistant `AppUser`, l'enum `Role`, le repository utilisateur et la premiere migration Flyway sont implementes. La migration V1 et la table `app_users` ont ete validees avec H2 en memoire et MySQL 8.

La connexion locale a MySQL avec le compte technique `finance_app`, l'application de la migration V1 par Flyway et la validation du schema par Hibernate ont ete verifiees. L'endpoint `/actuator/health` retourne `UP`. L'endpoint `/actuator/info` est disponible, mais aucune information personnalisee n'est encore configuree.

L'endpoint backend `POST /api/auth/register` est implemente et valide avec H2 et MySQL 8. Il valide et normalise les donnees, attribue obligatoirement le role `USER` et chiffre les mots de passe avec BCrypt avant leur enregistrement. Le stockage du hash BCrypt a ete valide sur la base MySQL reelle et la reponse HTTP ne contient aucune donnee sensible.

La verification des identifiants par `POST /api/auth/login` et la generation du JWT apres une connexion reussie sont validees avec H2 et MySQL 8. La validation sur la base MySQL reelle confirme la normalisation de l'adresse e-mail, la verification du hash BCrypt avec `PasswordEncoder.matches()` et la generation d'un token signe avec HS256. Une connexion valide retourne le token avec le resume public de l'utilisateur, tandis qu'un mauvais mot de passe retourne HTTP 401.

Le JWT contient uniquement les claims `sub`, `iat` et `exp` ; son subject contient l'identifiant utilisateur. Sa duree est configurable par `JWT_EXPIRATION_MINUTES` et vaut 60 minutes, soit 3600 secondes, par defaut. La cle de signature doit etre fournie en Base64 par la variable d'environnement obligatoire `JWT_SECRET_BASE64` et aucun secret n'est versionne. Aucun refresh token n'est genere.

L'authentification Bearer JWT et `GET /api/auth/me` sont implementes et valides avec H2 et MySQL 8. `POST /api/auth/register` et `POST /api/auth/login` restent publics, tandis que `GET /api/auth/me` exige un Bearer JWT valide et retourne uniquement l'utilisateur authentifie. L'API est stateless : le `SecurityFilterChain` desactive les sessions, CSRF, form login et HTTP Basic, puis place `JwtAuthenticationFilter` avant le filtre username/password. Aucun refresh token ni session HTTP n'est utilise.

Pour chaque requete Bearer, la signature, l'expiration et le subject sont verifies, puis `AppUser` est recharge depuis la base. La validation MySQL 8 confirme qu'un passage a `blocked=true` retire immediatement l'acces avec HTTP 401, meme lorsque le JWT a ete emis avant le blocage et reste cryptographiquement valide. Les utilisateurs supprimes ou desactives sont egalement refuses. Le role courant vient uniquement de la base et devient `ROLE_USER` ou `ROLE_ADMIN` dans le `SecurityContext` ; aucun role n'est stocke dans le JWT. Le frontend Angular est maintenant initialise ; aucun projet Python n'est initialise.

La phase Authentification et securite est terminee et validee avec H2 et MySQL 8. La chaine `register` -> `login` -> JWT -> Bearer -> `GET /api/auth/me` est operationnelle avec Spring Security stateless pour les roles `USER` et `ADMIN`. L'inscription publique cree toujours uniquement un `USER`. Le JWT HS256 reste minimal avec les claims `sub`, `iat` et `exp` ; le role est absent du token et recharge depuis la base a chaque requete protegee. Un utilisateur bloque, desactive ou supprime est donc refuse avec un ancien JWT encore valide. Aucun refresh token ni session HTTP n'est utilise.

Le premier compte `ADMIN` peut etre cree au demarrage uniquement par un bootstrap optionnel, idempotent et desactive par defaut avec `ADMIN_BOOTSTRAP_ENABLED=false`. Lorsqu'il est active, l'e-mail, le mot de passe, le prenom et le nom proviennent exclusivement des variables `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD`, `ADMIN_BOOTSTRAP_FIRST_NAME` et `ADMIN_BOOTSTRAP_LAST_NAME`. Le mot de passe suit exactement la politique de l'inscription et est enregistre uniquement sous forme de hash BCrypt. Le bootstrap, sa validation, son idempotence, le login ADMIN, le JWT et `GET /api/auth/me` ont ete valides avec H2 et MySQL 8. Un ADMIN deja present n'est pas modifie et un `USER` utilisant le meme e-mail n'est jamais promu automatiquement. Aucun ADMIN n'est insere par Flyway et aucun mot de passe ou secret n'est versionne.

La phase 3 est en cours et la sous-phase 3A consacree aux categories financieres personnelles est terminee et validee avec H2 et MySQL 8. Le module `Category` prend en charge les types `INCOME` et `EXPENSE` avec le CRUD protege complet : `POST`, `GET`, `GET /{id}`, `PUT /{id}` et `DELETE /{id}` sous `/api/categories`. La liste peut etre filtree par type. Chaque categorie appartient obligatoirement a l'utilisateur authentifie par JWT, y compris lorsqu'il possede le role `ADMIN`.

Le nom est nettoye et une valeur interne `normalizedName` en minuscules garantit l'unicite `(user_id, type, normalized_name)` dans le service et la base. Cette valeur et le proprietaire ne sont jamais exposes dans les DTO publics. L'isolation est stricte : les tentatives `GET`, `PUT` ou `DELETE` sur la categorie d'un autre utilisateur retournent HTTP 404 sans reveler son existence. Aucune categorie par defaut n'est creee.

La sous-phase 3B et le module `FinancialTransaction` sont termines et valides avec H2 et MySQL 8 pour les revenus et depenses personnels. Le CRUD protege complet est disponible sous `/api/transactions` avec `POST`, `GET`, `GET /{id}`, `PUT /{id}` et `DELETE /{id}`. La liste est triee par `transactionDate` decroissante puis par identifiant decroissant et accepte les filtres optionnels `type` et `categoryId`, seuls ou combines. Les montants utilisent `BigDecimal` et une colonne `DECIMAL(19,4)` ; ils doivent etre strictement positifs. La date est obligatoire, aujourd'hui est accepte et une date future est refusee. La description optionnelle est nettoyee et limitee a 255 caracteres.

Chaque transaction appartient obligatoirement a l'utilisateur authentifie et a l'une de ses categories. Le client ne fournit jamais le proprietaire et le champ `type` n'est pas une source metier cliente : il est derive de `Category.type` a chaque creation ou changement de categorie, puis stocke pour conserver l'historique. Un acces direct cross-user retourne HTTP 404, et utiliser la categorie d'un autre utilisateur retourne egalement HTTP 404. Le role `ADMIN` reste limite a ses propres transactions. Une categorie referencee ne peut ni etre supprimee ni changer de type ; ces operations retournent HTTP 409, tandis que son renommage reste autorise.

La migration Flyway V3 cree `financial_transactions`, ses cles et ses index sans aucune donnee initiale. Le module Transactions et les migrations V1, V2 et V3 sont valides avec H2 et MySQL 8. Aucun `PaymentMethod`, `SavingsGoal`, calcul de solde global ou dashboard n'est encore implemente.

La sous-phase 3C et le module des budgets mensuels personnels sont termines et valides avec H2 et MySQL 8. Le CRUD protege complet est disponible sous `/api/budgets` avec des filtres optionnels par `year`, `month` et `categoryId`, seuls ou combines. Un budget appartient a l'utilisateur authentifie et a l'une de ses categories `EXPENSE` ; une categorie `INCOME` est refusee en HTTP 400. Son montant utilise `BigDecimal` et `DECIMAL(19,4)`, doit etre strictement positif, et l'unicite `(user_id, category_id, budget_year, budget_month)` est garantie dans le service et par la migration Flyway V4.

Chaque reponse Budget calcule dynamiquement `spent` par une requete `SUM` sur les transactions `EXPENSE` du proprietaire, de la categorie et du mois concernes ; une transaction hors periode est ignoree. `remaining` vaut `amount - spent` et peut devenir negatif. `usagePercent` est calcule a deux decimales et peut depasser 100 ; ces trois valeurs ne sont jamais stockees dans la table `budgets`. Les acces cross-user retournent HTTP 404 et un `ADMIN` reste limite a ses propres budgets. Une categorie utilisee par une transaction ou un budget ne peut pas etre supprimee ni changer de type et retourne HTTP 409 ; son renommage reste autorise.

Le module Budget et les migrations V1, V2, V3 et V4 sont valides avec H2 et MySQL 8.

La sous-phase 3D et le module `SavingsGoal` sont termines et valides avec H2 et MySQL 8. Le CRUD protege complet est disponible sous `/api/savings-goals` avec `POST`, `GET`, `GET /{id}`, `PUT /{id}` et `DELETE /{id}`. Chaque objectif appartient uniquement a l'utilisateur authentifie par JWT, y compris pour un `ADMIN` ; aucun identifiant utilisateur n'est accepte dans les requetes et un acces cross-user retourne HTTP 404.

Les montants cible et epargne utilisent `BigDecimal` et `DECIMAL(19,4)`. `targetAmount` doit etre strictement positif, `savedAmount` doit etre positif ou nul et vaut zero a la creation lorsqu'il est omis. La date cible `targetDate` est optionnelle et informative. `remainingAmount = targetAmount - savedAmount` et `progressPercent = (savedAmount / targetAmount) * 100` sont calcules dynamiquement, jamais stockes, avec un pourcentage arrondi a deux decimales. Un objectif peut depasser sa cible : le restant devient negatif et la progression peut depasser 100 %.

La migration Flyway V5 cree uniquement la table `savings_goals`, ses contraintes et son index utilisateur, sans donnees initiales. Les migrations V1 a V5, le repository, le service et les endpoints HTTP avec JWT reel sont valides sur H2 et MySQL 8. Aucun `remainingAmount` ni `progressPercent` n'est persiste. Aucun `SavingsContribution`, lien automatique avec les transactions, dashboard, notification, frontend ou module Machine Learning n'est implemente dans cette sous-phase.

La sous-phase 3E Dashboard & statistiques est terminee et validee avec H2 et MySQL 8. Elle fournit un dashboard personnel mensuel calcule dynamiquement par `GET /api/dashboard/summary?year=2026&month=9`. L'annee est obligatoire et comprise entre 2000 et 2100 ; le mois est obligatoire et compris entre 1 et 12. Des parametres absents ou invalides retournent HTTP 400, tandis qu'une requete sans JWT retourne HTTP 401. L'identifiant utilisateur provient exclusivement du JWT, et un `ADMIN` reste limite a ses propres statistiques.

La synthese des transactions expose `totalIncome`, `totalExpense`, `balance` et `transactionCount` pour le mois demande. La synthese des budgets expose `totalBudget`, `budgetSpent`, `budgetRemaining`, `budgetUsagePercent` et `budgetCount` ; seules les depenses des categories budgetees sont comptees, sans double comptage. La synthese globale des objectifs expose `totalSavingsTarget`, `totalSavingsSaved`, `savingsRemaining`, `savingsProgressPercent` et `savingsGoalCount`. Tous les montants utilisent `BigDecimal`, les pourcentages sont calcules a deux decimales, et les restants negatifs ainsi que les progressions superieures a 100 % sont autorises.

Le dashboard ne possede aucune entite ni table : ses donnees proviennent de calculs dynamiques et d'agregations SQL/JPA sur les transactions, budgets et objectifs d'epargne existants. L'absence de donnees retourne des montants, comptes et pourcentages a zero. Le module, son endpoint JWT, ses validations et son isolation multi-utilisateur sont valides avec H2 et MySQL 8. Le test MySQL 8 a confirme la synthese complete avec HTTP 200 et l'isolation stricte : les donnees d'un utilisateur restent invisibles aux autres. Aucune migration V6, dependance Maven, interface frontend, fonctionnalite ML ou notification n'a ete ajoutee.

## Frontend — Phase 4A

La fondation Angular 22.2.1 standalone et l'authentification sont implementees.
Les routes publiques `/login` et `/register` utilisent des formulaires reactifs.
Le login appelle `/api/auth/login`, stocke uniquement `accessToken` dans
localStorage sous `smart_finance_access_token`, puis recupere le profil avec
`/api/auth/me`. L'inscription appelle `/api/auth/register` puis redirige vers login.
Aucun mot de passe ni secret JWT n'est stocke dans le frontend.

L'interceptor ajoute Authorization Bearer uniquement aux endpoints proteges de
l'API configuree et supprime la session en cas de reponse 401 protegee. Le guard
protege `/dashboard` ; le backend reste responsable de l'autorisation et de
l'isolation des donnees. Le layout responsive contient la navigation et logout.
Transactions, categories, budgets et objectifs d'epargne sont accessibles. Le dashboard est
maintenant connecte aux statistiques en phase 4B.
La racine et les routes inconnues redirigent vers dashboard si un token existe,
sinon vers login. Le profil est recharge apres un rafraichissement.

```powershell
cd frontend
npm ci
npm start
```

URL frontend : http://localhost:4200. Backend attendu : http://localhost:8080.
L'URL API http://localhost:8080/api est centralisee dans
`frontend/src/app/core/api.config.ts`.

La phase 4A et le CORS dev ont ete valides dans le navigateur par l'utilisateur.
Le backend attendu sur localhost:8080 doit utiliser le profil Spring dev pour
autoriser l'origine http://localhost:4200.

Details : [frontend/README.md](frontend/README.md). Transactions et categories sont terminees en 4C ; budgets UI termines en 4D ; objectifs UI implementes en 4E.

## Frontend — Phase 4B Dashboard

Le vrai dashboard Angular appelle GET /api/dashboard/summary avec year et month.
Il selectionne initialement la periode courante du navigateur, puis propose les
mois francais et une plage d'annees autour de l'annee courante (limites 2000..2100).
Un changement recharge les donnees et annule une requete precedente obsolete.

Les quatre cartes affichent revenus, depenses, solde et nombre de transactions.
Les sections affichent les budgets mensuels et les objectifs d'epargne globaux,
avec montants, compteurs et pourcentages fournis directement par le backend.
Les barres sont plafonnees visuellement a 100 %, tandis que les pourcentages reels
et les restants negatifs sont conserves. Les depassements sont signales.
Aucune devise effective n'est definie : format francais et suffixe neutre
« unités » centralise dans le pipe FinancialAmountPipe.

Le dashboard reste responsive (4/2/1 cartes selon la largeur), avec labels,
progress bars accessibles, chargement, erreur generique et bouton Reessayer.
Sans donnees, les cartes a zero restent visibles avec les messages appropries.
Aucune librairie graphique ni dependance n'a ete ajoutee.

Validation : npm test -- --watch=false : 29 tests reussis ; npm run build reussi.
La validation navigateur reelle du dashboard 4B et la synchronisation mois/annee sont confirmees par l'utilisateur.
Aucun fichier backend, migration ou MySQL modifie pendant cette phase.
La phase 4B est terminee ; les interfaces transactions/categories sont decrites ci-dessous.

## Frontend — Phase 4C Transactions et categories

Le frontend permet maintenant de saisir revenus et depenses sans Postman.
Les routes privees /categories et /transactions sont protegees par AuthGuard et
integrees au layout. Les services utilisent l'URL API centralisee et le JWT ajoute
par l'interceptor existant. Le backend reste responsable de l'isolation utilisateur.

Categories : listes revenus/depenses, creation, modification nom/type et suppression
avec confirmation inline. Nom requis, limite de 100 caracteres, messages clairs pour
les doublons et categories utilisees (409), les 404 et les erreurs generales.
Le renommage reste possible selon les regles backend.

Transactions : liste dans l'ordre backend, filtres combinables type/categorie et
reinitialisation. Creation et modification via Reactive Forms : type visuel pour
filtrer les categories, categorie requise, montant positif avec au plus 4 decimales,
date non future, description optionnelle (255 caracteres maximum). Aucun type ni
userId envoye : le backend derive le type depuis Category. Une categorie manquante
propose un lien vers sa creation. La suppression exige une confirmation explicite.
Les montants utilisent le pipe commun avec suffixe neutre « unités » ; +/− restent
uniquement visuels. Un succes ferme le formulaire et actualise la liste avec feedback.

Pages responsive avec cartes lisibles sur mobile, labels, focus visible, messages
accessibles, chargements, actions desactivees pendant mutations, erreurs/retry et
etats vides. Budgets accessibles depuis 4D ; objectifs accessibles depuis 4E. Aucune dependance ajoutee.

Validation : npm test -- --watch=false : 88 tests reussis ; npm run build reussi.
La validation navigateur reelle de 4C est confirmee par l'utilisateur ; cette phase
est documentee, commitee et poussee selon sa confirmation. Aucun fichier backend,
migration, MySQL ou CORS modifie pendant cette phase frontend.

## Frontend — Phase 4D Budgets UI

La route privee `/budgets`, protegee par AuthGuard, est accessible depuis MainLayout.
La page propose une grille responsive, la periode courante puis des filtres mois,
annee et categorie de depense, combines dans GET /api/budgets.

BudgetService couvre GET liste/detail, POST, PUT et DELETE. Les DTO suivent les
contrats backend et n'envoient que categoryId, year, month et amount.
Le formulaire reactif utilise exclusivement les categories EXPENSE et valide
annee 2000..2100, mois 1..12 et montant positif avec au plus quatre decimales.
La suppression exige une confirmation explicite ; les doublons HTTP 409 ont un
message metier. Chaque mutation recharge la liste et annonce son succes.
Apres creation/modification, la periode du budget enregistre est selectionnee et
le filtre categorie efface pour afficher le budget concerne.

Les cartes affichent montant prevu, depense, restant et utilisation fournis par
le backend. FinancialAmountPipe conserve le suffixe « unités ». Les barres CSS et
ARIA sont bornees a 0..100 ; le texte et aria-valuetext conservent le pourcentage
reel. Un depassement affiche « Budget dépassé » et le restant negatif est conserve.
Chargements, etats vides, erreurs/retry, labels et focus visible sont pris en charge.

Validation : 134 tests Angular reussis (46 nouveaux), build de production reussi
et git diff --check reussi. Aucun backend, migration, MySQL, CORS ou dependance
modifie ; logique Dashboard inchangee. Validation navigateur reelle de 4D terminee,
documentation, commit et push confirmes par l'utilisateur.

## Frontend — Phase 4E Objectifs d'epargne UI

La route privee `/savings-goals`, protegee par AuthGuard, est accessible dans
MainLayout. SavingsGoalService utilise le CRUD `/api/savings-goals`, avec le JWT
ajoute uniquement par l'interceptor existant. Les payloads contiennent seulement
name, targetAmount, savedAmount et targetDate, sans champ calcule ni userId.

Le formulaire reactif permet de creer et modifier un objectif : nom non blanc
(120 caracteres maximum), cible strictement positive et epargne positive ou nulle,
avec les limites backend de 15 chiffres entiers et 4 decimales. L'epargne est
initialisee a zero, facultative en creation (vide envoye comme zero) et requise
en modification. La date cible est facultative, accepte une date passee et est
affichee au format dd/MM/yyyy ; sans date, « Aucune date cible » est affiche.

Les cartes affichent cible, epargne, restant et progression issus du backend,
avec FinancialAmountPipe et le suffixe « unités ». La barre CSS et aria-valuenow
sont bornees a 0..100, tandis que texte et aria-valuetext conservent la progression
reelle, y compris au-dela de 100 %. « Objectif atteint » apparait des 100 % ; le
restant negatif reste visible et le style signale la reussite.

Creation, modification et suppression confirmee rechargent la liste avec feedback.
Chargements, doubles soumissions bloquees, erreurs locales, retry, etat vide,
labels, focus visible et progression ARIA sont geres. Grille responsive desktop,
deux colonnes tablette et une mobile. La logique Dashboard reste inchangee.

Validation : 181 tests Angular reussis (47 nouveaux), npm run build et
git diff --check reussis. Le MVP utilisateur incluant 4E est confirme termine par l'utilisateur.
Aucune dependance ajoutee ; backend, migrations, MySQL et CORS inchanges.
Aucun SavingsContribution, liaison automatique aux transactions, Admin, ML ou
notification ajoute dans cette sous-phase frontend.

## Backend — Phase 5A Admin MVP

Les API Admin gerent uniquement les comptes. Toutes les routes `/api/admin/**`
exigent `ROLE_ADMIN` : absence d'authentification = HTTP 401, compte USER = HTTP 403.
La regle est appliquee par SecurityConfig avant la regle generale `/api/**`.
Les JWT restent controles par le filtre existant, avec relecture du compte et de
son role en base a chaque requete. Le bootstrap administrateur existant reste inchange.

| Methode | Endpoint | Comportement |
| --- | --- | --- |
| GET | `/api/admin/users` | Liste des comptes par id decroissant |
| GET | `/api/admin/users?search=Sara` | Recherche partielle sur nom complet ou e-mail, insensible a la casse ; espaces externes retires |
| GET | `/api/admin/users/{id}` | Detail du compte ; HTTP 404 si absent |
| PUT | `/api/admin/users/{id}/block` | Bloque un USER ; retourne son DTO de compte |
| PUT | `/api/admin/users/{id}/unblock` | Debloque un USER ; retourne son DTO de compte |
| GET | `/api/admin/stats` | Compteurs de comptes uniquement |

`AdminUserResponse` expose exactement id, firstName, lastName, email, role,
enabled, blocked et createdAt. Aucun mot de passe, hash, JWT, champ financier ou
information interne supplementaire n'est expose. La recherche utilise des
parametres lies et traite `%`/`_` comme des caracteres litteraux. Une recherche
vide retourne la liste complete, sans pagination pour ce MVP.

`AdminService` porte les regles metier et les transactions DB. Les operations
block/unblock modifient uniquement blocked, sans modifier enabled, role ou mot
de passe. Elles sont repetables. Un compte bloque ne peut plus se connecter et
ses JWT deja emis sont refuses tant qu'il reste bloque. Apres deblocage, une
nouvelle connexion est possible si enabled=true. Aucun mecanisme de revocation
permanente des JWT n'est ajoute ; la securite existante controle l'etat courant.

L'auto-blocage retourne HTTP 409 avec un message explicite. Bloquer ou debloquer
un compte ADMIN, y compris son propre compte, est interdit avec HTTP 409.
UserNotFoundException, AdminSelfBlockException et AdminAccountActionForbiddenException
sont traduites par GlobalExceptionHandler au format d'erreur commun.
L'inscription publique impose toujours USER ; les champs role/roles/authorities
injectes ne permettent pas d'obtenir ADMIN.

`AdminStatsResponse` contient uniquement :

- totalUsers : tous les comptes (aucun soft delete dans le modele actuel) ;
- activeUsers : enabled=true et blocked=false, ADMIN inclus ;
- blockedUsers : blocked=true, meme si enabled=false ;
- adminUsers : role=ADMIN, quel que soit l'etat enabled/blocked.

Aucune API Admin ne consulte les transactions, categories, budgets ou objectifs
personnels. Les endpoints financiers existants restent limites au proprietaire,
y compris pour un ADMIN. Aucune suppression de compte ni modification de role
n'est implementee. Le modele AppUser et les migrations V1 a V5 sont inchanges.

Validation depuis `backend` : `./mvnw.cmd test` reussi, 246 tests dont 41 nouveaux
tests d'integration Admin, sans echec ni test ignore. `./mvnw.cmd package -DskipTests`
reussi apres cette suite ; JAR executable dans `backend/target/`.
Les tests automatiques utilisent H2 avec Flyway ; aucun test manuel MySQL lance.
`git diff --check` reussi, frontend et dependances inchanges, aucun secret ajoute.
Aucun commit ni push pour 5A. 5B Frontend Admin et Machine Learning non commences.
