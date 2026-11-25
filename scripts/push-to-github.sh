#!/usr/bin/env bash
# Push currently staged commits on the current branch to origin.
# For PRs use: ./scripts/mneme-pr.sh or ./scripts/commit-and-pr.sh
# Author: Priyanshu Chawda (priyanshuchawda)
set -euo pipefail

AUTHOR="Priyanshu Chawda"
GITHUB_USER="priyanshuchawda"
DEFAULT_REPO_NAME="mneme"
REPO_NAME="${1:-$DEFAULT_REPO_NAME}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

log() { printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"; }
die() { log "ERROR: $*" >&2; exit 1; }
require_command() { command -v "$1" >/dev/null 2>&1 || die "'$1' not found."; }

main() {
  require_command git
  require_command gh
  gh auth status >/dev/null 2>&1 || die "Run: gh auth login"
  cd "${PROJECT_ROOT}"

  if [[ ! -d .git ]]; then
    git init -b main
  fi

  if ! git remote get-url origin >/dev/null 2>&1; then
    gh repo create "${REPO_NAME}" --private --source=. --remote=origin \
      --description "Mneme — offline personal memory by ${AUTHOR}" \
      || die "Failed to create repo"
  fi

  BRANCH="$(git branch --show-current)"
  log "Pushing ${BRANCH} → origin"
  git push -u origin HEAD
  log "Done: https://github.com/${GITHUB_USER}/${REPO_NAME}/tree/${BRANCH}"
}

main "$@"
