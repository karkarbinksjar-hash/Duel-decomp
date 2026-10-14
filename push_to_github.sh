#!/usr/bin/env bash
set -e

# Yu-Gi-Oh! Recomp - Automated GitHub Push Script
# Usage:
#   Using PAT: ./push_to_github.sh --pat <TOKEN> <REPO_URL>
#   Using SSH: ./push_to_github.sh --ssh <GIT_SSH_URL>

MODE="$1"
ARG2="$2"
ARG3="$3"

if [ "$MODE" = "--pat" ]; then
    TOKEN="$ARG2"
    REPO="$ARG3"
    
    if [ -z "$TOKEN" ] || [ -z "$REPO" ]; then
        echo "Usage: ./push_to_github.sh --pat <TOKEN> <REPO_URL>"
        exit 1
    fi
    
    # Strip https:// if provided
    CLEAN_REPO=$(echo "$REPO" | sed 's|https://||' | sed 's|http://||' | sed 's|github.com/||')
    TARGET_URL="https://${TOKEN}@github.com/${CLEAN_REPO}"
    
    echo "Configuring remote origin with PAT authentication..."
    git remote remove origin 2>/dev/null || true
    git remote add origin "$TARGET_URL"
    
    echo "Pushing branches and tags to GitHub..."
    git push -u origin main --tags
    git push origin feature/dds-rom-pipeline-and-portrait-duel-engine || true
    
    echo "Push completed successfully!"

elif [ "$MODE" = "--ssh" ]; then
    REPO="$ARG2"
    
    if [ -z "$REPO" ]; then
        echo "Usage: ./push_to_github.sh --ssh git@github.com:owner/repo.git"
        exit 1
    fi
    
    echo "Configuring remote origin with SSH authentication..."
    git remote remove origin 2>/dev/null || true
    git remote add origin "$REPO"
    
    echo "Testing SSH connection to GitHub..."
    ssh -T -i /home/user/.ssh/id_ed25519 -o StrictHostKeyChecking=accept-new git@github.com || true
    
    echo "Pushing branches and tags to GitHub..."
    git push -u origin main --tags
    git push origin feature/dds-rom-pipeline-and-portrait-duel-engine || true
    
    echo "Push completed successfully!"
else
    echo "Unknown mode: $MODE. Use --pat or --ssh"
    exit 1
fi
