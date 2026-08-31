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
- `ADMIN` : supervise la plateforme et gere certains parametres globaux.

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

- Frontend : Angular, TypeScript, HTML, CSS ou SCSS, Angular Material.
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
| 1. Initialisation du backend Spring Boot | Termine |
| 2. Authentification et securite | En cours |
| 3. Profil utilisateur | Planifie |
| 4. Categories et modes de paiement | Planifie |
| 5. Transactions | Planifie |
| 6. Budgets | Planifie |
| 7. Objectifs d'epargne | Planifie |
| 8. Dashboard utilisateur | Planifie |
| 9. Administration | Planifie |
| 10. Notifications et recommandations statistiques | Planifie |
| 11. Export CSV | Planifie |
| 12. Initialisation du frontend Angular | Planifie |
| 13. Authentification frontend | Planifie |
| 14. Interfaces utilisateur | Planifie |
| 15. Interfaces administrateur | Planifie |
| 16. Integration frontend/backend | Planifie |
| 17. Module Machine Learning Python | Planifie |
| 18. Integration Spring Boot/FastAPI | Planifie |
| 19. Tests, securite et optimisation | Planifie |
| 20. Documentation finale et preparation de la demonstration | Planifie |

## Etat actuel

La phase 1 est terminee et la phase 2 est en cours. Le backend Spring Boot 3.5.16 est initialise avec Java 21.0.11. Le modele persistant `AppUser`, l'enum `Role`, le repository utilisateur et la premiere migration Flyway sont implementes. La migration V1 et la table `app_users` ont ete validees avec H2 en memoire et MySQL 8.

La connexion locale a MySQL avec le compte technique `finance_app`, l'application de la migration V1 par Flyway et la validation du schema par Hibernate ont ete verifiees. L'endpoint `/actuator/health` retourne `UP`. L'endpoint `/actuator/info` est disponible, mais aucune information personnalisee n'est encore configuree.

L'endpoint backend `POST /api/auth/register` est implemente et valide avec H2 et MySQL 8. Il valide et normalise les donnees, attribue obligatoirement le role `USER` et chiffre les mots de passe avec BCrypt avant leur enregistrement. Le stockage du hash BCrypt a ete valide sur la base MySQL reelle et la reponse HTTP ne contient aucune donnee sensible.

La verification des identifiants par `POST /api/auth/login` et la generation du JWT apres une connexion reussie sont validees avec H2 et MySQL 8. La validation sur la base MySQL reelle confirme la normalisation de l'adresse e-mail, la verification du hash BCrypt avec `PasswordEncoder.matches()` et la generation d'un token signe avec HS256. Une connexion valide retourne le token avec le resume public de l'utilisateur, tandis qu'un mauvais mot de passe retourne HTTP 401.

Le JWT contient uniquement les claims `sub`, `iat` et `exp` ; son subject contient l'identifiant utilisateur. Sa duree est configurable par `JWT_EXPIRATION_MINUTES` et vaut 60 minutes, soit 3600 secondes, par defaut. La cle de signature doit etre fournie en Base64 par la variable d'environnement obligatoire `JWT_SECRET_BASE64` et aucun secret n'est versionne. Aucun refresh token n'est genere.

Spring Security Web et la protection des routes ne sont pas encore implementes : aucun filtre JWT ni `SecurityFilterChain` n'est present, et le JWT retourne n'est pas encore lu depuis le header `Authorization`. Aucun projet Angular ou Python n'est encore initialise.
