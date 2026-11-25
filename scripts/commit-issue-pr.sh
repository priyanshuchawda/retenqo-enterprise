#!/usr/bin/env bash
# Stage, commit, push, and open/update a PR for an issue.
# Usage:
#   ./scripts/commit-issue-pr.sh 26 "feat(#26): card schema + Room migration" [extra body]
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ISSUE="${1:?issue number}"
MSG="${2:?commit message}"
EXTRA="${3:-}"

git add -A
# Never commit secrets / local research clones
git reset HEAD -- research/ 2>/dev/null || true
git reset HEAD -- .env .env.* 2>/dev/null || true

if git diff --cached --quiet; then
  echo "Nothing staged."
  exit 1
fi

git commit -m "$(cat <<EOF
${MSG}

Author: Priyanshu Chawda (priyanshuchawda)
EOF
)"

./scripts/mneme-pr.sh "$MSG" "$(cat <<EOF
## Summary
${MSG}

Closes #${ISSUE}

${EXTRA}

## Test plan
- [x] ./scripts/test-mneme.sh
- [ ] ./scripts/build-mneme.sh && ./scripts/install-mneme.sh
- [ ] Open Retenqo → Memory shows type chips; Done / Snooze work
EOF
)"
