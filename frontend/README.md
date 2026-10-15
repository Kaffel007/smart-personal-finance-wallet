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

- `/login` : connexion, puis récupération du profil via `/api/auth/me` ; USER vers `/dashboard`, ADMIN vers `/admin` depuis 5B.
- `/register` : inscription, puis redirection vers `/login`.
- `/dashboard` : layout privé et dashboard connecté depuis la phase 4B.
- `/` et routes inconnues : dashboard si un token existe, login sinon.
- JWT dans localStorage sous `smart_finance_access_token`. Aucun mot de passe stocké.
- Interceptor Bearer limité aux endpoints protégés de l'API configurée.
- Une réponse protégée HTTP 401 efface la session et redirige vers login.
- Guard de navigation et récupération du profil après rafraîchissement.
- Déconnexion : suppression du token et du profil, redirection vers login.
- Navigation USER : transactions, catégories, budgets et objectifs d'épargne ; navigation ADMIN : Administration, depuis 5B.
- `/account` et lien Mon compte communs à USER et ADMIN depuis 6B, avec authGuard uniquement.

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

Build de production réussi et 275 tests réussis (Vitest, HttpTestingController et
RouterTestingHarness), dont 59 tests ajoutés en 4C, 46 en 4D, 47 en 4E, 41 en 5B et 53 en 6B. Les tests HTTP sont
simulés et ne dépendent d'aucun backend réel ni de MySQL.

La phase 4A et CORS ont été validés dans le navigateur par l'utilisateur.
Le backend attendu écoute sur localhost:8080 avec le profil Spring `dev`,
qui autorise uniquement http://localhost:4200. Aucun proxy frontend n'est configuré.
Les phases 4B (dont la synchronisation mois/année), 4C et 4D sont validées dans le navigateur, selon la confirmation de l'utilisateur. La validation navigateur de 4E reste à effectuer.
Aucun fichier backend n'a été modifié pendant les phases frontend 4B, 4C, 4D et 4E.

## Structure

- `src/app/core/` : configuration API, modèles auth, service, interceptor, guards, validations et tests.
- `src/app/features/auth/` : formulaires réactifs login et register.
- `src/app/features/dashboard/` : modèle, service HTTP, dashboard, formatage des montants et tests.
- `src/app/features/categories/` : modèles, service CRUD, page, messages métier et tests.
- `src/app/features/transactions/` : modèles, service CRUD/filtres, page, validations et tests.
- `src/app/features/budgets/` : modèles, service CRUD/filtres, page, styles et tests.
- `src/app/features/savings-goals/` : modèles, service CRUD, validations, page, styles et tests.
- `src/app/features/admin/` : modèles de comptes/statistiques, service, page et tests.
- `src/app/features/account/` : modèles self-service, service, formulaires, validation de confirmation et tests.
- `src/app/core/admin.guard.ts` : contrôle du rôle ADMIN depuis le profil authentifié.
- `src/app/shared/finance-ui.css` : styles communs aux pages de gestion.
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
et disponibles dans le MainLayout. Budgets disponibles depuis 4D ; objectifs disponibles depuis 4E.

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
4C est terminée et validée dans le navigateur par l'utilisateur.

## Phase 4D — Budgets UI

`/budgets` est une route privée lazy-loaded protégée par AuthGuard et disponible
dans MainLayout. Les objectifs d'épargne sont disponibles depuis 4E.

`BudgetService` utilise `/api/budgets` : getAll(filters?), getOne(id), create,
update et delete. HttpParams transmet year/month/categoryId seuls ou combinés.
Les interfaces Budget, CreateBudgetRequest et UpdateBudgetRequest reproduisent
les DTO backend inspectés. POST/PUT envoient uniquement categoryId/year/month/amount,
même si le service reçoit un objet contenant des champs supplémentaires.
JWT et HTTP 401 restent gérés par l'interceptor existant.

La période initiale est celle du navigateur ; mois français et années courante -3
à +1, bornées à 2000..2100, suivent la convention Dashboard. Une année enregistrée
hors de cette plage de sélection reste proposée pour la période active.
Les filtres rechargent le backend, annulent les requêtes obsolètes et masquent
les anciennes données pendant le chargement. Un filtre catégorie est disponible.

