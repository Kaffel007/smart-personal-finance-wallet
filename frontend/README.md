# Smart Personal Finance Wallet — Frontend

Application Angular 22.2.1 standalone, TypeScript et CSS, sans bibliothèque UI.

## Lancement

```powershell
cd frontend
npm ci
npm start
```

Frontend : http://localhost:4200. API attendue : http://localhost:8080/api.
L'URL API est centralisée dans `src/app/core/api.config.ts`.

## Phase 4A

- `/login` : connexion, puis récupération du profil via `/api/auth/me`.
- `/register` : inscription, puis redirection vers `/login`.
- `/dashboard` : layout privé et dashboard connecté depuis la phase 4B.
- `/` et routes inconnues : dashboard si un token existe, login sinon.
- JWT dans localStorage sous `smart_finance_access_token`. Aucun mot de passe stocké.
- Interceptor Bearer limité aux endpoints protégés de l'API configurée.
- Une réponse protégée HTTP 401 efface la session et redirige vers login.
- Guard de navigation et récupération du profil après rafraîchissement.
- Déconnexion : suppression du token et du profil, redirection vers login.
- Transactions et catégories accessibles depuis le layout ; budgets et objectifs encore à venir.

L'inscription applique les limites backend : noms requis (100 caractères maximum),
e-mail valide (254 caractères maximum), mot de passe non vide de 12 caractères
Unicode minimum et 72 octets UTF-8 maximum. Les erreurs de champs sont affichées
avec des messages locaux, sans exposer les détails techniques du serveur.

Le guard frontend ne remplace pas la sécurité backend. localStorage est le choix
MVP et reste accessible au JavaScript de cette origine. Aucun secret de signature
JWT ne doit être placé dans le frontend.

## Validation

```powershell
npm run build
npm test -- --watch=false
```

Build de production réussi et 88 tests réussis (Vitest, HttpTestingController et
RouterTestingHarness), dont 59 tests ajoutés en 4C. Les tests HTTP sont
simulés et ne dépendent d'aucun backend réel ni de MySQL.

La phase 4A et CORS ont été validés dans le navigateur par l'utilisateur.
Le backend attendu écoute sur localhost:8080 avec le profil Spring `dev`,
qui autorise uniquement http://localhost:4200. Aucun proxy frontend n'est configuré.
La phase 4B et la synchronisation mois/année sont validées dans le navigateur, selon la confirmation de l'utilisateur. La validation navigateur de 4C reste à effectuer.
Aucun fichier backend n'a été modifié pendant les phases frontend 4B et 4C.

## Structure

- `src/app/core/` : configuration API, modèles auth, service, interceptor, guards, validations et tests.
- `src/app/features/auth/` : formulaires réactifs login et register.
- `src/app/features/dashboard/` : modèle, service HTTP, dashboard, formatage des montants et tests.
- `src/app/features/categories/` : modèles, service CRUD, page, messages métier et tests.
- `src/app/features/transactions/` : modèles, service CRUD/filtres, page, validations et tests.
- `src/app/shared/finance-ui.css` : styles communs aux deux pages.
- `src/app/layout/` : layout connecté et chargement du profil.
- `src/app/app.routes.ts` : routes lazy-loaded publiques et privées.
- `src/styles.css` : styles communs et responsive desktop/mobile.

## Phase 4B — Dashboard réel

`DashboardService.getSummary(year, month)` appelle
`GET http://localhost:8080/api/dashboard/summary?year=YYYY&month=MM`, avec
l'URL API existante et le JWT ajouté automatiquement par l'interceptor.
`DashboardSummary` reproduit exactement les 16 champs de la réponse backend.

La période initiale est le mois et l'année du navigateur. Les sélecteurs proposent
les douze mois français et les années de l'année courante moins trois à plus un,
dans les limites backend 2000..2100. Un changement recharge la synthèse ; une
sélection inchangée n'effectue pas de requête supplémentaire. Une requête devenue
obsolète est annulée et les données précédentes sont masquées pendant le chargement.

Le dashboard affiche revenus, dépenses, solde et nombre de transactions, puis
les budgets du mois (total, dépensé, restant, nombre, utilisation) et les objectifs
d'épargne globaux, toutes périodes confondues (cible, épargné, restant, nombre,
progression). Aucun calcul métier du solde ou des pourcentages n'est refait côté client.

