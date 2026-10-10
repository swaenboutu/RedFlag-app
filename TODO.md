# TODO

À reprendre plus tard. Cochez au fur et à mesure.

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
- [ ] **Politique de confidentialité dans l'app** : ajouter son lien dans « À propos » (tiroir) une fois qu'elle est hébergée sur le site
- [ ] **Déclaration d'usage du service d'accessibilité** dans la Play Console, avec une vidéo de démonstration
  (activation du service, interruption, « Non, fermer l'application »)
- [ ] **Formulaire « Sécurité des données »** (réponse : aucune donnée collectée ni partagée), classification du contenu,
  public cible
- [ ] **Fiche du store** : titre « Red Flag – Ethical app check », description courte et longue (EN + FR), icône 512 px,
  image de présentation 1024×500, captures (on peut reprendre celles de `docs/assets/img/`)
- [ ] **Test fermé** : 12 testeurs pendant 14 jours avant la production (compte personnel récent), puis rapport
  pré-lancement
- [ ] **Version 0.4.1** (`versionCode` 9) : release GitHub `v0.4.1` publiée (nouveau design, mode sombre) ; reste la fiche Play (la `v0.1.0` porte l'ancien nom)
- [ ] **Site** : reprendre la palette (crème, bleu nuit, corail) et les captures du nouveau design, clair et sombre

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
- [ ] **Recommandations de désinstallation** (idée, règles à travailler) : suggérer de désinstaller une app que l'utilisateur
  refuse presque toujours d'ouvrir, d'après ses tentatives d'ouverture des derniers mois et les statistiques de blocage
  - Données déjà là : `choice_events` (app, date, ouvert ou refusé, pause) ; tout reste sur le téléphone
  - Règles à définir, par exemple : fenêtre glissante (3 derniers mois ?), nombre minimal de tentatives (10 ?), part de
    refus ≥ 100 % ou un seuil acceptable (90 % ?), prise en compte ou non des pauses (« Ne plus me demander ») et du côté
    récent (l'app est-elle encore refusée ces dernières semaines ?)
  - Où l'afficher : une carte discrète dans « Vos applications » ou dans les statistiques d'une app, jamais une
    notification ; formulation sans jugement ; possibilité de la refuser (« ne plus me le suggérer pour cette app »)
  - Action : ouvrir la fiche de l'app dans les réglages Android (aucune permission) plutôt que lancer la désinstallation
    directement (`REQUEST_DELETE_PACKAGES` serait un point de plus à justifier auprès de Google Play)
  - À prévoir : texte FR/EN, tests des règles (fonction pure, testée avec des dates injectées), mention dans la FAQ

## Fait

- [x] Identifiant de l'app `app.redflag` (`applicationId` et package Kotlin, remplace `fr.conscience.numerique`) : à ne plus changer après la première publication sur Play
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

## Vocabulaire

- En français, « problématique » est devenu « enjeu » dans l'application, la FAQ et le site (`values-fr/strings.xml`, `raw-fr/faq.xml`,
  `docs/fr/`). Restent à jour à la main, si besoin : les pitchs (`pitch-*.md`) et les commentaires du code. Les noms de code
  (`Problem`, `nav_problems`…) et le terme anglais « issue » ne changent pas.

## Vérifier la parité des langues

```bash
diff <(grep -o 'name="[^"]*"' app/src/main/res/values/strings.xml | sort) \
     <(grep -o 'name="[^"]*"' app/src/main/res/values-fr/strings.xml | sort) && echo identique
```
