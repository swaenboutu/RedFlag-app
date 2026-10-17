#!/usr/bin/env bash
# Runs the tests that matter for what was just changed, rather than the whole suite every time.
#
#   scripts/test.sh focus Class [Class...]    the given classes only (unit or device, detected automatically)
#   scripts/test.sh commit                    all unit tests (a few seconds, no device)
#   scripts/test.sh auto                      commit + the device tests concerned by the modified files (git)
#   scripts/test.sh full                      before a push or a release: unit tests, lint, the whole device suite
# The device tests need a running emulator; the app's data is cleared before them, and the app is put back (debug) after.
set -euo pipefail

cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Android/Android Studio/jbr}"
# Git Bash (Windows) would turn the device paths given to adb and Gradle (/data/local/tmp/...) into Windows paths.
export MSYS_NO_PATHCONV=1
GRADLE=(./gradlew --console=plain -q)
SRC=app/src

# Path of a test class: $1 = name (e.g. FrictionGateTest); prints "unit" or "device", then the full name.
locate() {
    local f
    f=$(find "$SRC/test" "$SRC/androidTest" -name "$1.kt" | head -1)
    [ -n "$f" ] || { echo "Classe de test introuvable : $1" >&2; exit 2; }
    local kind=unit
    [[ "$f" == "$SRC/androidTest/"* ]] && kind=device
    local rel=${f#"$SRC"/*/"java/"}
    echo "$kind ${rel%.kt}" | tr '/' '.'
}

run_site() { # the static website (docs/): links, anchors, languages, FAQ up to date. Fast; skipped without Node.
    command -v node >/dev/null || { echo ">> site ignoré (Node introuvable)"; return 0; }
    echo ">> site"
    node scripts/site.mjs check
}

run_unit() { # $@ = class names (empty = all)
    local args=()
    for c in "$@"; do args+=(--tests "*.$c"); done
    echo ">> tests unitaires ${*:-(tous)}"
    "${GRADLE[@]}" :app:testDebugUnitTest "${args[@]}"
}

# Picks the device for the device tests. They clear the app's data: they never run by surprise on a real phone plugged in at the
# same time as an emulator. A phone alone, or a device chosen with ANDROID_SERIAL, is accepted.
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

APP_ID=app.redflag.android
FIXTURE_ID=app.redflag.fixture
TEST_LIST=/data/local/tmp/redflag-tests.txt

# The device animations are left as they are: turning them off made most tests faster, but OnboardingSkipTest then sometimes
# stalled for about 20 s in a full run, which cancelled the gain.

# Gradle uninstalls the app at the end of the device tests: it is put back (debug), and its accessibility service turned back on
# if it was on (the "secure" settings can only be changed on an emulator or a development device; elsewhere, without effect).
# (Leaving the APKs installed after the run, or installing the test apps in the same Gradle call as the tests, saved a few seconds
# but made OnboardingSkipTest stall for minutes in full runs: both were tried and dropped.)
restore_app() { # $1 = accessibility services enabled before the tests
    echo ">> réinstallation de l'app"
    "${GRADLE[@]}" :app:installDebug
    if [[ "$1" == *"$APP_ID"* ]]; then
        adb shell settings put secure enabled_accessibility_services "$1" >/dev/null 2>&1 || true
        adb shell settings put secure accessibility_enabled 1 >/dev/null 2>&1 || true
    fi
}

run_device() { # $@ = full class names (empty = all)
    pick_device
    local args=()
    if [ $# -gt 0 ]; then
        # The class list goes through a file on the device: the Android Gradle plugin keeps only the first of several classes
        # given with `-e class a,b`.
        mkdir -p build
        printf '%s\n' "$@" > build/device-tests.txt
        adb push build/device-tests.txt "$TEST_LIST" >/dev/null 2>&1
        args+=("-Pandroid.testInstrumentationRunnerArguments.testFile=$TEST_LIST")
    fi
    echo ">> tests sur appareil ${*:-(tous)}"
    # Ordinary apps that the screens list (a bare emulator only has system apps, which are hidden); removed at the end.
    "${GRADLE[@]}" :fixtures:installADebug :fixtures:installBDebug
    local services status=0
    services=$(adb shell settings get secure enabled_accessibility_services 2>/dev/null | tr -d '\r' || true)
    # Data cleared rather than app uninstalled: the same starting point (nothing must remain from the previous use).
    adb shell pm clear "$APP_ID" >/dev/null 2>&1 || true
    # Before Android 9 the test runner refuses to reinstall an app that is already there (INSTALL_FAILED_ALREADY_EXISTS): remove it first.
    local sdk
    sdk=$(adb shell getprop ro.build.version.sdk 2>/dev/null | tr -dc '0-9')
    if [ "${sdk:-99}" -lt 28 ]; then adb uninstall "$APP_ID" >/dev/null 2>&1 || true; fi
    "${GRADLE[@]}" :app:connectedDebugAndroidTest "${args[@]}" || status=$?
    for fixture in a b; do adb uninstall "$FIXTURE_ID.$fixture" >/dev/null 2>&1 || true; done
    adb shell "rm -f $TEST_LIST" >/dev/null 2>&1 || true
    restore_app "$services"
    return $status
}

# Device tests to run again for the modified files (not committed + commits not pushed).
# Prints one class name per line, or ALL. The files without a device test are reported on the error output.
device_classes_for_changes() {
    local files
    files=$( { git diff --name-only HEAD; git diff --name-only '@{upstream}..HEAD' 2>/dev/null; git ls-files --others --exclude-standard; } | sort -u)
    local out=() uncovered=()
    local matched=0
    add() { out+=("app.redflag.$1"); matched=1; }
    local all_view_models="ui.StatsViewModelsTest ui.AppListViewModelsTest ui.OnboardingViewModelsTest ui.ProblemViewModelsTest ui.HomeViewModelTest"
    while IFS= read -r f; do
        matched=0
        case "$f" in
            "") ;;
            # Build, manifest, dependencies, test apps: everything.
            *build.gradle.kts|*AndroidManifest.xml|gradle/*|*/libs.versions.toml|fixtures/*) echo ALL; return ;;
            # A modified test: that one only. The unit tests are run again anyway.
            */androidTest/*Test.kt) out+=("$(locate "$(basename "$f" .kt)" | cut -d' ' -f2)") ;;
            */androidTest/*ViewModelEnv.kt) for c in $all_view_models; do add "$c"; done ;;
            */src/test/*|*.md|docs/*|scripts/*|.gitignore|LICENSE|licenses/*) ;;
            # Covered by the unit tests, already run.
            */data/Stats.kt|*/data/ProblemCatalog.kt|*/util/*|*/ui/common/ProblemLabels.kt|*/res/values*/strings.xml|*/res/raw*/faq.xml) ;;
            # From here on, `;;&` goes on testing the next patterns: one file can concern several tests.
            *)
                case "$f" in
                    # ViewModels: their test (no screen).
                    */ui/stats/StatsViewModel.kt|*/ui/stats/AppStatsViewModel.kt) add ui.StatsViewModelsTest ;;&
                    */ui/apps/MainViewModel.kt|*/ui/apps/AppPickerViewModel.kt) add ui.AppListViewModelsTest ;;&
                    */ui/onboarding/Onboarding*ViewModel.kt) add ui.OnboardingViewModelsTest ;;&
                    */ui/problems/ProblemsManagerViewModel.kt|*/ui/problems/ProblemDetailViewModel.kt|*/ui/apps/AppDetailViewModel.kt) add ui.ProblemViewModelsTest ;;&
                    */ui/home/HomeViewModel.kt|*/ui/home/HomeStats.kt) add ui.HomeViewModelTest ;;&
                    # What builds every ViewModel, or the database they read.
                    */ui/common/ViewModelFactory.kt|*/AppContainer.kt|*/data/AppRepository.kt|*/data/Daos.kt|*/data/Entities.kt)
                        for c in $all_view_models; do add "$c"; done; add ui.HomeAndStatsScreensTest ;;&
                    # Database, settings, installed apps.
                    */app/schemas/*|*/data/AppDatabase.kt|*/data/Entities.kt) add data.MigrationTest; add data.AppRepositoryTest ;;&
                    */data/AppRepository.kt|*/data/Daos.kt) add data.AppRepositoryTest ;;&
                    */data/SettingsStore.kt) add data.SettingsStoreTest; add ui.ForcedOnboardingTest; add ui.DebugModeScreenTest ;;&
                    */data/InstalledAppsProvider.kt) add data.InstalledAppsProviderTest ;;&
                    # Home screen (the launcher), its drawer (Settings, FAQ, About) and the welcome tour it may open.
                    */ui/home/HomeActivity.kt|*/ui/home/PauseSheet.kt|*/layout/activity_home.xml|*/ui/common/AppDrawer.kt|*/layout/view_drawer.xml)
                        add ui.HomeTabTest; add ui.ForcedOnboardingTest; add ui.DebugModeScreenTest; add ui.FaqScreenTest ;;&
                    */ui/common/BottomNav.kt|*/layout/view_bottom_nav.xml) add ui.HomeTabTest; add ui.DetailScreensTest; add ui.HomeAndStatsScreensTest ;;&
                    # Apps and Statistics tabs.
                    */ui/apps/MainActivity.kt|*/ui/apps/AppListAdapter.kt|*/layout/activity_main.xml|*/layout/item_app.xml|*/ui/stats/StatsActivity.kt|*/ui/stats/StatsAdapter.kt|*/layout/activity_stats.xml|*/layout/item_stats_app.xml)
                        add ui.HomeAndStatsScreensTest ;;&
                    # Detail screens: an app, its statistics, an issue.
                    */ui/apps/AppDetail*.kt|*/ui/apps/LinkedAppsList.kt|*/ui/stats/AppStats*.kt|*/ui/problems/ProblemDetail*.kt|*/layout/activity_app_detail.xml|*/layout/activity_app_stats.xml|*/layout/activity_problem_detail.xml|*/layout/item_linked_app.xml)
                        add ui.DetailScreensTest ;;&
                    # Welcome tour: steps, "Skip" buttons, and what their ViewModels compute.
                    */ui/onboarding/*.kt|*/layout/activity_onboarding*.xml|*/layout/item_onboarding*.xml|*/layout/view_onboarding_topbar.xml) add ui.OnboardingSkipTest ;;&
                    */ui/onboarding/OnboardingActivity.kt|*/ui/onboarding/OnboardingPermissionActivity.kt) add ui.ForcedOnboardingTest ;;&
                    # The interruption screen and the service that shows it.
                    */AppContainer.kt|*/ui/interruption/*.kt|*/service/Friction*.kt|*/layout/activity_interstitial.xml) add ui.InterstitialActivityTest ;;&
                    # Bottom sheets (the edit sheet and their common base).
                    */ui/common/EditProblemSheet.kt|*/layout/sheet_edit_problem.xml|*/ui/common/OptionsSheet.kt) add ui.EditProblemSheetTest ;;&
                    # Warning before the accessibility settings (required by Google Play): Home, Settings, the window.
                    */ui/common/Dialogs.kt|*/ui/onboarding/OnboardingPermissionActivity.kt|*/ui/home/HomeActivity.kt|*/ui/settings/SettingsActivity.kt|*/service/*Accessibility*.kt|*/res/xml/accessibility_service_config.xml)
                        add ui.AccessibilityDisclosureTest ;;&
                    # FAQ (and the Settings, the theme card it shares with other screens).
                    */ui/settings/Faq*.kt|*/data/Faq.kt|*/layout/activity_faq.xml|*/layout/item_faq_*.xml|*/ui/settings/SettingsActivity.kt|*/layout/activity_settings.xml|*/ui/common/ThemeCard.kt|*/ui/common/CardStyle.kt|*/layout/item_manager_theme.xml)
                        add ui.FaqScreenTest ;;&
                    # Debug mode (seven taps on the version): the setting itself and the section of the Settings.
                    */data/DebugUnlock.kt|*/ui/settings/SettingsActivity.kt|*/layout/activity_settings.xml) add ui.DebugModeScreenTest ;;&
                    *) [ "$matched" -eq 1 ] || uncovered+=("$f") ;;
                esac
                ;;
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
        # Unit tests and lint in a single Gradle call (one start-up less); lint comes first, so a lint error stops before the
        # device tests.
        echo ">> tests unitaires (tous) et lint"
        "${GRADLE[@]}" :app:testDebugUnitTest :app:lintDebug
        run_site
        run_device
        ;;
    *)
        sed -n '2,8p' "$0" | sed 's/^# \{0,1\}//'
        exit 1
        ;;
esac
echo "OK"
