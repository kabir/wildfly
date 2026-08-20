#!/bin/bash
set -euo pipefail

DOCS_ROOT="docs/src/main/asciidoc"
MAIN_LLMS="${DOCS_ROOT}/llms.txt"

# Determine the diff base
if [ "${GITHUB_EVENT_NAME}" = "pull_request" ]; then
    BASE="origin/${GITHUB_BASE_REF}"
else
    BASE="${GITHUB_SHA}^"
fi

# Get newly added files under docs/ (status A = added)
new_files=$(git diff --name-only --diff-filter=A "${BASE}"...HEAD -- "${DOCS_ROOT}/" 2>/dev/null || \
            git diff --name-only --diff-filter=A "${BASE}" HEAD -- "${DOCS_ROOT}/")

if [ -z "$new_files" ]; then
    echo "No new files added under ${DOCS_ROOT}/, nothing to check."
    exit 0
fi

errors=()

for file in $new_files; do
    # Skip non-adoc, non-llms.txt files (images, etc.)
    if [[ "$file" != *.adoc && "$file" != */llms.txt ]]; then
        continue
    fi

    # Path relative to DOCS_ROOT (e.g. "_admin-guide/subsystem-configuration/Foo.adoc")
    rel_path="${file#${DOCS_ROOT}/}"

    # Determine the immediate subdirectory, if any
    if [[ "$rel_path" == */* ]]; then
        # File is in a subdirectory
        subdir="${rel_path%%/*}"
        subdir_llms="${DOCS_ROOT}/${subdir}/llms.txt"

        if [ "$file" = "$subdir_llms" ]; then
            # This IS a new llms.txt for a subdirectory — it must be referenced from the main llms.txt
            if ! grep -qF "${subdir}/llms.txt" "$MAIN_LLMS"; then
                errors+=("New sub-guide index '${rel_path}' is not referenced from ${MAIN_LLMS}")
            fi
        elif [[ "$file" == *.adoc ]]; then
            # This is a new .adoc in a subdirectory
            if [ -f "$subdir_llms" ]; then
                # Subdirectory has its own llms.txt — file must be referenced there
                # The reference path is relative to the subdir (e.g. "subsystem-configuration/Foo.adoc" or "Foo.adoc")
                inner_path="${rel_path#${subdir}/}"
                if ! grep -qF "$inner_path" "$subdir_llms"; then
                    errors+=("New file '${rel_path}' is not referenced in ${subdir_llms}")
                fi
            else
                # No sub-guide llms.txt — file must be referenced from the main llms.txt
                if ! grep -qF "$rel_path" "$MAIN_LLMS"; then
                    errors+=("New file '${rel_path}' is not referenced in ${MAIN_LLMS} (directory '${subdir}' has no llms.txt)")
                fi
            fi
        fi
    else
        # File is directly in the asciidoc root
        if [[ "$file" == *.adoc ]]; then
            if ! grep -qF "$rel_path" "$MAIN_LLMS"; then
                errors+=("New file '${rel_path}' is not referenced in ${MAIN_LLMS}")
            fi
        fi
    fi
done

if [ ${#errors[@]} -gt 0 ]; then
    echo "ERROR: The following new documentation files are not referenced from an llms.txt index:"
    echo ""
    for err in "${errors[@]}"; do
        echo "  - $err"
    done
    echo ""
    echo "Please add an entry for each file in the appropriate llms.txt."
    echo "  - Files in a subdirectory with its own llms.txt → add to that subdirectory's llms.txt"
    echo "  - Files in a subdirectory without an llms.txt → add to ${MAIN_LLMS}"
    echo "  - Files in the top-level docs directory → add to ${MAIN_LLMS}"
    echo "  - New subdirectory llms.txt files → add a reference from ${MAIN_LLMS}"
    exit 1
fi

echo "All new documentation files are properly referenced in llms.txt indexes."
