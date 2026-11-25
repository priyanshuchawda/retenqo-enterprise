#!/usr/bin/env bash
# Start work on a GitHub issue: sync main, create branch, open issue in browser optional.
# Usage: ./scripts/gh-start-issue.sh 26 card-schema
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ISSUE="${1:?issue number}"
SLUG="${2:?short-slug}"
git fetch origin main
git checkout main
git pull --ff-only origin main
BRANCH="feature/${ISSUE}-${SLUG}"
git checkout -b "$BRANCH"
echo "On $BRANCH — closes/implements #$ISSUE"
gh issue view "$ISSUE" --json title,url -q '"\(.title)\n\(.url)"'
