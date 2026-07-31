#!/bin/sh
set -eu

GRADLE_VERSION=8.6
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
CACHE_DIR="$APP_HOME/.gradle-bootstrap"
DIST_DIR="$CACHE_DIR/gradle-$GRADLE_VERSION"
ZIP_FILE="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"
DOWNLOAD_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
REQUIRED_JAR="$DIST_DIR/lib/plugins/gradle-diagnostics-$GRADLE_VERSION.jar"

install_gradle() {
  mkdir -p "$CACHE_DIR"
  rm -rf "$DIST_DIR" "$CACHE_DIR/.extract-$GRADLE_VERSION"
  rm -f "$ZIP_FILE.part"

  echo "Downloading Gradle $GRADLE_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --retry 3 --retry-delay 2 "$DOWNLOAD_URL" -o "$ZIP_FILE.part"
  elif command -v wget >/dev/null 2>&1; then
    wget --tries=3 -O "$ZIP_FILE.part" "$DOWNLOAD_URL"
  else
    echo "Error: curl or wget is required." >&2
    exit 1
  fi

  mv "$ZIP_FILE.part" "$ZIP_FILE"
  mkdir -p "$CACHE_DIR/.extract-$GRADLE_VERSION"
  unzip -q "$ZIP_FILE" -d "$CACHE_DIR/.extract-$GRADLE_VERSION"
  mv "$CACHE_DIR/.extract-$GRADLE_VERSION/gradle-$GRADLE_VERSION" "$DIST_DIR"
  rm -rf "$CACHE_DIR/.extract-$GRADLE_VERSION"

  if [ ! -x "$DIST_DIR/bin/gradle" ] || [ ! -f "$REQUIRED_JAR" ]; then
    echo "Error: Gradle extraction was incomplete. Delete .gradle-bootstrap and retry." >&2
    exit 1
  fi
}

# Validate both the launcher and a core plugin JAR. This prevents an incomplete
# extraction from being reused, which caused the old NoSuchFileException.
if [ ! -x "$DIST_DIR/bin/gradle" ] || [ ! -f "$REQUIRED_JAR" ]; then
  install_gradle
fi

exec "$DIST_DIR/bin/gradle" --no-daemon "$@"
