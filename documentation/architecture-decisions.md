# Decisions d'architecture preliminaires

Ce document liste les decisions initiales du projet. Elles pourront evoluer pendant les phases d'initialisation et de developpement.

| Decision | Choix | Justification | Statut |
| --- | --- | --- | --- |
| Plateforme initiale | Application web uniquement | Concentrer la premiere version sur le perimetre PFA principal | Valide |
| Application mobile | Aucune application Flutter en premiere version | Flutter reste une perspective future | Valide |
| UI Angular | Angular Material | Bibliotheque mature, bien integree a Angular et adaptee aux interfaces de gestion | Provisoire |
| Graphiques | Chart.js avec ng2-charts | Solution courante pour dashboards Angular et graphiques financiers simples | Provisoire |
| Java | Java 21 | Version LTS utilisee et verifiee par Maven Wrapper | Valide |
| Backend | Spring Boot 3.5.16 | Version initialisee et compilee avec Java 21 | Valide |
| Build backend | Maven Wrapper 3.3.4 avec Maven 3.9.16 | Build reproductible sans installation globale de Maven | Valide |
| API | API REST | Architecture claire pour separer frontend, backend et futur service ML | Valide |
| Securite | Spring Security | Integration native avec Spring Boot | Provisoire |
| Authentification | JWT | Permet une authentification stateless adaptee a une application web moderne | Provisoire |
| Hash des mots de passe | BCrypt | Algorithme reconnu pour stocker les mots de passe de maniere securisee | Valide |
| Base de donnees | MySQL 8 | Base relationnelle adaptee aux donnees metier du projet | Provisoire |
| Migrations | Flyway | Versionner les evolutions de schema de maniere controlee | Provisoire |
| Machine Learning | Python et FastAPI | Python est adapte aux traitements ML, FastAPI facilite l'exposition d'un service REST | Provisoire |
| Notifications | Notifications internes en premiere version | Simpler a developper avant email ou push | Valide |
| Budgets | Budgets mensuels en premiere version | Periode simple et naturelle pour les finances personnelles | Valide |
| Devise | Devise principale configurable par utilisateur | Simplifie la premiere version tout en gardant une personnalisation utile | Valide |
| Connexion bancaire | Aucune connexion bancaire reelle | Evite les contraintes legales, securite et integration bancaire en premiere version | Valide |
