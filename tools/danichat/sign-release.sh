#!/usr/bin/env bash
# Signs locally or on a trusted runner. Never enable shell tracing around secrets.
set -euo pipefail
umask 077

unsigned_apk="${1:?usage: sign-release.sh UNSIGNED_APK SIGNED_APK}"
signed_apk="${2:?usage: sign-release.sh UNSIGNED_APK SIGNED_APK}"
: "${DANICHAT_KEYSTORE_BASE64:?missing release keystore secret}"
: "${DANICHAT_KEY_ALIAS:?missing key alias}"
: "${DANICHAT_STORE_PASSWORD:?missing store password}"
: "${DANICHAT_KEY_PASSWORD:?missing key password}"
: "${DANICHAT_RELEASE_CERT_SHA256:?missing expected release certificate fingerprint}"
: "${DANICHAT_SDK_ROOT:?missing SDK root}"

expected_cert="${DANICHAT_RELEASE_CERT_SHA256//:/}"
expected_cert="${expected_cert,,}"
[[ "$expected_cert" =~ ^[a-f0-9]{64}$ ]] || { echo 'Invalid expected certificate fingerprint' >&2; exit 1; }
[[ -f "$unsigned_apk" && ! -e "$signed_apk" ]] || { echo 'Invalid input or existing output' >&2; exit 1; }

staging_dir="$(mktemp -d)"
trap 'rm -rf "$staging_dir"' EXIT
export DANICHAT_TEMP_KEYSTORE="$staging_dir/release.jks"
python3 - <<'PY'
import base64, os
from pathlib import Path
Path(os.environ['DANICHAT_TEMP_KEYSTORE']).write_bytes(
    base64.b64decode(''.join(os.environ['DANICHAT_KEYSTORE_BASE64'].split()), validate=True)
)
PY
signer="$DANICHAT_SDK_ROOT/build-tools/37.0.0/apksigner"
aligner="$DANICHAT_SDK_ROOT/build-tools/37.0.0/zipalign"
"$aligner" -P 16 -f 4 "$unsigned_apk" "$staging_dir/aligned.apk"
"$signer" sign --ks "$DANICHAT_TEMP_KEYSTORE" --ks-key-alias "$DANICHAT_KEY_ALIAS" \
    --ks-pass env:DANICHAT_STORE_PASSWORD --key-pass env:DANICHAT_KEY_PASSWORD \
    --v4-signing-enabled false --out "$staging_dir/signed.apk" "$staging_dir/aligned.apk"
"$signer" verify --verbose "$staging_dir/signed.apk" > /dev/null
actual_cert="$("$signer" verify --print-certs "$staging_dir/signed.apk" |
    sed -n 's/.*certificate SHA-256 digest: //p' | head -1 | tr '[:upper:]' '[:lower:]')"
[[ "$actual_cert" == "$expected_cert" ]] || { echo 'Release certificate mismatch' >&2; exit 1; }
"$aligner" -c -P 16 4 "$staging_dir/signed.apk"
mkdir -p "$(dirname "$signed_apk")"
cp "$staging_dir/signed.apk" "$signed_apk"
(
    cd "$(dirname "$signed_apk")"
    sha256sum "$(basename "$signed_apk")" > SHA256SUMS
)
echo 'Release APK signature and alignment verified.'
