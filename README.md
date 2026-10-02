# Conscience Numérique (Android)

Application de **friction intentionnelle éthique** : quand vous ouvrez une application que vous avez
vous-même signalée comme problématique, un écran vous rappelle vos valeurs et vous demande si vous
voulez vraiment continuer. Le but n'est pas de bloquer, mais de créer une pause consciente.

Le nom « Conscience Numérique » est provisoire. Pour le changer, il suffit de modifier
`app_name` dans [brand.xml](app/src/main/res/values/brand.xml) : il est repris partout (icône,
écrans, service d'accessibilité) ; le titre de ce README est à modifier à la main.

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
gradlew.bat :app:testDebugUnitTest         # tests unitaires (sans appareil)
gradlew.bat :app:connectedDebugAndroidTest # tests sur appareil (émulateur lancé) : migrations, base de données, écrans
```

Pour ne pas rejouer toute la suite à chaque modification, `scripts/test.sh` (Git Bash) choisit les tests :

```
scripts/test.sh focus FrictionGateTest InterstitialActivityTest   # seulement ces classes (unitaire ou appareil, détecté)
scripts/test.sh commit                                            # tous les tests unitaires, avant un commit
scripts/test.sh auto                                              # unitaires + tests sur appareil concernés par les fichiers modifiés
scripts/test.sh full                                              # avant un push ou une version : tout + lint
```

Les tests sur appareil réinstallent l'app : désinstaller d'abord la version déjà présente si l'installation échoue
(`adb uninstall fr.conscience.numerique`), ce qui efface ses données.

Ouvrir le dossier dans Android Studio, puis lancer sur l'AVD. Activer ensuite le service dans
*Paramètres > Accessibilité > Conscience Numérique* (la bannière de l'écran principal y renvoie).

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
