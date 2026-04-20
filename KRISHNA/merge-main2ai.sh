#!/bin/bash

# Configuration
SOURCE_BRANCH="main"
TARGET_BRANCH="AI-capabilities"
REMOTE="origin"

echo "🔄 Initializing merge: ${SOURCE_BRANCH} ➡️ ${TARGET_BRANCH}"

# Ensure repo is clean (no uncommitted changes)
if ! git diff-index --quiet HEAD --; then
  echo "ERROR: Working directory is not clean. Commit or stash changes before proceeding."
  exit 1
fi

# Fetch latest from remote
echo "Fetching latest changes from ${REMOTE}..."
git fetch ${REMOTE}

# Checkout target branch
echo "Checking out target branch ${TARGET_BRANCH}..."
git checkout ${TARGET_BRANCH}

# Pull latest target branch updates
echo "Pulling latest ${TARGET_BRANCH}..."
git pull ${REMOTE} ${TARGET_BRANCH}

# Merge source branch into target
echo "Merging ${SOURCE_BRANCH} into ${TARGET_BRANCH}..."
if git merge ${REMOTE}/${SOURCE_BRANCH} --no-ff -m "Merge ${SOURCE_BRANCH} into ${TARGET_BRANCH}"; then
  echo "Merge completed successfully."
else
  echo "Merge conflict detected ❌ . Please resolve conflicts manually."
    echo "CONFLICTS DETECTED"
    # List only the files that are currently in a conflict state
    git diff --name-only --diff-filter=U

    echo "--------------------------------------------------"
    echo "Instructions:"
    echo "1. Open the files listed above."
    echo "2. Search for '<<<<<<<', '=======', and '>>>>>>>'."
    echo "3. Resolve the differences and save."
    echo "4. Run 'git add <file>' for each resolved file."
    echo "5. Run 'git commit' to finalize the merge."
fi

# Push updated branch
echo "Pushing merged changes to ${REMOTE}/${TARGET_BRANCH}..."
git push ${REMOTE} ${TARGET_BRANCH}

echo "=== Merge operation completed successfully ✅ "

