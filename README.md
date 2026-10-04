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
scripts/test.sh auto                                              # unitaires + tests sur appareil concernés par les fichiers modifiés (signale ceux qui n'ont aucun test)
scripts/test.sh full                                              # avant un push ou une version : tout + lint
```

Un hook `pre-push` (`scripts/hooks/`) lance `full` avant chaque `git push` et l'annule en cas d'échec (émulateur requis ;
`git push --no-verify` pour passer outre). Après un clone : `git config core.hooksPath scripts/hooks`.

Les tests sur appareil vident les données de l'app au départ, puis la réinstallent (en debug) à leur fin, en réactivant son
service d'accessibilité s'il l'était : l'app reste donc sur l'émulateur (seules ses données sont remises à zéro).

Ouvrir le dossier dans Android Studio, puis lancer sur l'AVD. Activer ensuite le service dans
*Paramètres > Accessibilité > Conscience Numérique* (la bannière de l'écran principal y renvoie).

## Publier une version de test (GitHub Releases)

`scripts/release.sh` construit l'APK signé de production dans `build/release/` (avec sa somme SHA-256) ; avec `--publish`, il crée la
release GitHub (pré-version) après confirmation. Il faut la CLI GitHub (`winget install GitHub.cli`, puis `gh auth login`), un arbre de
travail propre, des commits poussés, et une version (`versionName`/`versionCode` dans `app/build.gradle.kts`) pas encore publiée.
Sans la CLI, joindre l'APK à la main : GitHub > Releases > Draft a new release.

La clé de signature (`~/.android-keys/conscience-numerique.jks`) et `keystore.properties` ne sont jamais versionnés : **les sauvegarder**
(une application signée avec une autre clé ne peut plus se mettre à jour par-dessus l'ancienne). Sur le téléphone du testeur : autoriser
l'installation depuis la source choisie, puis, sur Android 13 et plus, autoriser les « paramètres restreints » de l'application avant
d'activer le service d'accessibilité.

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
