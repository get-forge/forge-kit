#!/usr/bin/env bash
set -euo pipefail

# owasp-update-resilient.sh
# Type: executable
# Refresh the OWASP dependency-check NVD cache (single bounded attempt).
# Skips the NVD API when the restored cache is still within nvdValidForHours.
# If NVD is unavailable but a restored cache exists, exit successfully so CI
# keeps using the last good snapshot (see nvdValidForHours in pom.xml).
# Usage: ./owasp-update-resilient.sh

# ---- Imports ----------------------------------------------------------------

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
# shellcheck source=../../scripts/lib/common.sh
source "${REPO_ROOT}/scripts/lib/common.sh"

# ---- Constants --------------------------------------------------------------

readonly CACHE_DIR="${REPO_ROOT}/.dependency-check-cache"
readonly CACHE_DB="${CACHE_DIR}/odc.mv.db"
# Keep in sync with nvdValidForHours in pom.xml (dependency-check profile).
readonly NVD_VALID_FOR_HOURS=168
readonly UPDATE_TIMEOUT_SECONDS="${OWASP_UPDATE_TIMEOUT_SECONDS:-2400}"

# ---- Functions --------------------------------------------------------------

validate_dependencies() {
    require_command gum
}

resolve_timeout_cmd() {
    if command -v timeout &> /dev/null; then
        echo timeout
    elif command -v gtimeout &> /dev/null; then
        echo gtimeout
    fi
}

run_with_timeout() {
    local duration="$1"
    shift
    local timeout_cmd
    timeout_cmd="$(resolve_timeout_cmd)"

    if [[ -n "${timeout_cmd}" ]]; then
        "${timeout_cmd}" "${duration}" "$@"
        return $?
    fi

    "$@"
}

resolve_maven_cmd() {
    if command -v mvnd &> /dev/null; then
        echo mvnd
    else
        echo "${REPO_ROOT}/mvnw"
    fi
}

cache_mtime_seconds() {
    if [[ "$(uname -s)" == "Darwin" ]]; then
        stat -f %m "${CACHE_DB}"
    else
        stat -c %Y "${CACHE_DB}"
    fi
}

cache_age_hours() {
    local mtime now age_seconds
    mtime="$(cache_mtime_seconds)"
    now="$(date +%s)"
    age_seconds=$((now - mtime))
    echo $((age_seconds / 3600))
}

cache_is_usable() {
    [[ -f "${CACHE_DB}" ]]
}

cache_is_fresh() {
    if ! cache_is_usable; then
        return 1
    fi
    local age_hours
    age_hours="$(cache_age_hours)"
    ((age_hours < NVD_VALID_FOR_HOURS))
}

set_cache_modified_output() {
    local cache_modified="$1"
    if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
        echo "cache_modified=${cache_modified}" >> "${GITHUB_OUTPUT}"
    fi
}

run_update() {
    local maven_cmd
    maven_cmd="$(resolve_maven_cmd)"
    run_with_timeout "${UPDATE_TIMEOUT_SECONDS}" \
        bash -c "cd \"${REPO_ROOT}\" && \"${maven_cmd}\" -B -N org.owasp:dependency-check-maven:update-only -Pdependency-check"
}

# ---- Main -------------------------------------------------------------------

main() {
    validate_dependencies

    if cache_is_fresh; then
        set_cache_modified_output false
        exit 0
    fi

    set +e
    run_update
    local update_exit=$?
    set -e

    if [[ "${update_exit}" -eq 0 ]]; then
        set_cache_modified_output true
        exit 0
    fi

    if cache_is_usable; then
        log_warn "NVD update failed; keeping existing cache."
        set_cache_modified_output false
        exit 0
    fi

    log_error "NVD update failed and no usable cache exists."
    set_cache_modified_output false
    exit 1
}

main "$@"
