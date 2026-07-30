# AGENTS.md

## Methode de travail

- Travailler etape par etape.
- Ne realiser que la phase demandee.
- Ne pas anticiper plusieurs grandes phases.
- Examiner l'existant avant toute modification.
- Annoncer les modifications prevues.
- Expliquer les fichiers importants crees ou modifies.
- S'arreter a la fin de chaque etape.
- Attendre une validation explicite avant la phase suivante.

## Securite

- Ne jamais enregistrer de vrai mot de passe dans le depot.
- Ne jamais enregistrer une vraie cle JWT.
- Ne jamais afficher ou reproduire les secrets.
- Utiliser des variables d'environnement.
- Proteger les donnees financieres cote backend.
- Appliquer l'isolation stricte des donnees par utilisateur.
- Ne jamais considerer le frontend comme une protection suffisante.

## Qualite

- Utiliser des noms de classes, methodes et variables en anglais.
- Utiliser une architecture claire et modulaire.
- Eviter la duplication.
- Ajouter les validations necessaires.
- Gerer proprement les erreurs.
- Compiler et tester apres les modifications techniques.
- Signaler honnetement les tests non executes ou les problemes rencontres.
- Ne jamais supprimer un fichier sans expliquer la raison.

## Documentation

- La documentation fonctionnelle peut etre ecrite en francais.
- Les noms techniques et le code doivent rester en anglais.
- Mettre a jour le README lorsque l'etat du projet change.
- Conserver la feuille de route a jour.

## Contexte technique

- Frontend Angular.
- Backend Spring Boot.
- Base MySQL.
- Securite JWT et BCrypt.
- Migrations Flyway.
- Machine Learning avec Python dans une phase avancee du PFA.
- Flutter uniquement comme perspective future.

## Conventions metier

- Utiliser `AppUser` pour l'entite utilisateur.
- Utiliser `FinancialTransaction` pour l'entite transaction.
- Roles fonctionnels `USER` et `ADMIN`.
- Un utilisateur accede uniquement a ses propres donnees.
- Un administrateur ne peut pas consulter les mots de passe.
- Un administrateur ne modifie pas les transactions privees.
