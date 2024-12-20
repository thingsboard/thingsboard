#!/bin/bash

# Debug-Modus aktivieren, um alle Befehle anzuzeigen
set -x

# Funktion, um einen Befehl auszuführen und den Benutzer zu pausieren
run_command() {
  command=$1
  eval "$command"
  read -p "Drücken Sie Enter, um fortzufahren..."
}

# Repository aktualisieren
echo "Aktualisiere das Git-Repository..."
run_command "git pull"

# Caches entfernen
echo "Entferne Caches..."
run_command "rm -rf ~/.m2/repository ~/.gradle/caches/ ui-ngx/node_modules"

# Lizenz-Header überprüfen und formatieren
echo "Formatiere Lizenz-Header..."
run_command "mvn license:format"

# Projekt mit Maven bauen
echo "Baue das Projekt mit Maven..."
run_command "mvn clean install -DskipTests -Ddockerfile.skip=false"

# Verzeichnis wechseln
echo "Wechsle ins OUT-Verzeichnis..."
run_command "cd .. && cd OUT"

# Docker-Image speichern
echo "Speichere Docker-Image als tb.tar..."
echo "Geben Sie einen Namen für die .tar-Datei ein (z. B. tb_image.tar):"
read -r tar_name
run_command "docker save thingsboard/tb-postgres:latest > $tar_name"

echo "Das Skript wurde erfolgreich abgeschlossen. Alle Schritte wurden im aktuellen Terminal ausgeführt."
