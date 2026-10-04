#!/usr/bin/env bash
# Lance les tests utiles selon ce qu'on vient de modifier, plutôt que toute la suite à chaque fois.
#
#   scripts/test.sh focus Classe [Classe...]   les classes données seulement (unitaires ou sur appareil, détecté tout seul)
#   scripts/test.sh commit                     tous les tests unitaires (quelques secondes, sans appareil)
#   scripts/test.sh auto                       commit + les tests sur appareil concernés par les fichiers modifiés (git)
#   scripts/test.sh full                       avant un push ou une version : unitaires, tout l'appareil, lint
# Les tests sur appareil demandent un émulateur lancé ; l'app est remise (debug) à leur fin, ses données sont vidées au départ.
set -euo pipefail

cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Android/Android Studio/jbr}"
GRADLE=(./gradlew --console=plain -q)
SRC=app/src
PKG_DIR=java/fr/conscience/numerique

# Chemin d'une classe de test : $1 = nom (ex. FrictionGateTest) ; affiche "unit" ou "device" puis le nom complet.
locate() {
    local f
    f=$(find "$SRC/test" "$SRC/androidTest" -name "$1.kt" | head -1)
    [ -n "$f" ] || { echo "Classe de test introuvable : $1" >&2; exit 2; }
    local kind=unit
    [[ "$f" == "$SRC/androidTest/"* ]] && kind=device
    local rel=${f#"$SRC"/*/"java/"}
    echo "$kind ${rel%.kt}" | tr '/' '.'
}

run_site() { # le site statique (docs/) : liens, ancres, langues, FAQ à jour. Rapide ; ignoré sans Node.
    command -v node >/dev/null || { echo ">> site ignoré (Node introuvable)"; return 0; }
    echo ">> site"
    node scripts/site.mjs check
}

run_unit() { # $@ = noms de classes (vide = tout)
    local args=()
    for c in "$@"; do args+=(--tests "*.$c"); done
    echo ">> tests unitaires ${*:-(tous)}"
    "${GRADLE[@]}" :app:testDebugUnitTest "${args[@]}"
}

# Choisit l'appareil des tests sur appareil. Ils vident les données de l'app : on ne les lance jamais par surprise sur un vrai
# téléphone branché en même temps qu'un émulateur. Un téléphone seul, ou un appareil choisi avec ANDROID_SERIAL, est accepté.
pick_device() {
    local devices count
    devices=$(adb devices | awk '$2 == "device" { print $1 }')
    count=$(printf '%s\n' "$devices" | grep -c . || true)
    if [ "$count" -eq 0 ]; then
        echo "Aucun émulateur ou téléphone connecté." >&2
        exit 3
    fi
    if [ -n "${ANDROID_SERIAL:-}" ]; then
        return
    fi
    if [ "$count" -gt 1 ]; then
        local emulator
        emulator=$(printf '%s\n' "$devices" | grep '^emulator-' | head -1 || true)
        if [ -z "$emulator" ]; then
            echo "Plusieurs appareils connectés et aucun émulateur : choisir avec ANDROID_SERIAL=<numéro de série> (adb devices)." >&2
            exit 3
        fi
        echo ">> plusieurs appareils connectés : les tests ne tournent que sur $emulator (les données des autres ne sont pas touchées)"
        export ANDROID_SERIAL=$emulator
    fi
}

APP_ID=fr.conscience.numerique

# Gradle désinstalle l'app à la fin des tests sur appareil : on la remet (en debug), et on réactive son service d'accessibilité
# s'il l'était (les réglages « secure » ne sont modifiables que sur un émulateur ou un appareil de développement ; sinon, sans effet).
restore_app() { # $1 = services d'accessibilité actifs avant les tests
    echo ">> réinstallation de l'app"
    "${GRADLE[@]}" :app:installDebug
    if [[ "$1" == *"$APP_ID"* ]]; then
        adb shell settings put secure enabled_accessibility_services "$1" >/dev/null 2>&1 || true
        adb shell settings put secure accessibility_enabled 1 >/dev/null 2>&1 || true
    fi
}

run_device() { # $@ = noms complets de classes (vide = tout)
    pick_device
    local args=()
    if [ $# -gt 0 ]; then
        local IFS=,
        args+=("-Pandroid.testInstrumentationRunnerArguments.class=$*")
    fi
    echo ">> tests sur appareil ${*:-(tous)}"
    local services status=0
    services=$(adb shell settings get secure enabled_accessibility_services 2>/dev/null | tr -d '\r' || true)
    # Données vidées plutôt qu'app désinstallée : le même point de départ (rien ne doit rester de l'utilisation précédente).
    adb shell pm clear "$APP_ID" >/dev/null 2>&1 || true
    "${GRADLE[@]}" :app:connectedDebugAndroidTest "${args[@]}" || status=$?
    restore_app "$services"
    return $status
}

# Tests sur appareil à rejouer d'après les fichiers modifiés (non commités + commits non poussés).
# Affiche un nom de classe par ligne, ou ALL. Les fichiers sans test sur appareil sont signalés sur la sortie d'erreur.
device_classes_for_changes() {
    local files
    files=$( { git diff --name-only HEAD; git diff --name-only '@{upstream}..HEAD' 2>/dev/null; git ls-files --others --exclude-standard; } | sort -u)
    local out=() uncovered=()
    local matched=0
    add() { out+=("fr.conscience.numerique.$1"); matched=1; }
    while IFS= read -r f; do
        matched=0
        case "$f" in
            "") ;;
            # Build, manifeste, dépendances : tout.
            *build.gradle.kts|*AndroidManifest.xml|gradle/*|*/libs.versions.toml) echo ALL; return ;;
            # Un test modifié : lui seul. Les tests unitaires sont de toute façon rejoués.
            */androidTest/*Test.kt) out+=("$(locate "$(basename "$f" .kt)" | cut -d' ' -f2)") ;;
            */androidTest/*ViewModelEnv.kt) add ui.StatsViewModelsTest; add ui.AppListViewModelsTest; add ui.OnboardingViewModelsTest; add ui.ProblemViewModelsTest ;;
            */src/test/*|*.md|docs/*|scripts/*|.gitignore|TODO.md|LICENSE|licenses/*) ;;
            # ViewModels : leur test (sans écran). `;;&` : on continue, un ViewModel peut aussi concerner un écran plus bas.
            */ui/StatsViewModel.kt|*/ui/AppStatsViewModel.kt) add ui.StatsViewModelsTest ;;&
            */ui/MainViewModel.kt|*/ui/AppPickerViewModel.kt) add ui.AppListViewModelsTest ;;&
            */ui/Onboarding*ViewModel.kt) add ui.OnboardingViewModelsTest ;;&
            */ui/ProblemsManagerViewModel.kt|*/ui/ProblemDetailViewModel.kt|*/ui/AppDetailViewModel.kt) add ui.ProblemViewModelsTest ;;&
            # Ce qui sert à construire tous les ViewModels, ou la base qu'ils lisent.
            */ui/ViewModelFactory.kt|*/AppContainer.kt|*/data/AppRepository.kt|*/data/Daos.kt|*/data/Entities.kt)
                add ui.StatsViewModelsTest; add ui.AppListViewModelsTest; add ui.OnboardingViewModelsTest; add ui.ProblemViewModelsTest; add ui.HomeAndStatsScreensTest ;;&
            */ui/MainActivity.kt|*/ui/OnboardingPermissionActivity.kt) add ui.ForcedOnboardingTest ;;&
            # Accueil et statistiques, à l'écran.
            */ui/MainActivity.kt|*/ui/StatsActivity.kt|*/ui/AppListAdapter.kt|*/ui/StatsAdapter.kt|*/ui/MainViewModel.kt|*/ui/StatsViewModel.kt|*/layout/activity_main.xml|*/layout/activity_stats.xml|*/layout/item_app.xml|*/layout/item_stats_app.xml)
                add ui.HomeAndStatsScreensTest ;;&
            app/schemas/*|*/data/AppDatabase.kt|*/data/Entities.kt) add data.MigrationTest; add data.AppRepositoryTest ;;
            */data/AppRepository.kt|*/data/Daos.kt) add data.AppRepositoryTest ;;
            */data/SettingsStore.kt) add data.SettingsStoreTest; add ui.ForcedOnboardingTest; add ui.DebugModeScreenTest ;;&
            */data/InstalledAppsProvider.kt) add data.InstalledAppsProviderTest ;;
            */AppContainer.kt|*/ui/InterstitialActivity.kt|*/service/Friction*.kt|*/layout/activity_interstitial.xml|*/layout/item_problem_pill.xml|*/ui/ProblemPillAdapter.kt)
                add ui.InterstitialActivityTest ;;
            */ui/BottomNav.kt|*/layout/view_bottom_nav.xml|*/ui/AppStats*.kt|*/ui/AppDetail*.kt|*/ui/ProblemDetail*.kt|*/layout/activity_app_stats.xml|*/layout/activity_app_detail.xml|*/layout/activity_problem_detail.xml)
                add ui.DetailScreensTest ;;
            # Parcours d'accueil : étapes, boutons « Passer », et ce que leurs ViewModels calculent.
            */ui/Onboarding*.kt|*/layout/activity_onboarding*.xml|*/layout/item_onboarding*.xml) add ui.OnboardingSkipTest ;;&
            # Fenêtres de saisie des problématiques.
            */ui/Dialogs.kt|*/layout/dialog_new_problem.xml) add ui.EditProblemDialogTest ;;
            # Mode debug (sept appuis sur la version) : le réglage lui-même et la section des Réglages.
            */data/DebugUnlock.kt) add ui.DebugModeScreenTest ;;&
            # Avertissement avant les réglages d'accessibilité (exigé par Google Play) : bandeau, Réglages, fenêtre.
            */ui/Dialogs.kt|*/ui/OnboardingPermissionActivity.kt|*/ui/MainActivity.kt|*/service/*Accessibility*.kt|*/res/xml/accessibility_service_config.xml) add ui.AccessibilityDisclosureTest ;;&
            # FAQ (et les Réglages qui y mènent, la carte de thème qu'elle partage avec d'autres écrans).
            */ui/Faq*.kt|*/data/Faq.kt|*/layout/activity_faq.xml|*/layout/item_faq_*.xml|*/ui/SettingsActivity.kt|*/layout/activity_settings.xml|*/ui/ThemeCard.kt|*/ui/CardStyle.kt|*/layout/item_manager_theme.xml)
                add ui.FaqScreenTest ;;
            # Couverts par les tests unitaires, déjà joués.
            */data/Stats.kt|*/data/ProblemCatalog.kt|*/util/*|*/ui/ProblemLabels.kt|*/res/values*/strings.xml|*/res/raw*/faq.xml) ;;
            *) [ "$matched" -eq 1 ] || uncovered+=("$f") ;;
        esac
    done <<< "$files"
    if [ ${#uncovered[@]} -gt 0 ]; then
        echo "!! sans test automatique (à vérifier à la main) :" >&2
        printf '   %s\n' "${uncovered[@]}" >&2
    fi
    [ ${#out[@]} -eq 0 ] || printf '%s\n' "${out[@]}" | sort -u
}

mode=${1:-}
[ $# -gt 0 ] && shift
case "$mode" in
    focus)
        [ $# -gt 0 ] || { echo "Usage : $0 focus Classe [Classe...]" >&2; exit 1; }
        units=(); devices=()
        for name in "$@"; do
            read -r kind full < <(locate "$name")
            if [ "$kind" = unit ]; then units+=("$name"); else devices+=("$full"); fi
        done
        [ ${#units[@]} -eq 0 ] || run_unit "${units[@]}"
        [ ${#devices[@]} -eq 0 ] || run_device "${devices[@]}"
        ;;
    commit)
        run_unit
        run_site
        ;;
    auto)
        run_unit
        run_site
        mapfile -t classes < <(device_classes_for_changes)
        if [ ${#classes[@]} -eq 0 ] || [ -z "${classes[0]}" ]; then
            echo ">> aucun test sur appareil concerné par les fichiers modifiés"
        elif [ "${classes[0]}" = ALL ]; then
            run_device
        else
            run_device "${classes[@]}"
        fi
        ;;
    full)
        run_unit
        run_site
        run_device
        echo ">> lint"
        "${GRADLE[@]}" :app:lintDebug
        ;;
    *)
        sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'
        exit 1
        ;;
esac
echo "OK"
