#!/usr/bin/env bash
# Commit staged product work and push via origin (private repo).
# Prefer: stage files yourself, then:
#   ./scripts/commit-and-pr.sh "commit message" "PR title"
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
AUTHOR="Priyanshu Chawda"
GITHUB_USER="priyanshuchawda"

MSG="${1:?commit message required}"
PR_TITLE="${2:-$MSG}"

if git diff --cached --quiet; then
  echo "Nothing staged. Stage files first (git add ...)."
  exit 1
fi

git commit -m "$(cat <<EOF
${MSG}

Author: ${AUTHOR} (${GITHUB_USER})
EOF
)"

./scripts/mneme-pr.sh "$PR_TITLE" "## Summary
${MSG}

## Test plan
- [ ] ./scripts/build-mneme.sh
- [ ] ./scripts/install-mneme.sh
- [ ] Open Retenqo → Pulse → note in timeline
"
