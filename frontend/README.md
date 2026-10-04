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
- `/dashboard` : layout privé et contenu provisoire, aucun appel aux statistiques.
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

Build de production réussi et 16 tests réussis (Vitest, HttpTestingController et
RouterTestingHarness). Les tests HTTP sont simulés : ils ne valident ni MySQL ni
le parcours contre un backend réel.

Lors de cette phase, aucun serveur backend n'écoutait sur localhost:8080.
Le parcours réel et CORS n'ont donc pas été validés. La configuration backend
inspectée ne configure pas CORS pour localhost:4200 ; une autorisation backend
sera probablement nécessaire pour les appels directs depuis le navigateur.
Aucun fichier backend n'a été modifié. Aucun proxy frontend n'est configuré.

## Structure

- `src/app/core/` : configuration API, modèles auth, service, interceptor, guards, validations et tests.
- `src/app/features/auth/` : formulaires réactifs login et register.
- `src/app/features/dashboard/` : dashboard provisoire.
- `src/app/layout/` : layout connecté et chargement du profil.
- `src/app/app.routes.ts` : routes lazy-loaded publiques et privées.
- `src/styles.css` : styles communs et responsive desktop/mobile.

La phase 4B et les interfaces CRUD métier ne sont pas commencées.
