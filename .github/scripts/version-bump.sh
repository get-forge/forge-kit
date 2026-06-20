#!/usr/bin/env bash
set -euo pipefail

# version-bump.sh
# Check and bump version using Commitizen
# Usage: ./version-bump.sh
#
# Outputs GitHub Actions outputs:
#   - current_version: The version before bumping
#   - bumped: "true" if version was bumped, "false" otherwise
#   - next_version: The final version (new version if bumped, current if not)

# ---- Configuration ----------------------------------------------------------

GITHUB_OUTPUT="${GITHUB_OUTPUT:-/dev/stdout}"
COMMITIZEN_NO_BUMP_EXIT=21

# ---- Main --------------------------------------------------------------------

# Committer is set by the GPG action in CI so commits show as Verified.
git fetch --tags
git checkout main
git pull --ff-only origin main

CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "current_version=$CURRENT_VERSION" >> "$GITHUB_OUTPUT"
echo "Current version: $CURRENT_VERSION"

sed -i "s/^version = \".*\"/version = \"$CURRENT_VERSION\"/" .cz.toml

# Preview next version (exit 21 = no eligible conventional commits)
set +e
NEXT_VERSION=$(LEFTHOOK=0 cz bump --get-next 2>/dev/null)
GET_NEXT_EXIT=$?
set -e

if [ "$GET_NEXT_EXIT" -eq "$COMMITIZEN_NO_BUMP_EXIT" ] || [ -z "$NEXT_VERSION" ]; then
  echo "No version bump needed - no eligible commits found."
  echo "bumped=false" >> "$GITHUB_OUTPUT"
  echo "next_version=$CURRENT_VERSION" >> "$GITHUB_OUTPUT"
  exit 0
fi

if [ "$GET_NEXT_EXIT" -ne 0 ]; then
  echo "::error::commitizen --get-next failed (exit ${GET_NEXT_EXIT})"
  exit "$GET_NEXT_EXIT"
fi

echo "Next version: ${NEXT_VERSION}"

TAG="v${NEXT_VERSION}"
if git rev-parse "${TAG}^{commit}" >/dev/null 2>&1; then
  EXISTING_SHA=$(git rev-parse "${TAG}^{commit}")
  echo "::error::Tag ${TAG} already exists at ${EXISTING_SHA}."
  echo "::error::Orphaned release tags from an older version line can block semver bumps."
  echo "::error::Delete conflicting tags on GitHub, then re-run this workflow."
  exit 1
fi

# Changelog + .cz.toml only; Maven POMs and git tag are handled below so we control tagging.
set +e
BUMP_LOG=$(LEFTHOOK=0 cz bump --yes --changelog --version-files-only 2>&1)
BUMP_EXIT=$?
set -e
echo "$BUMP_LOG"

if [ "$BUMP_EXIT" -eq "$COMMITIZEN_NO_BUMP_EXIT" ]; then
  echo "No version bump needed - no eligible commits found."
  echo "bumped=false" >> "$GITHUB_OUTPUT"
  echo "next_version=$CURRENT_VERSION" >> "$GITHUB_OUTPUT"
  exit 0
fi

if [ "$BUMP_EXIT" -ne 0 ]; then
  echo "::error::commitizen bump failed (exit ${BUMP_EXIT})"
  exit "$BUMP_EXIT"
fi

NEXT_VERSION=$(grep '^version = ' .cz.toml | sed 's/^version = "\(.*\)"/\1/')
echo "Version bumped: ${CURRENT_VERSION} -> ${NEXT_VERSION}"

mvn versions:set -DnewVersion="${NEXT_VERSION}" -DgenerateBackupPoms=false
mvn versions:commit

git add -A
git commit -m "chore(release): bump version to ${NEXT_VERSION} [skip ci]" --signoff --no-verify
git tag -s "v${NEXT_VERSION}" -m "Release v${NEXT_VERSION}"

echo "bumped=true" >> "$GITHUB_OUTPUT"
echo "next_version=${NEXT_VERSION}" >> "$GITHUB_OUTPUT"
