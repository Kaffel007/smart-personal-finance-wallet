# Brouillon du modele metier

Ce document est une premiere version textuelle du modele metier. Il ne correspond pas encore a des entites Java ni a des tables MySQL.

## AppUser

- Role : represente un utilisateur de l'application.
- Attributs envisages : id, firstName, lastName, email, passwordHash, preferredCurrency, enabled, blocked, createdAt, updatedAt.
- Relations principales : possede des roles, transactions, categories personnelles, budgets, objectifs, notifications et recommandations.
- Regles metier : email unique, mot de passe stocke avec BCrypt, acces uniquement a ses propres donnees, compte bloque interdit de connexion.

## Role

- Role : represente les autorisations fonctionnelles.
- Attributs envisages : id, name.
- Relations principales : associe a plusieurs `AppUser`.
- Regles metier : roles principaux `USER` et `ADMIN`.

## Category

- Role : classe les transactions.
- Attributs envisages : id, name, type, global, color, icon, active, createdAt.
- Relations principales : peut appartenir a un `AppUser` si elle est personnelle, peut etre utilisee par plusieurs `FinancialTransaction` et `Budget`.
- Regles metier : seules les categories personnelles peuvent etre modifiees par leur proprietaire, seules les categories globales sont gerees par l'admin.

## PaymentMethod

- Role : indique le mode de paiement d'une transaction.
- Attributs envisages : id, name, global, active.
- Relations principales : peut etre associe a plusieurs `FinancialTransaction`.
- Regles metier : les modes globaux sont administres par l'admin.

## FinancialTransaction

- Role : represente un revenu ou une depense.
- Attributs envisages : id, amount, type, transactionDate, description, createdAt, updatedAt.
- Relations principales : appartient a un `AppUser`, une `Category` et eventuellement un `PaymentMethod`.
- Regles metier : montant strictement positif, type `INCOME` ou `EXPENSE`, date et categorie obligatoires, revenu augmente le solde, depense reduit le solde.

## Budget

- Role : definit une limite de depense mensuelle.
- Attributs envisages : id, name, amountLimit, month, year, alertThresholdPercent, createdAt, updatedAt.
- Relations principales : appartient a un `AppUser`, peut etre lie a une `Category`.
- Regles metier : pas de doublon pour la meme categorie et la meme periode, calcul du montant depense et restant a partir des transactions.

## SavingsGoal

- Role : represente un objectif d'epargne.
- Attributs envisages : id, name, targetAmount, currentAmount, deadline, status, createdAt, completedAt.
- Relations principales : appartient a un `AppUser`, possede plusieurs `SavingsContribution`.
- Regles metier : objectif termine automatiquement lorsque le montant cible est atteint.

## SavingsContribution

- Role : represente une contribution ajoutee a un objectif d'epargne.
- Attributs envisages : id, amount, contributionDate, note, createdAt.
- Relations principales : appartient a un `SavingsGoal`.
- Regles metier : montant non negatif, met a jour la progression de l'objectif.

## Notification

- Role : informe l'utilisateur d'un evenement important.
- Attributs envisages : id, type, title, message, read, createdAt.
- Relations principales : appartient a un `AppUser`.
- Regles metier : notification limitee a son proprietaire, types possibles budget, objectif, anomalie et systeme.

## Recommendation

- Role : propose une action financiere personnalisee.
- Attributs envisages : id, type, title, message, severity, generatedAt, dismissed.
- Relations principales : appartient a un `AppUser`, peut etre liee a une categorie, un budget ou un objectif.
- Regles metier : ne doit etre generee que si l'historique disponible est suffisant.

## AdminActionLog

- Role : trace les actions administratives importantes.
- Attributs envisages : id, action, targetType, targetId, details, createdAt.
- Relations principales : appartient a l'`AppUser` administrateur qui a realise l'action.
- Regles metier : ne doit pas contenir de mot de passe ni de secret.

## GlobalSetting

- Role : stocke certains parametres globaux de la plateforme.
- Attributs envisages : id, settingKey, settingValue, description, updatedAt.
- Relations principales : modifie par un administrateur.
- Regles metier : ne doit pas stocker de secret reel, peut definir des seuils globaux d'alerte.
