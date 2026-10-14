#!/usr/bin/env bash
# Gera a aplicação executável para Linux ou macOS (com o Java incluído).
# Requisitos: JDK 17+ e Maven no PATH.  Resultado: dist/Orcamentos
set -euo pipefail
cd "$(dirname "$0")"

echo "[1/3] A compilar e a juntar as bibliotecas..."
mvn -q clean package -DskipTests

echo "[2/3] A criar a aplicação com o Java incluído..."
rm -rf dist
ICONE=()
if [[ "$(uname)" == "Linux" ]]; then ICONE=(--icon packaging/icone.png); fi

jpackage --type app-image \
  --name Orcamentos \
  --app-version 1.0 \
  --vendor "Numeros Proficuos" \
  --input target/jpackage \
  --main-jar orcamentos.jar \
  --main-class Main \
  "${ICONE[@]}" \
  --add-modules java.base,java.desktop,java.logging,java.xml,java.prefs,jdk.unsupported \
  --java-options "-Dfile.encoding=UTF-8" \
  --dest dist

echo "[3/3] Concluído: dist/ (Linux: dist/Orcamentos/bin/Orcamentos | macOS: dist/Orcamentos.app)"
