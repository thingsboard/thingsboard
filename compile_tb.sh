#!/bin/bash

# Debug-Modus aktivieren, um alle Befehle anzuzeigen
set -x

# Funktion, um ein neues Screen-Fenster zu erstellen und darin einen Befehl auszuführen
run_in_screen() {
  echo "Geben Sie einen Namen für die Screen-Sitzung ein:"
  read -r screen_name
  command=$1
  screen -dmS "$screen_name" bash -c "$command; exec bash"
}

# Repository aktualisieren
echo "Aktualisiere das Git-Repository..."
run_in_screen "git pull && read -p 'Drücken Sie Enter, um fortzufahren...'"

# Caches entfernen
echo "Entferne Caches..."
run_in_screen "rm -rf ~/.m2/repository ~/.gradle/caches/ ui-ngx/node_modules && read -p 'Drücken Sie Enter, um fortzufahren...'"

# Projekt mit Maven bauen
echo "Baue das Projekt mit Maven..."
run_in_screen "mvn clean install -DskipTests -Ddockerfile.skip=false && read -p 'Drücken Sie Enter, um fortzufahren...'"

# Verzeichnis wechseln
echo "Wechsle ins OUT-Verzeichnis..."
run_in_screen "cd .. && cd OUT && read -p 'Drücken Sie Enter, um fortzufahren...'"

# Docker-Image speichern
echo "Speichere Docker-Image als tb.tar..."
echo "Geben Sie einen Namen für die .tar-Datei ein (z. B. tb_image.tar):"
read -r tar_name
run_in_screen "docker save thingsboard/tb-postgres:latest > $tar_name && read -p 'Drücken Sie Enter, um fortzufahren...'"

echo "Das Skript wurde erfolgreich abgeschlossen. Alle Schritte wurden in separaten Screen-Sitzungen ausgeführt."

