#!/bin/bash
set -euo pipefail

lc() { printf '%s' "$1" | tr '[:upper:]' '[:lower:]'; }

echo "=== Git Milestone Backup ==="
git status --short || true
echo

read -r -p "Stage ALL changes (including new/deleted)? [y/N]: " STAGE_ALL
if [ "$(lc "${STAGE_ALL:-}")" = "y" ]; then
  git add -A
  echo "All changes staged."
else
  echo "Skipping auto stage."
fi

if git diff --cached --quiet; then
  read -r -p "No staged changes. Tag current HEAD anyway? [y/N]: " CONT
  if [ "$(lc "${CONT:-}")" != "y" ]; then
    echo "Aborting."
    exit 0
  fi
fi

while true; do
  read -r -p "Version tag (e.g. v2025-08-29-m1): " VERSION_TAG
  [ -z "$VERSION_TAG" ] && { echo "Empty tag."; continue; }
  if git rev-parse -q --verify "refs/tags/$VERSION_TAG" >/dev/null; then
    echo "Tag exists."
  elif printf '%s' "$VERSION_TAG" | grep -q '[[:space:]]'; then
    echo "No spaces."
  else
    break
  fi
done

read -r -p "Commit title: " COMMIT_TITLE
[ -z "${COMMIT_TITLE}" ] && COMMIT_TITLE="chore: milestone ${VERSION_TAG}"

echo "Enter description (end with single '.' line):"
DESC_LINES=()
while IFS= read -r L; do
  [ "$L" = "." ] && break
  DESC_LINES+=("$L")
done
DESCRIPTION=$(printf "%s\n" "${DESC_LINES[@]:-}")

NEW_COMMIT=0
if ! git diff --cached --quiet; then
  git commit -m "${COMMIT_TITLE}" -m "${DESCRIPTION}"
  NEW_COMMIT=1
else
  echo "No new commit (using existing HEAD)."
fi

TAG_MSG="${COMMIT_TITLE}

${DESCRIPTION}"
git tag -a "${VERSION_TAG}" -m "${TAG_MSG}"
echo "Tag ${VERSION_TAG} created."

read -r -p "Create bundle? [y/N]: " MAKE_BUNDLE
if [ "$(lc "${MAKE_BUNDLE:-}")" = "y" ]; then
  TS=$(date +%Y%m%d-%H%M%S)
  BUNDLE="milestone-${VERSION_TAG}-${TS}.bundle"
  git bundle create "${BUNDLE}" --all
  echo "Bundle: ${BUNDLE}"
fi

if git remote | grep -q .; then
  read -r -p "Push commit & tag? [y/N]: " PUSH_IT
  if [ "$(lc "${PUSH_IT:-}")" = "y" ]; then
    git push origin HEAD
    git push origin "${VERSION_TAG}"
    echo "Pushed."
  else
    echo "Skipped push."
  fi
else
  echo "No remote configured."
fi

echo "=== Summary ==="
[ $NEW_COMMIT -eq 1 ] && echo "Commit: $(git rev-parse --short HEAD)"
echo "Tag:    ${VERSION_TAG}"
[ -n "${BUNDLE:-}" ] && echo "Bundle: ${BUNDLE}"
echo "Done."