# TODO

À reprendre plus tard. Cochez au fur et à mesure.

## À décider d'abord

- [ ] **Identifiant de l'app** (`fr.conscience.numerique`, utilisé comme `applicationId` et comme package Kotlin) : il date
  d'avant le nom « Red Flag » et ne pourra plus changer après la première publication sur Play. À choisir avant (par
  exemple `app.redflag`, à adapter à un domaine ou un nom qu'on possède). Le renommer touche `app/build.gradle.kts`,
  les dossiers `java/…`, le manifeste, `scripts/*` (service d'accessibilité, `pm clear`), le README et le schéma Room.

## Publication sur le Play Store

Fait : AAB et APK release signés (`scripts/release.sh`), `targetSdk` 37, écran d'accueil qui explique le service
d'accessibilité et demande un accord explicite, FAQ, site statique (`docs/`, EN + FR), release GitHub `v0.1.0`
(ancien nom, privée).

- [ ] **Compte développeur** : création, vérification d'identité, enregistrement de l'app (vérification des développeurs
  Google)
- [ ] **Mettre le site en ligne** (GitHub Pages sur `docs/`) : son adresse sert de politique de confidentialité
  - Remplacer l'adresse provisoire `example@email.com` : `node scripts/site.mjs set-email <adresse>`
  - Relire la politique de confidentialité (section « Vie privée » du site) ; elle doit avoir sa propre adresse
    (page ou ancre stable) à donner à la Play Console
- [ ] **Déclaration d'usage du service d'accessibilité** dans la Play Console, avec une vidéo de démonstration
  (activation du service, interruption, « Non, fermer l'application »)
- [ ] **Formulaire « Sécurité des données »** (réponse : aucune donnée collectée ni partagée), classification du contenu,
  public cible
- [ ] **Fiche du store** : titre « Red Flag – Ethical app check », description courte et longue (EN + FR), icône 512 px,
  image de présentation 1024×500, captures (on peut reprendre celles de `docs/assets/img/`)
- [ ] **Test fermé** : 12 testeurs pendant 14 jours avant la production (compte personnel récent), puis rapport
  pré-lancement
- [ ] **Version de production** : `versionCode` / `versionName` (`0.1.0` actuellement), republier la release GitHub sous
  le nom « Red Flag » (la `v0.1.0` porte l'ancien)
- [ ] En français, **modifier le terme "Problématique"** par le terme "enjeux".

## Qualité

- [ ] **Essayer l'app sur un vrai téléphone** : le Redmi A5 n'est pas détecté par `adb` (débogage USB, pilote). Vérifier
  surtout le service d'accessibilité, la superposition et les consignes d'économie d'énergie de Xiaomi
- [ ] **Écrans encore sans test** : réglages, liste des problématiques, sélecteur d'apps, parcours d'accueil (le script
  `scripts/test.sh auto` les signale quand on les modifie)
- [ ] **Langue de l'app** : `ProblemsManagerViewModel` et `ProblemDetailViewModel` gardent un contexte d'application
  (`applicationContext`), dont la langue peut ne pas suivre celle choisie pour l'app, comme c'était le cas pour la FAQ.
  Vérifier, et renforcer le test `theFaqFollowsTheLanguageChosenForTheApp` (il passe aussi avec l'ancien code)
- [ ] Polices du site en WOFF2 (elles sont en TTF, plus lourdes)

## Fonctionnalités

- [ ] **Ajouter une langue** (par exemple l'espagnol)
  - Nouveau dossier `values-es/` avec les mêmes clés que `values/` (vérification de parité : voir la commande ci-dessous),
    dont les 23 problématiques du catalogue
  - `res/raw-es/faq.xml` pour la FAQ, puis `node scripts/site.mjs sync-faq` et une page `docs/es/`
  - Ajouter la langue dans `res/xml/locales_config.xml`
  - Les pluriels espagnols ont une forme `many` en plus

## Fait

- [x] Doublons factorisés (libellés, sections, icônes, `toRef()`, accès au conteneur) : revue de code, point 3
- [x] ViewModels par fabrique (`screenViewModel`), testés sur base en mémoire ; accueil et statistiques testés : point 5
- [x] Un seul identifiant par problématique, base en version 7 : point 6
- [x] Structure, point 4 : `ui/` en sous-dossiers par domaine, `MonitoredAppDao` scindé (un DAO par table), icônes en cache
  (`AppIconCache`, remplie au démarrage pour les apps signalées, lecture hors du fil principal)
- [x] FAQ (écran « Comment ça marche », générée aussi pour le site) ; le Lorem Ipsum a disparu
- [x] Nom « Red Flag » et sous-titre « Ethical app check » (`app_name`, `app_subtitle` dans `brand.xml`)
- [x] Site statique EN + FR, captures régénérées (`docs/`)
- [x] Essai à blanc de la revue Play : points sensibles identifiés (accessibilité, `targetSdk`, test fermé, politique de
  confidentialité)

## Vérifier la parité des langues

```bash
diff <(grep -o 'name="[^"]*"' app/src/main/res/values/strings.xml | sort) \
     <(grep -o 'name="[^"]*"' app/src/main/res/values-fr/strings.xml | sort) && echo identique
```
