# TODO

À reprendre plus tard. Cochez au fur et à mesure.

## Liste

- [ ] **Vérifier les points 3 à 6 de la revue de code**
  - [ ] 3. Factoriser les duplications : libellé d'une problématique (recalculé à 4 endroits), calcul des sections
    (`ProblemsManagerViewModel` / `AppDetailViewModel`), chargement d'icône (3 adaptateurs), `Problem.toRef()`, accès au
    conteneur (`(application as ConscienceApp).container`, 8 fichiers)
  - [ ] 4. Structure : sous-dossiers de `ui/` par écran (apps, problématiques, réglages, interruption), scinder
    `MonitoredAppDao`, chargement des icônes sur le fil principal et toutes en mémoire au démarrage (petit cache)
  - [ ] 5. Tests : migrations Room 1→4 (les schémas JSON existent), ViewModels testables via une fabrique
    (`ViewModelProvider.Factory`), test de l'écran d'interruption relancé pour deux apps différentes
    (bug déjà corrigé, sans test de non-régression)
  - [ ] 6. Décision de conception : identifier une problématique personnalisée par un identifiant plutôt que par son texte
    (renommer touche aujourd'hui 3 tables)
- [ ] **Écran de statistiques**, à ajouter dans le menu (barre du bas)
  - Les données existent déjà : chaque « Oui » / « Non » est enregistré (`ChoiceEvent`), et `AppRepository.refusals`
    (nombre de refus par app) est prêt mais jamais lu
  - Idées du document de conception : déclenchements, Oui contre Non, par app, par semaine ou par mois
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
- [ ] **Onboarding**, dans cet ordre :
  1. un écran explicatif
  2. un écran pour mettre des problématiques en favoris
  3. l'association d'une problématique à une application installée
  - L'écran des favoris et celui du choix d'apps existent déjà : les réutiliser. Demander aussi l'activation du service
    d'accessibilité (il n'y a pas encore d'écran guidé pour ça, seulement le bandeau)
  - Pas de dark patterns (exigence du pitch)
- [ ] **FAQ** : l'application, son fonctionnement, son côté éthique et sa politique de confidentialité
  - Remplacera le Lorem Ipsum de « Comment ça marche » (Réglages), texte `settings_help_body`
  - La politique de confidentialité sert aussi pour le Play Store (point ci-dessus)

## Déjà ouvert (rappel)

- Le nom « Conscience Numérique » est provisoire : `app_name` dans `app/src/main/res/values/brand.xml`. L'identifiant
  de l'app (`fr.conscience.numerique`) ne pourra plus changer après publication : le choisir avant.
- Version minimale d'Android (26 aujourd'hui) : passer à 28 supprimerait les 7 avertissements de polices variables
  (graisses fausses sur Android 8.x)
- Rendu non vérifié sur Android 8 à 11 (l'émulateur actuel est en Android 17)
- Texte « Comment ça marche » : Lorem Ipsum provisoire

## Vérifier la parité des langues

```bash
diff <(grep -o 'name="[^"]*"' app/src/main/res/values/strings.xml | sort) \
     <(grep -o 'name="[^"]*"' app/src/main/res/values-fr/strings.xml | sort) && echo identique
```
