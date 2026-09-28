# Conscience Numérique (Android)

Application de **friction intentionnelle éthique** : quand vous ouvrez une application que vous avez
vous-même signalée comme problématique, un écran vous rappelle vos valeurs et vous demande si vous
voulez vraiment continuer. Le but n'est pas de bloquer, mais de créer une pause consciente.

Cahier des charges : [pitch-dev-android-ios.md](pitch-dev-android-ios.md).
Licence : [GPL v3](LICENSE) (copyleft : toute version modifiée et distribuée doit rester sous GPL, code source ouvert).

## Stack

Kotlin, MVVM, Room (KSP), vues XML + ViewBinding, Material 3. `minSdk 26`, `targetSdk 37`.
100 % local : aucun backend, aucune télémétrie, sauvegardes désactivées.

## Structure

```
app/src/main/java/fr/conscience/numerique/
├── data/      Room (entités, DAO, base), repository, liste des apps installées
├── service/   FrictionAccessibilityService (détection du premier plan), FrictionGate
├── ui/        MainActivity (liste + labellisation), InterstitialActivity, ViewModel
└── util/      Utilitaires purs (parseProblems)
```

## Lancer

```
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

Ouvrir le dossier dans Android Studio, puis lancer sur l'AVD. Activer ensuite le service dans
*Paramètres > Accessibilité > Conscience Numérique* (la bannière de l'écran principal y renvoie).

## État de la V1

Fait : liste des apps, labellisation (champ libre + suggestions), interception via le service
d'accessibilité, interstitiel Oui / Non / pause 1 h, journal des choix en base.

À faire : écran de statistiques, réglage de fréquence (chaque ouverture / 1re du jour), durées de
pause configurables, onboarding d'activation du service, tests instrumentés, politique de
confidentialité.

## Problématiques et langues

Le catalogue prédéfini (7 catégories, 23 problématiques) est dans `data/ProblemCatalog.kt`. Chaque
entrée a une **clé stable** stockée en base ; son libellé vient des ressources : `values/strings.xml`
(en-US, par défaut) et `values-fr/strings.xml` (fr-FR). Pour ajouter une problématique : une entrée
dans le catalogue + une chaîne `problem_*` dans **les deux** fichiers. Les problématiques
personnalisées sont du texte libre, affiché tel quel dans toutes les langues. La langue de l'app
peut se changer dans les réglages Android (`locales_config.xml`).

## Choix à connaître

- « Non » renvoie à l'écran d'accueil ; l'app cible n'est pas tuée (Android ne l'autorise pas sans
  permission dédiée).
- Le service ne lit **aucun contenu d'écran** (`canRetrieveWindowContent="false"`).
- Les icônes des apps tierces sont chargées via `PackageManager`, jamais embarquées.
