#!/usr/bin/env bash
# Maakt de drie GitHub Environments aan via de gh CLI.
# Gebruik: bash .github/scripts/setup-environments.sh [prod-reviewer-username]
#
# Vereiste: gh CLI geïnstalleerd en ingelogd (gh auth login)
set -euo pipefail

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
PROD_REVIEWER="${1:-LU2webservice}"

echo "Repository: $REPO"
echo ""

# --- dev: geen restricties ---
echo "Aanmaken: dev..."
gh api --method PUT "repos/$REPO/environments/dev" \
  --input - <<'EOF'
{
  "wait_timer": 0,
  "deployment_branch_policy": null
}
EOF
echo "  dev aangemaakt (geen restricties)"

# --- test: geen restricties ---
echo "Aanmaken: test..."
gh api --method PUT "repos/$REPO/environments/test" \
  --input - <<'EOF'
{
  "wait_timer": 0,
  "deployment_branch_policy": null
}
EOF
echo "  test aangemaakt (geen restricties)"

# --- prod: required reviewer ---
echo "Aanmaken: prod (required reviewer: $PROD_REVIEWER)..."
REVIEWER_ID=$(gh api "users/$PROD_REVIEWER" --jq .id)

gh api --method PUT "repos/$REPO/environments/prod" \
  --input - <<EOF
{
  "wait_timer": 0,
  "prevent_self_review": true,
  "reviewers": [{"type": "User", "id": $REVIEWER_ID}],
  "deployment_branch_policy": null
}
EOF
echo "  prod aangemaakt (reviewer: $PROD_REVIEWER, self-review geblokkeerd)"

echo ""
echo "Klaar! Controleer: https://github.com/$REPO/settings/environments"
