# Pitchs développeurs — Application "Red Flag"

---

## 🤖 PITCH DÉVELOPPEUR ANDROID

### Concept

Je cherche un développeur Android pour créer une application de **friction intentionnelle éthique**. Le principe : quand un utilisateur tente d'ouvrir une application qu'il a lui-même labelisée comme "problématique" (ex : sexisme, exploitation humaine, toxicité…), une fenêtre d'interstitiel s'affiche lui rappelant ses propres valeurs et lui demandant s'il veut vraiment ouvrir l'app. Il choisit "Oui" (l'app se lance) ou "Non" (l'app est tuée).

L'objectif n'est pas de bloquer, mais de **créer une pause consciente** entre l'impulsion et l'action.

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

### Points techniques importants à anticiper

- L'**Accessibility Service** nécessite une permission explicite de l'utilisateur dans les paramètres système — prévoir un onboarding clair pour guider cette activation
- Certains constructeurs (Xiaomi, Huawei, Samsung…) ont des surcouches qui peuvent interférer avec les Accessibility Services — prévoir des tests multi-constructeurs
- L'interstitiel doit s'afficher **avant** que l'app cible soit visible, ce qui nécessite une gestion fine des événements `TYPE_WINDOW_STATE_CHANGED`
- L'app doit gérer les cas de **relance** (si l'utilisateur revient sur l'app labelisée depuis le multitâche)
- Tout le stockage est **100% local**, pas de backend, pas de compte utilisateur

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
- Coder avec des fichiers de traduction dès le départ (strings.xml), même si la V1 est en français uniquement — ça coûte presque rien à faire maintenant et beaucoup à rattraper après

**Considérations légales**
- Les icônes des apps tierces doivent être récupérées via le système (`PackageManager`) et non embarquées dans l'app — c'est la pratique tolérée par les éditeurs
- La licence open source sera de type **GPL** (et non MIT) pour empêcher toute réutilisation commerciale sans consentement

---

### Stack souhaitée

- Kotlin natif
- Architecture MVVM
- Room pour la persistance
- Le projet sera publié en **open source sous licence GPL**

---

### Ce que je recherche

Un devis et une estimation de charge pour une V1 fonctionnelle couvrant les 4 fonctionnalités principales listées ci-dessus, en tenant compte des exigences non fonctionnelles décrites.

---
---

## 🍎 PITCH DÉVELOPPEUR iOS

### Concept

Je cherche un développeur iOS pour créer une application de **friction intentionnelle éthique**. Le principe : quand un utilisateur tente d'ouvrir une application qu'il a lui-même labelisée comme "problématique" (ex : sexisme, exploitation humaine, toxicité…), une fenêtre d'interstitiel s'affiche lui rappelant ses propres valeurs et lui demandant s'il veut vraiment ouvrir l'app. Il choisit "Oui" (l'app se lance) ou "Non" (l'app est fermée).

L'objectif n'est pas de bloquer, mais de **créer une pause consciente** entre l'impulsion et l'action.

---

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
- Coder avec des fichiers de traduction dès le départ (Localizable.strings), même si la V1 est en français uniquement — ça coûte presque rien à faire maintenant et beaucoup à rattraper après

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