Le formulaire réactif inline préremplit la période active en création et tous les
champs en modification. Les catégories sont chargées par GET /api/categories?type=EXPENSE
et filtrées défensivement ; aucune INCOME ne peut être proposée ou soumise.
Sans catégorie de dépense, un lien conduit vers `/categories`.
Année et mois entiers sont limités à 2000..2100 et 1..12 ; le montant est requis,
positif, inférieur à 10¹⁵ et limité à quatre décimales comme le backend.

Après POST/PUT réussi, le formulaire est fermé/réinitialisé, la période de la
réponse sélectionnée, le filtre catégorie effacé et un seul GET recharge la liste.
DELETE exige une confirmation inline ; après HTTP 204, liste et feedback sont
actualisés. Les doubles soumissions sont bloquées pendant les mutations.
Les conflits 409 affichent le doublon catégorie/période ; erreurs génériques,
catégorie invalide, 404, chargements séparés et boutons Réessayer sont gérés.

Les cards affichent amount/spent/remaining/usagePercent du backend sans calcul
métier local. FinancialAmountPipe garde « unités ». La largeur et aria-valuenow
sont plafonnés à 100 ; texte et aria-valuetext gardent la vraie valeur backend.
Au-delà de 100 %, style d'alerte et « Budget dépassé » ; restant négatif visible.
Grille trois colonnes desktop, deux tablette et une mobile, formulaire adaptatif,
labels visibles, aides/aria-invalid, focus visible et messages status/alert.
La logique Dashboard reste inchangée et reflète les mutations au prochain chargement.

Validation : 9 tests service, 35 tests composant et 2 tests route/layout ajoutés ;
134 tests au total réussis, npm run build et git diff --check réussis.
Aucune dépendance ajoutée ni modification backend, migration, MySQL ou CORS.
4D est terminée et validée dans le navigateur ; documentation, commit et push
confirmés par l'utilisateur.

## Phase 4E — Objectifs d'épargne UI

`/savings-goals` est une route privée lazy-loaded protégée par AuthGuard et
accessible dans MainLayout, avec lien actif et sans badge À venir.

`SavingsGoalService` utilise `/api/savings-goals` : getAll(), getOne(id), create,
update et delete. Les interfaces SavingsGoal, CreateSavingsGoalRequest et
UpdateSavingsGoalRequest reproduisent les DTO backend inspectés. Les réponses
contiennent id/name/targetAmount/savedAmount/remainingAmount/progressPercent/targetDate.
La date est nullable ; savedAmount peut être absent ou null en création, mais
est obligatoire en modification. Le service ne transmet que les champs écrits
name/targetAmount/savedAmount/targetDate, même si l'objet reçu contient des champs
calculés ou internes. JWT et HTTP 401 restent gérés par l'interceptor.

Le formulaire réactif inline valide le nom non blanc (120 caractères maximum),
la cible positive et l'épargne positive ou nulle, avec quatre décimales maximum
et un montant inférieur à 10¹⁵. Le validateur de montant existant est réutilisé.
L'épargne initiale est zéro ; vide en création devient zéro dans le payload,
vide en modification est invalide. La date cible utilise un input date facultatif,
avec validation calendaire, sans imposer de date future.
La modification préremplit tous les champs et permet d'effacer la date cible.

Après POST/PUT réussi, formulaire fermé/réinitialisé, GET de la liste et feedback
« Objectif créé. » ou « Objectif mis à jour. ». DELETE exige une confirmation
inline ; après 204, un GET recharge la liste et « Objectif supprimé. » est annoncé.
Les mutations bloquent les doubles soumissions. Les erreurs liste/enregistrement/
suppression et 400/404 restent locales sans exposer les détails techniques ; un
bouton Réessayer recharge la liste. L'état vide propose « Créer un objectif ».

Les cards conservent l'ordre backend et affichent nom, cible, épargné, restant,
progression et date dd/MM/yyyy ou « Aucune date cible ». FinancialAmountPipe garde
le suffixe « unités ». Aucun calcul métier du restant ou du pourcentage côté UI.
Barre CSS et aria-valuenow bornés à 0..100 ; texte et aria-valuetext gardent la
valeur réelle. Dès 100 %, « Objectif atteint » et style de réussite ; au-delà,
pourcentage réel et restant négatif restent visibles.

