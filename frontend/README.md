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
- Transactions, budgets et objectifs affichés comme rubriques à venir sans routes cassées.

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

Build de production réussi et 29 tests réussis (Vitest, HttpTestingController et
RouterTestingHarness), dont 13 tests dashboard ajoutés en 4B. Les tests HTTP sont
simulés et ne dépendent d'aucun backend réel ni de MySQL.

La phase 4A et CORS ont été validés dans le navigateur par l'utilisateur.
Le backend attendu écoute sur localhost:8080 avec le profil Spring `dev`,
qui autorise uniquement http://localhost:4200. Aucun proxy frontend n'est configuré.
La validation navigateur réelle du dashboard 4B reste à effectuer.
Aucun fichier backend n'a été modifié pendant la phase 4B.

## Structure

- `src/app/core/` : configuration API, modèles auth, service, interceptor, guards, validations et tests.
- `src/app/features/auth/` : formulaires réactifs login et register.
- `src/app/features/dashboard/` : modèle, service HTTP, dashboard, formatage des montants et tests.
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

4B est implémentée et testée avec Angular ; validation navigateur réelle à faire.
Les interfaces CRUD métier et la phase 4C ne sont pas commencées.
