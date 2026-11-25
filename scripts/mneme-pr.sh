#!/usr/bin/env bash
# Create/update a GitHub PR for the current branch (product workflow).
# Usage:
#   ./scripts/mneme-pr.sh "title" "body markdown"
#   ./scripts/mneme-pr.sh --push-only
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

AUTHOR="Priyanshu Chawda"
GITHUB_USER="priyanshuchawda"
REPO_NAME="mneme"

require() { command -v "$1" >/dev/null || { echo "need $1"; exit 1; }; }
require git
require gh

BRANCH="$(git branch --show-current)"
[[ "$BRANCH" != "main" ]] || { echo "Create a feature branch first"; exit 1; }

git push -u origin HEAD

if [[ "${1:-}" == "--push-only" ]]; then
  echo "Pushed $BRANCH"
  exit 0
fi

TITLE="${1:-Mneme product update}"
BODY="${2:-## Summary
- Product work on \`$BRANCH\`

## Test plan
- [ ] ./scripts/build-mneme.sh
- [ ] ./scripts/install-mneme.sh
- [ ] Pulse + timeline on Phone 1
}"

EXISTING="$(gh pr list --head "$BRANCH" --json number --jq '.[0].number' 2>/dev/null || true)"
if [[ -n "$EXISTING" && "$EXISTING" != "null" ]]; then
  echo "PR already open: #$EXISTING"
  gh pr view "$EXISTING" --web || gh pr view "$EXISTING"
else
  gh pr create --title "$TITLE" --body "$BODY" --base main
fi
