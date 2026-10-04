# Pitch développeur Android — Application « Red Flag »

Nom : **Red Flag**, sous-titre « Ethical app check » (fiche du store : « Red Flag – Ethical app check »).
Langues : français et anglais dès la V1. Licence : GPL v3, code ouvert. Sans compte, sans publicité, sans serveur.

**État du projet** : la version Android existe (dépôt Git, GPL) et couvre tout ce qui est décrit plus bas. Ce document est donc
surtout un cahier des charges de reprise, de revue et de publication. Une version iOS est prévue ensuite, qui reprendra le
comportement décrit ici : voir [pitch-dev-ios.md](pitch-dev-ios.md). Pour le design de l'application et du logo, voir
[pitch-designer.md](pitch-designer.md).

---

### Concept

Je cherche un développeur Android pour reprendre, revoir et publier une application de **friction intentionnelle éthique** (déjà réalisée). Le principe : quand un utilisateur tente d'ouvrir une application qu'il a lui-même labelisée comme "problématique" (ex : sexisme, exploitation humaine, toxicité…), une fenêtre d'interstitiel s'affiche lui rappelant ses propres valeurs et lui demandant s'il veut vraiment ouvrir l'app. Il choisit "Oui" (l'app se lance) ou "Non" (l'app est tuée).

L'objectif n'est pas de bloquer, mais de **créer une pause consciente** entre l'impulsion et l'action.

---

### Comportement de référence (décidé et implémenté)

- **L'interruption** : écran plein écran « Une seconde. » : l'app visée, les problématiques liées à elle, la question « Est-ce
  vraiment important d'ouvrir l'app maintenant ? », « Non, fermer l'application » (bouton principal), « Oui, lancer l'app »
  (secondaire) et un lien « Ne plus me demander pendant 1 heure ». « Non » renvoie à l'écran d'accueil du téléphone.
- **Un « Oui » vaut 15 minutes** par app, à partir du dernier passage dans l'app ; il est oublié quand l'écran s'éteint.
  Sans cela, passer d'une app à l'autre et revenir répéterait l'interruption sans cesse.
- **Pause** : durée réglable (1 h par défaut), par app, depuis l'interruption ou les Réglages.
- **Problématiques** : un catalogue de 23 problématiques en 7 thèmes (bien-être mental, éthique sociale, impact
  environnemental, exploitation humaine, données et vie privée, pratiques économiques, éthique politique), traduites en FR/EN ;
  l'utilisateur peut en renommer, et en créer de personnalisées (rangées dans un thème ou dans « Personnalisé »), ainsi que
  mettre des favoris. Un identifiant unique par problématique : la clé du catalogue, ou `custom:…` pour les siennes.
- **Écrans** : accueil guidé (problématiques qui comptent, puis apps concernées, puis explication du service d'accessibilité
  et accord explicite), liste des apps (filtres : toutes, signalées, non signalées), détail d'une app, liste des
  problématiques, statistiques (apps interrompues, graphique sur 14 jours, jour/mois/année), Réglages, FAQ « Comment ça marche ».
- **Réglages** : liste affichée (avec ou sans apps système ; une app signalée reste toujours listée), durée de pause,
  autorisations Android. Mode debug caché (sept appuis sur le numéro de version) : réinitialiser l'app.
- **Statistiques** locales : refus, passages malgré tout, pauses, par app et par jour.

---

### Fonctionnalités attendues

1. **Listing des applications installées**
   - Récupération de la liste complète via `PackageManager`
   - Affichage avec icône, nom et nom de package

2. **Système de labellisation**
   - Pour chaque app, l'utilisateur peut attribuer une ou plusieurs problématiques personnalisées (champ libre + suggestions prédéfinies)
   - Stockage local (Room ou SharedPreferences)

3. **Interception du lancement**
   - Via **Accessibility Services** : détecter quand une app labelisée passe au premier plan
   - Afficher immédiatement un écran d'interstitiel custom avant que l'app soit visible
   - L'interstitiel affiche : le nom de l'app, ses problématiques définies, et deux boutons "Oui, continuer" / "Non, revenir en arrière"

4. **Gestion du choix**
   - "Oui" → l'app se lance normalement
   - "Non" → retour à l'écran d'accueil (ou écran précédent)

5. **Statistiques (optionnel / v2)**
   - Nombre de fois où l'utilisateur a dit "Non" par app
   - Historique des choix

---

### Ce que Google Play impose (audit fait en octobre 2026)

L'usage de l'API d'accessibilité est le point sensible de la revue : l'app n'est pas un outil d'accessibilité.
- **Avertissement bien visible dans l'app** avant d'envoyer l'utilisateur dans les réglages d'accessibilité, avec un accord
  explicite (« Accepter et continuer ») : fait, et gardé par un test pour qu'il ne disparaisse pas.
- **Formulaire de déclaration** dans la Play Console, avec probablement une **vidéo** de démonstration. À faire.
- Le service ne lit **aucun contenu d'écran** (`canRetrieveWindowContent=false`) : il ne détecte que le nom de l'app au premier
  plan. À dire clairement dans la politique de confidentialité.
