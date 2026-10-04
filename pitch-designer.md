# Pitch designer — Application « Red Flag » : design de l'application et logo

## Le projet en deux phrases

**Red Flag – Ethical app check** est une application Android (iOS ensuite) qui crée une **pause consciente** : quand on ouvre une
application qu'on a soi-même signalée comme problématique (réseaux sociaux addictifs, exploitation humaine, atteinte à la vie
privée, impact environnemental…), un écran demande « Est-ce vraiment important de l'ouvrir maintenant ? ». Rien n'est
interdit : l'utilisateur choisit, mais il choisit **en conscience**.

Tout reste sur le téléphone : pas de compte, pas de publicité, pas de collecte de données. Le code est ouvert (GPL v3).
Français et anglais dès le premier jour.

## Pourquoi un designer

L'application fonctionne : elle a été conçue par une personne seule, sans identité graphique professionnelle. L'interface
actuelle (Material 3, bleu nuit, ambre) est correcte mais **neutre**, et le logo est un simple symbole « pause » blanc sur un fond
bleu. Je cherche quelqu'un qui donne à Red Flag **une vraie identité** et affine chaque écran, surtout celui qui compte :
l'interruption.

## Ce qui rend ce projet particulier

1. **Une pause, pas un blocage.** L'écran d'interruption doit calmer, pas culpabiliser. Il apparaît à un moment où la personne
   est pressée d'ouvrir une app : il doit se lire en une seconde et laisser une vraie place au « Non ».
