# Red Flag (Android)

Application de **friction intentionnelle éthique** : quand vous ouvrez une application que vous avez
vous-même signalée comme problématique, un écran vous rappelle vos valeurs et vous demande si vous
voulez vraiment continuer. Le but n'est pas de bloquer, mais de créer une pause consciente.

Nom : **Red Flag**, sous-titre « Ethical app check » (titre de la fiche du store : « Red Flag – Ethical app check »). Ils sont définis par
`app_name` et `app_subtitle` dans [brand.xml](app/src/main/res/values/brand.xml) et repris partout (icône, écrans, service d'accessibilité) ;
le titre de ce README et le site (`docs/`) sont à modifier à la main. L'identifiant technique `fr.conscience.numerique` est resté celui
d'avant le choix du nom : il ne pourra plus changer après la première publication.

Cahiers des charges : [Android](pitch-dev-android.md), [iOS](pitch-dev-ios.md), [design](pitch-designer.md).
Licence : [GPL v3](LICENSE) (copyleft : toute version modifiée et distribuée doit rester sous GPL, code source ouvert).

## Stack

Kotlin, MVVM, Room (KSP), vues XML + ViewBinding, Material 3. `minSdk 26`, `targetSdk 37`.
100 % local : aucun backend, aucune télémétrie, sauvegardes désactivées.

## Structure

```
app/src/main/java/fr/conscience/numerique/
├── data/      Room (entités, DAO par table, base), repository, liste des apps installées, cache des icônes
├── service/   FrictionAccessibilityService (détection du premier plan), FrictionGate
├── ui/        Un dossier par domaine : apps (liste, détail d'une app), problems (liste et détail des problématiques),
│              stats, settings (Réglages et FAQ), interruption (écran « Une seconde »), onboarding, common (éléments partagés)
└── util/      Utilitaires purs (parseProblems)
```

`fixtures/` : deux apps vides (Fixture A et B), installées par `scripts/test.sh` pendant les tests sur appareil pour que les écrans aient des apps ordinaires à lister (un émulateur de base n'a que des apps système, masquées), puis retirées. Jamais publiées.

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
*Paramètres > Accessibilité > Red Flag* (la bannière de l'écran principal y renvoie).

## Publier une version de test (GitHub Releases)

`scripts/release.sh` construit l'APK signé de production dans `build/release/` (avec sa somme SHA-256) ; avec `--publish`, il crée la
release GitHub (pré-version) après confirmation. Il faut la CLI GitHub (`winget install GitHub.cli`, puis `gh auth login`), un arbre de
travail propre, des commits poussés, et une version (`versionName`/`versionCode` dans `app/build.gradle.kts`) pas encore publiée.
Sans la CLI, joindre l'APK à la main : GitHub > Releases > Draft a new release.

La clé de signature (`~/.android-keys/conscience-numerique.jks`) et `keystore.properties` ne sont jamais versionnés : **les sauvegarder**
(une application signée avec une autre clé ne peut plus se mettre à jour par-dessus l'ancienne). Sur le téléphone du testeur : autoriser
l'installation depuis la source choisie, puis, sur Android 13 et plus, autoriser les « paramètres restreints » de l'application avant
d'activer le service d'accessibilité.

## Site web (docs/)

Site statique, sans serveur ni dépendance : une page par langue (`docs/index.html` (anglais) et `docs/fr/` (français)), un menu à ancres, la FAQ, des captures et un
formulaire de contact. Aucune ressource externe (polices et scripts sont dans
`docs/assets/`).

```bash
python -m http.server 8000 -d docs     # aperçu sur http://localhost:8000
node scripts/site.mjs sync-faq         # recopie la FAQ de l'application (res/raw/faq.xml et raw-fr/faq.xml) dans les deux pages
node scripts/site.mjs check            # liens, ancres, images, langues, FAQ à jour, adresse de contact (aussi joué par scripts/test.sh)
node scripts/site.mjs set-email adresse@exemple.fr   # adresse de contact : actuellement example@email.com
```

La FAQ a donc une seule source (les fichiers XML de l'application) : la modifier là, puis `sync-faq`. Le formulaire prépare un message dans le
logiciel de messagerie du visiteur (lien `mailto:`) : aucun service tiers ne reçoit ses données. Hébergement possible : GitHub Pages (branche
`main`, dossier `/docs` ; un dépôt privé exige un compte payant) ou tout hébergeur de fichiers statiques.

À compléter quand le nom sera trouvé : le nom (en-têtes, titres, pied de page, politique de confidentialité), l'adresse de contact, le lien de
téléchargement, la date de la politique de confidentialité, et les captures d'écran si l'interface change.

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