- **`targetSdk` 36 minimum** (ici 37), `minSdk` 26. Sauvegardes Android désactivées.
- **Compte personnel récent** : test fermé avec 12 testeurs pendant 14 jours avant la production.
- **Vérification des développeurs** : enregistrement de l'app et de son identifiant auprès de Google.
- **Politique de confidentialité en ligne** (le site `docs/` la porte) et formulaire « Sécurité des données » (rien n'est
  collecté ni partagé).
- **L'identifiant de l'app** (`applicationId`) ne peut plus changer après la première publication : à fixer avant.
- Livrable : un **AAB signé** (3,4 Mo), l'APK de test signé aussi ; `scripts/release.sh` fait les deux.

---

### Points techniques importants à anticiper

- L'**Accessibility Service** nécessite une permission explicite de l'utilisateur dans les paramètres système — prévoir un onboarding clair pour guider cette activation
- Certains constructeurs (Xiaomi, Huawei, Samsung…) ont des surcouches qui peuvent interférer avec les Accessibility Services — prévoir des tests multi-constructeurs
- L'interstitiel doit s'afficher **avant** que l'app cible soit visible, ce qui nécessite une gestion fine des événements `TYPE_WINDOW_STATE_CHANGED`
- L'app doit gérer les cas de **relance** (si l'utilisateur revient sur l'app labelisée depuis le multitâche)
- Tout le stockage est **100% local**, pas de backend, pas de compte utilisateur
- Un fabricant (Xiaomi, par exemple) peut tuer le service en arrière-plan ou demander des autorisations de plus (démarrage
  automatique, affichage au-dessus d'autres apps) : **vérifier sur de vrais téléphones**, pas seulement sur émulateur. À faire.

---

### Exigences non fonctionnelles importantes

**Vie privée — point critique**
- Zéro télémétrie, zéro analytics tiers (pas de Firebase, Mixpanel ou équivalent)
- Tout reste 100% local, aucune donnée ne quitte l'appareil
- La politique de confidentialité devra expliquer clairement ce que l'Accessibility Service peut techniquement "voir", même si l'app n'en fait rien de malveillant — les utilisateurs et le Play Store poseront la question

**Performance de l'interstitiel**
- L'écran de friction doit s'afficher en moins d'une seconde — une latence perceptible viderait le concept de son sens et frustrerait l'utilisateur

**UX de la friction**
- Prévoir un mode **pause temporaire** (désactiver la friction sur une app pendant 1h, 24h…) — sans ça, les utilisateurs désinstalleront
- Prévoir un paramètre de **fréquence** : est-ce qu'on interroge l'utilisateur à chaque ouverture, ou seulement la première fois de la journée ?

**Cohérence éthique de l'app elle-même**
- Pas de dark patterns dans l'onboarding
- Pas de notifications agressives pour ramener l'utilisateur dans l'app
- Pas de modèle freemium manipulateur si monétisation un jour envisagée

**Accessibilité**
- Compatibilité TalkBack dès la V1
- Contraste suffisant sur l'interstitiel
- L'écran de friction doit être entièrement navigable sans interaction tactile

**Internationalisation**
- Fait : `values/` (anglais) et `values-fr/`, avec un test qui vérifie que les deux ont les mêmes clés, et la langue propre à l'app
  choisie dans les réglages Android (`locales_config.xml`). Prochaine langue envisagée : l'espagnol (pluriels avec forme `many`).
- Le texte de la FAQ est dans `res/raw/faq.xml` (et `raw-fr/`) ; le site statique en est généré, pour qu'il ne diverge jamais de l'app.

**Considérations légales**
- Les icônes des apps tierces doivent être récupérées via le système (`PackageManager`) et non embarquées dans l'app — c'est la pratique tolérée par les éditeurs
- La licence open source sera de type **GPL** (et non MIT) pour empêcher toute réutilisation commerciale sans consentement

---

### Stack et conventions du dépôt

- Kotlin natif, vues XML + ViewBinding, Material 3, Room (base en version 7, migrations testées), architecture MVVM
- `ui/` rangé par domaine (apps, problems, stats, settings, interruption, onboarding, common) ; un DAO par table
- Les ViewModels sont construits par une fabrique (`screenViewModel`) et testés sur une base en mémoire
- Icônes des apps lues par le système et gardées en cache (préchargées au démarrage pour les apps signalées)
- Tests : une centaine de tests unitaires, une centaine sur appareil (émulateur) ; `scripts/test.sh` choisit ceux qui
  concernent les fichiers modifiés (`auto`), ou tout (`full`, aussi avant chaque push, avec le lint et la vérification du site).
  Une mini-app de test (`fixtures/`) fournit des apps ordinaires aux émulateurs qui n'en ont pas. Testé sur Android 8.1 et récent.
- Le projet est publié en **open source sous licence GPL**

---

### Ce que je recherche

La V1 Android existe. Je cherche, selon ce que vous proposez (devis et estimation de charge pour chaque point) :
1. une **revue de code** et de l'architecture (en particulier : le service d'accessibilité et sa fiabilité) ;
2. des **tests sur de vrais appareils** de plusieurs fabricants et versions d'Android ;
3. un accompagnement pour la **publication sur Google Play** (déclaration d'accessibilité, vidéo, formulaires, test fermé) ;
4. les écrans encore sans test automatique (Réglages, liste des problématiques, sélecteur d'apps, parcours d'accueil) ;
5. l'ajout d'une langue.