Styles communs finance-ui.css réutilisés, grille desktop trois colonnes,
tablette deux et mobile une ; formulaire responsive, labels, aides de validation,
focus visible, messages status/alert et progression ARIA. La logique Dashboard
reste inchangée : les nouveaux totaux seront visibles à son prochain chargement.

Validation : 7 tests service, 38 tests composant et 2 tests route/layout ajoutés,
soit 181 tests réussis au total ; npm run build et git diff --check réussis.
Les tests HTTP sont simulés ; validation navigateur réelle de 4E restante.
Aucune dépendance ajoutée, aucun backend, migration, MySQL ou CORS modifié.
Pas de SavingsContribution, de liaison automatique aux transactions, d'Admin,
de ML, de notifications ou de graphiques avancés. Aucun commit ni push pour 4E.

## Phase 5B — Frontend Admin MVP

La page `/admin` est lazy-loaded, protégée par authGuard et adminGuard. Sans token,
redirection `/login` ; profil USER vers `/dashboard` ; profil ADMIN autorisé.
adminGuard utilise AuthService.currentUser ou GET /api/auth/me si le profil n'est
pas encore chargé, notamment après rafraîchissement. Un échec de lecture du profil
ferme la session et retourne au login. Le JWT n'est jamais décodé pour connaître le rôle.
MainLayout réutilise le profil chargé afin d'éviter un second appel immédiat à /me.

Après POST /api/auth/login puis GET /api/auth/me, la réponse /me décide de la
destination : USER /dashboard, ADMIN /admin. Le guard des pages publiques renvoie
aussi un ADMIN dont le profil est connu vers /admin. MainLayout conserve les cinq
liens USER et propose uniquement Administration pour ADMIN, avec déconnexion et
indicateur « Espace administrateur ». Aucun changement au backend ni au stockage JWT.

`AdminUser` reproduit id/firstName/lastName/email/role/enabled/blocked/createdAt ;
`AdminStats` reproduit totalUsers/activeUsers/blockedUsers/adminUsers.
Le service frontend AdminService expose :

- getStats() : GET /api/admin/stats ;
- getUsers(search?) : GET /api/admin/users, HttpParams search normalisé et omis si vide ;
- getUser(id) : GET /api/admin/users/{id} ;
- blockUser(id) / unblockUser(id) : PUT /api/admin/users/{id}/block ou /unblock, sans payload de compte.

Authorization et HTTP 401 restent entièrement gérés par l'interceptor existant.
La page charge statistiques et utilisateurs indépendamment, avec erreurs et retry
séparés. Quatre cartes affichent exclusivement les compteurs backend. Les comptes
restent dans l'ordre reçu et montrent identité, e-mail, rôle, statut et date française.
Les états bloqué et désactivé sont affichés ensemble si nécessaire.

La recherche part uniquement au submit (bouton ou Entrée), sans requête à chaque
frappe ; Réinitialiser recharge la liste complète. Les requêtes obsolètes sont
annulées. Le chargement masque les anciennes données, avec message spécifique en recherche.
La liste présente déjà tous les champs de compte ; getUser reste disponible sans
ajouter de page ou panneau de détail.

Bloquer exige une confirmation inline ; Débloquer est direct. Seuls les comptes
USER autres que le compte connecté disposent de ces actions. Aucun bouton pour
ADMIN, ni suppression ou changement de rôle. Les doubles clics sont bloqués
pendant la mutation ; après succès, liste de la recherche soumise et statistiques
sont rechargées depuis le backend et un feedback est annoncé.
Les erreurs de mutation 403/404/409 et générales ont des messages locaux, sans
afficher le message technique ou le JSON reçu. Aucune donnée financière, aucun
hash/mot de passe/JWT n'est affiché. Les autorisations backend restent déterminantes.

Styles finance-ui.css réutilisés, cartes statistiques 4/2/1 colonnes et comptes
2/1 colonnes selon largeur ; labels, boutons nommés, focus visible, status/alert,
confirmation accessible et formulaire utilisable au clavier. Aucune dépendance ajoutée.

