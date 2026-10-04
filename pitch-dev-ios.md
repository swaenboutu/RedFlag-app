# Pitch développeur iOS — Application « Red Flag »

Nom : **Red Flag**, sous-titre « Ethical app check » (fiche du store : « Red Flag – Ethical app check »).
Langues : français et anglais dès la V1. Licence : GPL v3, code ouvert. Sans compte, sans publicité, sans serveur.

**État du projet** : la version Android existe déjà (dépôt Git, GPL) et sert de **référence de comportement** : voir plus bas et
[pitch-dev-android.md](pitch-dev-android.md). La version iOS est à développer. Pour le design de l'application et du logo, voir
[pitch-designer.md](pitch-designer.md).

---

### Concept

Je cherche un développeur iOS pour créer une application de **friction intentionnelle éthique**. Le principe : quand un utilisateur tente d'ouvrir une application qu'il a lui-même labelisée comme "problématique" (ex : sexisme, exploitation humaine, toxicité…), une fenêtre d'interstitiel s'affiche lui rappelant ses propres valeurs et lui demandant s'il veut vraiment ouvrir l'app. Il choisit "Oui" (l'app se lance) ou "Non" (l'app est fermée).

L'objectif n'est pas de bloquer, mais de **créer une pause consciente** entre l'impulsion et l'action.

---

### Ce qui est déjà décidé (version Android, à reproduire quand c'est possible)

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

- Ce qui change sur iOS : pas de liste des apps installées (sélection manuelle), pas de service d'accessibilité (c'est la
  fonction Raccourcis, voir ci-dessous), et une interception qui n'est pas hermétique ; l'explication de la permission devient
  l'explication du raccourci.
- Le **catalogue de 23 problématiques** et ses traductions FR/EN, ainsi que le texte de la FAQ, existent déjà (fichiers
  `strings.xml` et `faq.xml` du dépôt Android) : à reprendre tels quels pour `Localizable.strings`, plutôt que de les réécrire.
- Même nom, même identité visuelle (voir le pitch designer), même licence GPL.
- Sans compte, sans publicité, sans serveur ; politique de confidentialité commune, hébergée sur le site statique du projet.

### Contraintes iOS à avoir en tête dès le départ

iOS ne permet pas d'intercepter nativement le lancement d'une app tierce comme on peut le faire sur Android. Deux approches sont envisageables, avec leurs compromis respectifs :

**Option A — Raccourcis iOS (Shortcuts)**
- L'utilisateur crée une automatisation dans l'app Raccourcis qui redirige l'ouverture de l'app cible vers notre app
- Notre app affiche l'interstitiel, puis selon le choix, ouvre ou non l'app cible
- L'app peut **proposer le raccourci pré-configuré** via `INUIAddVoiceShortcutViewController` (un tap de confirmation par app à configurer)
- Limite : fonctionne uniquement si l'app est ouverte depuis l'écran d'accueil (pas via Spotlight ou lien externe)
- Référence existante : c'est le modèle utilisé par **One Sec** (validé par Apple, présent sur l'App Store)

**Option B — Screen Time API (FamilyControls / ManagedSettings)**
- Permet de bloquer des apps et d'afficher des écrans de restriction
- Conçu pour le contrôle parental, UI partiellement imposée par Apple
- Moins de liberté sur le design de l'interstitiel
- Nécessite une justification solide lors de la review Apple

**Recommandation** : partir sur l'Option A pour la V1, qui est la plus éprouvée et la moins risquée pour la validation App Store.

---

### Fonctionnalités attendues

1. **Sélection manuelle des applications**
   - iOS ne permettant pas de lister les apps installées, l'utilisateur sélectionne les apps à surveiller manuellement
   - Utiliser `FamilyActivityPicker` (Screen Time API) si Option B, ou liste manuelle avec champ de recherche si Option A

2. **Système de labellisation**
   - Pour chaque app sélectionnée, l'utilisateur attribue une ou plusieurs problématiques (champ libre + suggestions)
   - Stockage local (CoreData ou UserDefaults)

3. **Génération des Raccourcis**
   - Pour chaque app labelisée, proposer à l'utilisateur d'ajouter le raccourci d'interception via `INUIAddVoiceShortcutViewController`
   - Prévoir un onboarding clair expliquant pourquoi cette étape est nécessaire

4. **Écran interstitiel**
   - Affichage du nom de l'app, des problématiques définies
   - Deux boutons : "Oui, continuer" / "Non, revenir en arrière"
   - "Oui" → ouvre l'app via `UIApplication.shared.open`
   - "Non" → retour à l'écran d'accueil

5. **Statistiques (optionnel / v2)**
   - Nombre de refus par app
   - Historique des choix

---

### Points techniques importants à anticiper

- La **review Apple** sera un point d'attention : bien documenter l'usage des APIs Screen Time si utilisées, et préparer une justification claire du cas d'usage
- L'approche Raccourcis crée une **friction d'installation** (un tap par app) — l'onboarding doit être irréprochable pour ne pas perdre l'utilisateur
- L'interception via Raccourcis n'est **pas hermétique** (Spotlight, liens externes) — à mentionner clairement dans l'app
- Tout le stockage est **100% local**, pas de backend, pas de compte utilisateur
- Prévoir la compatibilité iOS 16+ minimum

---

### Exigences non fonctionnelles importantes

**Vie privée — point critique**
- Zéro télémétrie, zéro analytics tiers (pas de Firebase, Mixpanel ou équivalent)
- Tout reste 100% local, aucune donnée ne quitte l'appareil
- La politique de confidentialité devra expliquer clairement ce que l'app peut techniquement observer — Apple y sera attentif lors de la review

**Performance de l'interstitiel**
- L'écran de friction doit s'afficher en moins d'une seconde — une latence perceptible viderait le concept de son sens et frustrerait l'utilisateur

**UX de la friction**
- Prévoir un mode **pause temporaire** (désactiver la friction sur une app pendant 1h, 24h…) — sans ça, les utilisateurs désinstalleront
- Prévoir un paramètre de **fréquence** : est-ce qu'on interroge l'utilisateur à chaque ouverture, ou seulement la première fois de la journée ?
- Mentionner clairement dans l'app que l'interception n'est pas hermétique (Spotlight, liens externes) — transparence attendue par les utilisateurs

**Cohérence éthique de l'app elle-même**
- Pas de dark patterns dans l'onboarding
- Pas de notifications agressives pour ramener l'utilisateur dans l'app
- Pas de modèle freemium manipulateur si monétisation un jour envisagée

**Accessibilité**
- Compatibilité VoiceOver dès la V1
- Contraste suffisant sur l'interstitiel
- L'écran de friction doit être entièrement navigable sans interaction tactile

**Internationalisation**
- Français et anglais dès la V1 (`Localizable.strings`, reprendre les textes d'Android) ; langue d'app propre aux réglages iOS

**Considérations légales**
- Les icônes des apps tierces doivent être récupérées via le système et non embarquées dans l'app
- La licence open source sera de type **GPL** (et non MIT) pour empêcher toute réutilisation commerciale sans consentement

---

### Stack souhaitée

- Swift natif / SwiftUI
- CoreData pour la persistance
- Le projet sera publié en **open source sous licence GPL**

---

### Ce que je recherche

Un devis et une estimation de charge pour une V1 fonctionnelle, en précisant quelle approche technique (Option A ou B) vous recommandez et pourquoi, et en tenant compte des exigences non fonctionnelles décrites.