Les barres et leur valeur ARIA sont bornées à 0..100 ; le texte et `aria-valuetext`
conservent le pourcentage réel, même au-delà de 100 %. Les restants négatifs et les
dépassements restent visibles. Les soldes positif, nul et négatif ont des styles
et des libellés distincts.

Les montants utilisent un format français à deux décimales. Aucune devise effective
n'est définie dans le contrat ou la documentation actuelle : le suffixe neutre
« unités » est centralisé dans `financial-amount.pipe.ts`. Il ne représente pas
une gestion multi-devise.

États prévus : chargement, erreur générique avec bouton Réessayer, zéro avec les
quatre cartes conservées, absence de budget et absence d'objectif. Grille de
quatre cartes sur grand écran, deux sur tablette et une sur mobile ; sections
adaptatives, labels visibles et barres de progression accessibles.

Les tests vérifient le contrat HTTP, la période initiale, tous les indicateurs,
les changements de période, l'absence de doublons, l'annulation des requêtes
obsolètes, l'erreur/retry, l'état vide, les dépassements et les trois états du solde.

4B est terminée, testée avec Angular et validée dans le navigateur par l'utilisateur.

## Phase 4C — Transactions et catégories

Les routes privées `/transactions` et `/categories` sont protégées par AuthGuard
et disponibles dans le MainLayout. Budgets et objectifs restent marqués À venir.

`CategoryService` utilise `/api/categories` : getAll(type?), getOne(id), create,
update et delete. Les DTO contiennent uniquement name/type ; la réponse ajoute id.
La page sépare revenus et dépenses et propose un formulaire réactif inline, avec
nom obligatoire et limite de 100 caractères. Création, modification du nom/type
et suppression sont possibles. Une confirmation inline est obligatoire avant DELETE.
Les conflits 409 sont traduits en messages locaux : doublon ou catégorie utilisée.
Le renommage reste disponible ; le backend arbitre le changement de type et la
suppression d'une catégorie utilisée. Les 404 et autres erreurs ne révèlent aucun
détail technique.

`TransactionService` utilise `/api/transactions` : getAll(filters?), getOne(id),
create, update et delete. Les filtres type/categoryId sont transmis par HttpParams,
combinables et réinitialisables. La liste conserve l'ordre reçu du backend.
La réponse reprend exactement id, amount, type, transactionDate, description,
categoryId et categoryName. Les payloads create/update contiennent seulement
categoryId, amount, transactionDate et description : aucun userId ni type.

Le type visuel du formulaire sélectionne les catégories disponibles, puis le
backend dérive le type réel depuis la catégorie. Changer le type vide la sélection
précédente. Si aucune catégorie n'existe, un lien conduit vers `/categories`.
Le montant est positif, limité à 15 chiffres entiers et 4 décimales ; la date est
requise, non future et initialisée à la date locale du navigateur. La description
est optionnelle, limitée à 255 caractères et affichée par un tiret si absente.
Le préremplissage, la modification et la suppression avec confirmation sont pris
en charge. Les signes +/− sont uniquement visuels ; les montants métier restent
positifs. FinancialAmountPipe conserve le format français et le suffixe « unités ».

Après mutation, la liste est rechargée et un message de succès est annoncé sans
redirection vers le dashboard. Pendant une mutation, les actions sont désactivées
et les doubles soumissions évitées. Chargement, erreurs avec retry et états vides
sont distincts. Les filtres annulent les requêtes devenues obsolètes.
Les pages utilisent des cards responsive, labels, focus visible, aides associées
aux champs et feedback accessible. Aucun window.confirm ni bibliothèque UI.

Le JWT reste ajouté par l'interceptor existant ; les 401 restent gérés globalement.
Le backend conserve toute la responsabilité de l'autorisation et de l'isolation
par utilisateur. Aucun fichier backend, migration ou donnée MySQL modifié.

Validation : 88 tests et build de production réussis. Les tests sont entièrement
simulés et vérifient contrats HTTP, CRUD, filtres, validations, confirmations,
conflits, erreurs, feedback et protection des routes.
4C est implémentée/testée Angular ; validation navigateur réelle restante.
4D Budgets UI et 4E Savings Goals UI ne sont pas commencées.
