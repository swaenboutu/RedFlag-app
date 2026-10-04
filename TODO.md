# TODO

À reprendre plus tard. Cochez au fur et à mesure.

## Liste

- [ ] **Vérifier les points 3 à 6 de la revue de code**
  - [x] 3. Factoriser les duplications : libellé d'une problématique (recalculé à 4 endroits), calcul des sections
    (`ProblemsManagerViewModel` / `AppDetailViewModel`), chargement d'icône (3 adaptateurs), `Problem.toRef()`, accès au
    conteneur (`(application as ConscienceApp).container`, 8 fichiers)
  - [ ] 4. Structure : sous-dossiers de `ui/` par écran (apps, problématiques, réglages, interruption), scinder
    `MonitoredAppDao`, chargement des icônes sur le fil principal et toutes en mémoire au démarrage (petit cache)
  - [x] 5. Tests : ViewModels construits par une fabrique (`screenViewModel`) et testés sur une base en mémoire, écrans
    d'accueil et de statistiques testés. Pas encore d'écran testé pour : réglages, problématiques (liste), sélecteur d'apps,
    parcours d'accueil (le script `scripts/test.sh auto` les signale quand on les modifie)
  - [x] 6. Un seul identifiant par problématique (clé du catalogue, ou `custom:…` généré pour une personnalisée) : renommer
    ne touche plus qu'une table (base en version 7)
- [ ] **Essai à blanc de la publication sur le Play Store** (voir si l'app passerait la revue)
  - Le point sensible : l'usage du service d'accessibilité doit être justifié et déclaré
  - À préparer : politique de confidentialité en ligne, formulaire « Sécurité des données », captures, icône, signature de
    la version de production
  - Piste de test interne + rapport pré-lancement de la Play Console
- [ ] **Ajouter une langue** (par exemple l'espagnol)
  - Nouveau dossier `values-es/` avec les mêmes clés que `values/` (vérification de parité : voir la commande ci-dessous),
    dont les 23 problématiques du catalogue
  - Ajouter la langue dans `res/xml/locales_config.xml`
  - Les pluriels espagnols ont une forme `many` en plus
- [ ] **FAQ** : l'application, son fonctionnement, son côté éthique et sa politique de confidentialité
  - Remplacera le Lorem Ipsum de « Comment ça marche » (Réglages), texte `settings_help_body`
  - La politique de confidentialité sert aussi pour le Play Store (point ci-dessus)

## Déjà ouvert (rappel)

- Le nom est « Red Flag » (`app_name` et `app_subtitle` dans `app/src/main/res/values/brand.xml`). L'identifiant de l'app
  (`fr.conscience.numerique`) date d'avant ce choix et ne pourra plus changer après publication : le choisir avant.
- Texte « Comment ça marche » : Lorem Ipsum provisoire

## Vérifier la parité des langues

```bash
diff <(grep -o 'name="[^"]*"' app/src/main/res/values/strings.xml | sort) \
     <(grep -o 'name="[^"]*"' app/src/main/res/values-fr/strings.xml | sort) && echo identique
```
