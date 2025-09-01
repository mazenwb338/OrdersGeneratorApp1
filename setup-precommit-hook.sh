#!/bin/bash
set -euo pipefail

HOOKS_DIR=".githooks"
HOOK_FILE="${HOOKS_DIR}/pre-commit"

mkdir -p "${HOOKS_DIR}"

cat > "${HOOK_FILE}" <<'EOF'
#!/bin/bash
set -e
# Block heap dumps and very large files.
for f in $(git diff --cached --name-only); do
  if [[ "$f" == *.hprof ]]; then
    echo "ERROR: $f is an .hprof dump. Remove it before committing."
    exit 1
  fi
  if [ -f "$f" ]; then
    sz=$(wc -c <"$f")
    if [ "$sz" -gt 50000000 ]; then
      echo "ERROR: $f is >50MB ($sz bytes). Use external storage."
      exit 1
    fi
  fi
done
EOF

chmod +x "${HOOK_FILE}"
git config core.hooksPath "${HOOKS_DIR}"
echo "Pre-commit hook installed at ${HOOK_FILE}"