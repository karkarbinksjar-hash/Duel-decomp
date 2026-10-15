#!/usr/bin/env bash
set -e

# Yu-Gi-Oh! Recomp - Automated GitHub Push Script
TOKEN_FILE="/home/user/.github_token"
REPO="karkarbinksjar-hash/Duel-decomp"

if [ -f "$TOKEN_FILE" ]; then
    TOKEN=$(cat "$TOKEN_FILE" | tr -d ' \n\r')
fi

MODE="$1"
if [ "$MODE" = "--pat" ] && [ -n "$2" ]; then
    TOKEN="$2"
    if [ -n "$3" ]; then REPO="$3"; fi
fi

if [ -z "$TOKEN" ]; then
    echo "Error: No GitHub token provided or found in $TOKEN_FILE"
    exit 1
fi

CLEAN_REPO=$(echo "$REPO" | sed 's|https://||' | sed 's|http://||' | sed 's|github.com/||')
TARGET_URL="https://${TOKEN}@github.com/${CLEAN_REPO}"

echo "Configuring remote origin..."
git remote remove origin 2>/dev/null || true
git remote add origin "$TARGET_URL"

echo "Pushing branches and tags to GitHub..."
git push -u origin main
git push origin feature/opponent-sequencer-gbc-assets-and-battle-ux || true
git push origin feature/dds-rom-pipeline-and-portrait-duel-engine || true
git push origin --tags

echo "Push completed successfully!"
