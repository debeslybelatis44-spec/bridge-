# LOTATO PRO - Application Android Native

Application Android native pour la gestion de loterie, avec impression directe sur les terminaux POS Sunmi (V2S et compatibles).

## Structure du projet

```
app/src/main/java/com/lotato/pro/
├── activities/
│   ├── LoginActivity.java          # Écran de connexion
│   ├── MainActivity.java           # Navigation principale
│   ├── DrawSelectionFragment.java  # Sélection du tirage
│   ├── BettingActivity.java        # Interface de jeu (pari)
│   ├── HistoryFragment.java        # Historique des tickets
│   ├── ReportsFragment.java        # Rapports journaliers
│   └── WinnersFragment.java        # Tickets gagnants
├── models/
│   ├── Bet.java                    # Modèle d'un pari
│   ├── Draw.java                   # Modèle d'un tirage
│   ├── Ticket.java                 # Modèle d'un ticket
│   └── AppSession.java             # Gestion de session locale
├── api/
│   └── ApiService.java             # Connexion à l'API REST
├── print/
│   └── SunmiPrintHelper.java       # Impression via SDK Sunmi
└── utils/
    └── GameEngine.java             # Logique de jeu (Borlette, Lotto, etc.)
```

## Connexion à votre API

L'URL de l'API est configurée dans `ApiService.java` :
```java
private static final String BASE_URL = "https://lotato1.onrender.com/api";
```

Modifiez cette ligne si votre serveur change d'adresse.

## Jeux supportés

- **Borlette** (2 chiffres)
- **Lotto 3, 4, 5** (3, 4, 5 chiffres)
- **Mariage** (4 chiffres, format XX&XX)
- **Mariage Auto** (généré depuis les Borlettes du panier)
- **Lotto 4 Auto / Lotto 5 Auto** (généré automatiquement)
- **BO** (doubles : 00, 11, 22... 99)
- **GRAP** (triples : 000, 111... 999)
- **N0 à N9** (tous les numéros finissant par le chiffre choisi)
- **Mariages gratuits** (générés automatiquement selon les paliers de mise)

## Impression Sunmi

L'impression utilise le SDK Sunmi Inner Printer :
- Compatible : Sunmi V2S, V2 Pro, T2, P2, L2, et tous les POS Sunmi avec imprimante intégrée
- Format : 58mm / 80mm
- Le helper `SunmiPrintHelper.java` gère la connexion et la déconnexion automatique

## Compiler avec GitHub Actions (sans Android Studio)

### Étapes :

1. **Créer un dépôt GitHub** (public ou privé)
2. **Pousser ce dossier** sur le dépôt :
   ```bash
   git init
   git add .
   git commit -m "LOTATO PRO Android v6.0"
   git branch -M main
   git remote add origin https://github.com/VOTRE_NOM/lotato-android.git
   git push -u origin main
   ```
3. **GitHub Actions** se déclenche automatiquement
4. Allez dans **Actions** → cliquez sur le workflow → **Artifacts** → téléchargez `lotato-pro-apk`
5. Installez l'APK sur votre Sunmi V2S

### Pour créer une release (tag) :
```bash
git tag v6.0
git push origin v6.0
```
L'APK sera attaché automatiquement à la release GitHub.

## Installer sur Sunmi V2S

1. Transférer l'APK via USB ou télécharger depuis GitHub
2. Activer **Sources inconnues** dans Paramètres → Sécurité
3. Ouvrir le fichier APK et installer
4. Connecter avec vos identifiants habituels

## Modifier l'URL de l'API

Si vous changez l'hébergement de votre serveur, modifiez dans `ApiService.java` :
```java
private static final String BASE_URL = "https://VOTRE_NOUVEAU_SERVEUR.com/api";
```

## Notes techniques

- **minSdk** : 21 (Android 5.0+) — compatible avec tous les Sunmi POS
- **targetSdk** : 34 (Android 14)
- Le SDK Sunmi est récupéré depuis JitPack : `com.github.sunmi:printersdk:1.0.14`
- La session est stockée localement en `SharedPreferences`
- Les requêtes réseau utilisent OkHttp 4 avec timeout de 30 secondes