2. **Le nom dit « alerte », le ton doit rester serein.** « Red Flag » évoque le rouge et l'alarme ; l'application, elle, ne
   juge pas. Comment l'identité tient-elle cette tension ? C'est la question centrale du logo. (Aujourd'hui : le rouge n'est
   quasiment pas utilisé ; l'identité est bleu nuit et ambre.) Je suis ouvert à une autre direction, si elle est argumentée.
3. **Une app éthique doit l'être jusque dans son design** : aucun dark pattern, aucune urgence artificielle, aucune astuce
   visuelle pour pousser à rester ou à cliquer. Le bouton qui ferme l'app visée est mis en avant ; celui qui l'ouvre reste
   accessible et honnête, sans être caché ni honteux.
4. **Un outil de confiance.** L'app demande une permission sensible (le service d'accessibilité d'Android). L'écran qui
   l'explique doit rassurer sans minimiser : ce qu'elle voit (le nom de l'app au premier plan), ce qu'elle ne voit pas (le
   contenu de l'écran), et que rien ne quitte le téléphone.

## À qui l'app s'adresse

Des adultes qui voudraient reprendre la main sur leur usage du téléphone, et en particulier sur les apps dont ils
désapprouvent le fonctionnement ou les valeurs. Pas de public technique : on peut ne jamais être entré dans les réglages
d'accessibilité d'Android. L'app est utilisée par petites touches, souvent debout ou en déplacement.

## Ce que je demande

### 1. Le logo et l'identité
- **Le logo** (symbole + version avec le nom « Red Flag », et si possible avec le sous-titre « Ethical app check »), lisible
  de 16 px (favicon du site) à une affiche.
- **L'icône de l'application Android** : icône adaptative (fond et premier plan séparés, zone sûre de 66 dp sur 108 dp),
  **version monochrome** pour les icônes thématiques d'Android 13+, et la même icône carrée pour iOS (1024 × 1024, sans
  transparence).
- **Une courte charte** : palette (avec rôles et contrastes), typographies, usage du logo (zone de protection, fonds interdits),
  ton des illustrations ou des pictogrammes s'il y en a. Les polices actuelles sont **Bricolage Grotesque** (titres) et **Inter**
  (textes), toutes deux libres (SIL OFL) ; elles sont modifiables si vous avez mieux, à condition de rester libres de droits.

### 2. Le design de l'application
Une quinzaine d'écrans, déjà réalisés en code, à reprendre, simplifier ou refondre. Les captures sont dans
`docs/assets/img/fr/` et `docs/assets/img/en/` du dépôt :

| Groupe | Écrans |
|---|---|
| Accueil guidé | Bienvenue ; choix des problématiques ; choix des apps concernées ; détail d'une app ; explication de la permission d'accessibilité |
| Au quotidien | Liste des apps (filtres : toutes, signalées, non signalées) ; détail d'une app ; liste des problématiques |
| **L'interruption** | **L'écran plein écran « Une seconde. »** : l'app visée, ses problématiques, la question, deux boutons, un lien « ne plus me demander pendant 1 heure » |
| Suivi | Statistiques (graphique sur 14 jours, jour / mois / année) ; détail d'une app |
| Autour | Réglages ; « Comment ça marche » (FAQ en accordéon) ; fenêtres de saisie |

À prévoir : l'état vide, le chargement et les erreurs ; le mode sombre (aujourd'hui seule la version claire est dessinée,
l'interruption est déjà sombre) ; les écrans en français **et** en anglais (les textes français sont en général plus longs) ; la lecture avec **TalkBack** et le grossissement des textes à 200 %.

### 3. L'image de la publication
- Pour **Google Play** : icône 512 × 512, image de présentation 1024 × 500, captures d'écran habillées (téléphone, titre court),
  éventuellement une vidéo de présentation.
- Pour le **site web** (statique, `docs/`) : l'identité reprise sur le site actuel (aujourd'hui construit par moi, sobre), une image
  de partage (réseaux sociaux) et les captures.

## Contraintes à respecter

- **Android d'abord, Material 3** : on peut s'écarter de Material pour l'identité, mais les composants doivent rester
  reconnaissables et accessibles. iOS viendra ensuite, avec les mêmes bases.
- **Accessibilité** : contraste WCAG AA au minimum (AAA pour l'écran d'interruption), cibles tactiles de 48 dp,
  compatibilité TalkBack et daltonisme (ne jamais signifier uniquement par la couleur ; le graphique des statistiques distingue
  aujourd'hui trois états par le vert, l'ambre et le bleu, à revoir de ce point de vue).
- **Poids et technique** : pictogrammes en vecteur (SVG, importés en `VectorDrawable`) ; aucune image lourde, l'app pèse 3,4 Mo
  et doit rester légère.
- **Icônes d'apps tierces** : elles viennent du système, on n'en embarque et n'en retouche aucune.
- **Licence** : le code est en GPL v3. Je souhaite que le logo et les visuels soient **libres d'utilisation avec le projet**
  (par exemple CC BY-SA ou une licence équivalente), mais que le **nom et le logo** restent protégés pour le projet officiel (on
  pourra forker le code, pas se faire passer pour Red Flag). C'est à discuter ensemble, avant le contrat.
- **Vérification du nom** : « Red Flag » est un nom courant, et d'autres apps le portent peut-être. Si votre recherche
  d'antériorité d'un logo ou d'un nom vous alerte, dites-le-moi.

## Ce que je fournis

Le dépôt complet (code et captures), l'accès à une version de test sur téléphone, les textes en français et en anglais, la liste
des 23 problématiques (7 thèmes) avec leurs pictogrammes éventuels à créer, et ma disponibilité pour des échanges réguliers.
Je suis ouvert aux propositions de méthode (ateliers, moodboard, allers-retours).

## Ce que j'attends de votre réponse

1. Votre **portfolio**, avec un projet proche (application mobile, identité simple et sobre, cause éthique ou sociale).
2. Votre lecture de la tension « Red Flag / ton serein » : une première intuition suffit.
3. Une **proposition de déroulé** : étapes, nombre d'allers-retours, livrables, format (Figma, SVG, PNG, PDF).
4. Un **devis** (logo et identité d'une part, design de l'application d'autre part, éventuellement l'image de la publication) et
   un **délai**.
5. Vos conditions sur la **propriété et la licence** des créations.

*Contact : [à compléter]. Budget indicatif : [à compléter].*
