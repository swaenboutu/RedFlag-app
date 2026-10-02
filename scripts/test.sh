#!/usr/bin/env bash
# Lance les tests utiles selon ce qu'on vient de modifier, plutôt que toute la suite à chaque fois.
#
#   scripts/test.sh focus Classe [Classe...]   les classes données seulement (unitaires ou sur appareil, détecté tout seul)
#   scripts/test.sh commit                     tous les tests unitaires (quelques secondes, sans appareil)
#   scripts/test.sh auto                       commit + les tests sur appareil concernés par les fichiers modifiés (git)
#   scripts/test.sh full                       avant un push ou une version : unitaires, tout l'appareil, lint
#
# Les tests sur appareil demandent un émulateur lancé. Si l'installation échoue (signature différente) :
#   adb uninstall fr.conscience.numerique   (efface les données de l'app)
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

run_unit() { # $@ = noms de classes (vide = tout)
    local args=()
    for c in "$@"; do args+=(--tests "*.$c"); done
    echo ">> tests unitaires ${*:-(tous)}"
    "${GRADLE[@]}" :app:testDebugUnitTest "${args[@]}"
}

run_device() { # $@ = noms complets de classes (vide = tout)
    adb get-state >/dev/null 2>&1 || { echo "Aucun émulateur ou téléphone connecté." >&2; exit 3; }
    local args=()
    if [ $# -gt 0 ]; then
        local IFS=,
        args+=("-Pandroid.testInstrumentationRunnerArguments.class=$*")
    fi
    echo ">> tests sur appareil ${*:-(tous)}"
    "${GRADLE[@]}" :app:connectedDebugAndroidTest "${args[@]}"
}

# Tests sur appareil à rejouer d'après les fichiers modifiés (non commités + dernier commit non poussé).
device_classes_for_changes() {
    local files
    files=$( { git diff --name-only HEAD; git diff --name-only '@{upstream}..HEAD' 2>/dev/null; git ls-files --others --exclude-standard; } | sort -u)
    local out=()
    add() { out+=("fr.conscience.numerique.$1"); }
    while IFS= read -r f; do
        case "$f" in
            *build.gradle.kts|*AndroidManifest.xml|gradle/*|*/androidTest/*Test.kt)
                # Build ou manifeste : tout. Un test modifié : lui seul.
                if [[ "$f" == */androidTest/*Test.kt ]]; then
                    local n; n=$(basename "$f" .kt)
                    out+=("$(locate "$n" | cut -d' ' -f2)")
                else
                    echo ALL; return
                fi ;;
            app/schemas/*|*/data/Migrations.kt|*/data/Entities.kt|*/data/AppDatabase.kt) add data.MigrationTest; add data.AppRepositoryTest ;;
            */data/AppRepository.kt|*/data/*Dao.kt) add data.AppRepositoryTest ;;
            */data/SettingsStore.kt) add data.SettingsStoreTest ;;
            */data/InstalledAppsProvider.kt) add data.InstalledAppsProviderTest ;;
            */AppContainer.kt|*/ui/InterstitialActivity.kt|*/service/Friction*.kt|*/res/layout/activity_interstitial.xml) add ui.InterstitialActivityTest ;;
            */ui/BottomNav.kt|*/ui/AppStatsActivity.kt|*/ui/AppDetailActivity.kt|*/ui/ProblemDetailActivity.kt) add ui.DetailScreensTest ;;
        esac
    done <<< "$files"
    printf '%s\n' "${out[@]}" | sort -u
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
        ;;
    auto)
        run_unit
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
        run_device
        echo ">> lint"
        "${GRADLE[@]}" :app:lintDebug
        ;;
    *)
        sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'
        exit 1
        ;;
esac
echo "OK"
