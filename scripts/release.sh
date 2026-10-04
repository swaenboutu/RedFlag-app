#!/usr/bin/env bash
# Construit l'APK signé de la version de production, prêt à être joint à une « release » GitHub.
#
#   scripts/release.sh             construit build/release/red-flag-v<version>.apk (+ somme de contrôle SHA-256)
#   scripts/release.sh --publish   construit, puis crée la release GitHub v<version> avec l'APK (demande confirmation ; nécessite
#                                  la CLI GitHub : `winget install GitHub.cli`, puis `gh auth login`)
#
# La version vient de `versionName` dans app/build.gradle.kts : l'augmenter (et `versionCode`) avant chaque nouvelle release.
# La clé de signature est décrite dans keystore.properties (jamais versionné) ; sans elle, l'APK ne serait pas installable.
set -euo pipefail

cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-C:/Program Files/Android/Android Studio/jbr}"

[ -f keystore.properties ] || { echo "keystore.properties introuvable : sans clé de signature, l'APK ne serait pas installable." >&2; exit 2; }

version=$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' app/build.gradle.kts | head -1)
[ -n "$version" ] || { echo "versionName introuvable dans app/build.gradle.kts" >&2; exit 2; }
tag="v$version"
out="build/release"
apk="$out/red-flag-$tag.apk"

echo ">> version $version"
./gradlew --console=plain -q :app:assembleRelease
mkdir -p "$out"
cp app/build/outputs/apk/release/app-release.apk "$apk"
(cd "$out" && sha256sum "$(basename "$apk")" > "$(basename "$apk").sha256")
echo ">> $apk ($(du -h "$apk" | cut -f1))"
cat "$apk.sha256"

if [ "${1:-}" != "--publish" ]; then
    echo
    echo "Pour la publier à la main : GitHub > Releases > Draft a new release, tag $tag, joindre l'APK (et le .sha256)."
    echo "Ou : scripts/release.sh --publish"
    exit 0
fi

command -v gh >/dev/null || { echo "CLI GitHub (gh) introuvable : winget install GitHub.cli, puis gh auth login." >&2; exit 3; }
git diff --quiet && git diff --cached --quiet || { echo "Des modifications ne sont pas commitées : une release doit correspondre à un commit." >&2; exit 3; }
git rev-parse "$tag" >/dev/null 2>&1 && { echo "Le tag $tag existe déjà : augmenter versionName." >&2; exit 3; }
git status -sb | head -1 | grep -q 'ahead' && { echo "Des commits ne sont pas poussés : les pousser avant de publier." >&2; exit 3; }

printf 'Publier la release %s (publique) avec %s ? [oui/non] ' "$tag" "$(basename "$apk")"
read -r answer
[ "$answer" = "oui" ] || { echo "Annulé."; exit 0; }
gh release create "$tag" "$apk" "$apk.sha256" --title "Red Flag $version" --generate-notes --prerelease
echo ">> release $tag publiée (marquée « pré-version »)."