Validation : 8 tests service, 22 tests composant et 11 nouveaux tests auth/guard,
soit 222 tests réussis au total. npm run build et git diff --check réussis.
Les tests HTTP sont simulés ; validation navigateur réelle de 5B restante.
Backend, migrations, MySQL et CORS inchangés. Aucun commit ni push ; ML non commencé.

## Phase 6B — Mon compte

`/account` est une route privée lazy-loaded commune à USER et ADMIN. Elle utilise
authGuard et aucun adminGuard : une visite anonyme conduit au login. MainLayout
ajoute Mon compte aux deux navigations, sans changer les destinations après login
ou les autorisations Admin. Un même AccountComponent affiche les quatre sections :
Informations du compte, Informations personnelles, Adresse e-mail et Mot de passe.

`Account` reproduit les champs id, firstName, lastName, email, role, enabled,
blocked et createdAt du backend inspecté. Les champs de rôle/statut restent en
lecture seule. Les modèles de requêtes n'ont aucun identifiant cible ni champ
de rôle/état ; le composant et les méthodes du service ne prennent aucune cible.
Il n'existe aucun sélecteur utilisateur ni édition d'un USER par ADMIN.

`AccountService` utilise l'URL API centralisée et expose :

- getMe() : GET /api/account/me ;
- updateProfile({ firstName, lastName }) : PUT /api/account/profile ;
- updateEmail({ newEmail, currentPassword }) : PUT /api/account/email ;
- changePassword({ currentPassword, newPassword }) : PUT /api/account/password, réponse 204.

Les payloads sont construits explicitement avec ces seuls champs ; la confirmation
reste locale. Authorization et HTTP 401 restent gérés par l'interceptor existant.
Les informations affichent le rôle Utilisateur/Administrateur, les états Actif,
Bloqué et Désactivé (les deux derniers ensemble si nécessaire), et la date française.
Le profil préremplit prénom/nom avec les contraintes de RegisterComponent : requis,
non blancs, 100 caractères maximum, puis trim avant envoi. L'e-mail est trim avant
validation, requis, valide et limité à 254 caractères ; le mot de passe actuel
est obligatoire et non blanc. Le formulaire de mot de passe réutilise exactement
passwordValidator (12 caractères Unicode minimum, 72 octets UTF-8 maximum), exige
une confirmation identique et refuse un nouveau mot de passe égal à l'actuel.

Après profil/e-mail réussi, la réponse backend actualise Account et AuthService
via syncCurrentUser, qui ne conserve que le résumé id/prénom/nom/e-mail/rôle.
L'en-tête se met à jour sans nouvelle connexion ni système global supplémentaire.
Les mots de passe sont effacés après succès, après erreur de credentials dans le
formulaire concerné, et à la destruction du composant. Ils ne sont jamais stockés
dans localStorage ou journalisés. Aucun hash/JWT n'est affiché.

Chargement du compte avec retry, états distincts pour les trois sauvegardes et
verrou commun contre les mutations concurrentes/doubles soumissions. Feedback
par section ; e-mail utilisé 409, mauvais mot de passe actuel 400 et erreur de
politique serveur sont traduits en messages locaux. Aucun JSON/message technique
du backend n'est repris directement. Les mots de passe incorrects en 400 ne
déconnectent pas l'utilisateur ; un 401 reste traité normalement par l'interceptor.

La session continue après chaque modification. Le JWT existant reste valide après
changement d'e-mail et de mot de passe jusqu'à expiration, sous réserve des contrôles
backend habituels. Aucun logout forcé après succès. L'isolation est imposée par le backend.

Styles finance-ui.css réutilisés ; quatre cards, deux colonnes desktop puis une
sur tablette/mobile, labels visibles, aides/erreurs associées, focus visible,
autocomplete given-name/family-name/email/current-password/new-password et status/alert.

Validation : 4 tests service, 42 tests composant et 7 tests navigation ajoutés ;
275 tests réussis au total, npm run build et git diff --check réussis.
Les tests HTTP sont simulés. Backend, migrations, MySQL, CORS et dépendances inchangés.
6A est terminée et validée MySQL selon la confirmation de l'utilisateur.
6B est implémentée/testée Angular ; validation navigateur réelle restante.
Aucun commit ni push pour 6B ; ML non commencé.
