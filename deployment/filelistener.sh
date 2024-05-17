#!/bin/bash

FILE_TO_WATCH="/app/testfile.txt"
# later on "/app/webapps/rapla.war"
COMMAND_TO_EXECUTE="echo help"
# later "screen -XS rapla_session kill; screen -dmS rapla_session ./raplaserver.sh run"
INTERVAL=1  # Polling interval in seconds

# Get the initial checksum of the file
if [ ! -f "$FILE_TO_WATCH" ]; then
  echo "File $FILE_TO_WATCH does not exist."
  exit 1
fi

initial_checksum=$(md5sum "$FILE_TO_WATCH" | awk '{ print $1 }')

echo "Watching for changes in $FILE_TO_WATCH..."
while true; do
  sleep $INTERVAL

  if [ ! -f "$FILE_TO_WATCH" ]; then
    echo "File $FILE_TO_WATCH does not exist anymore."
    exit 1
  fi

  current_checksum=$(md5sum "$FILE_TO_WATCH" | awk '{ print $1 }')

  if [ "$initial_checksum" != "$current_checksum" ]; then
    echo "File $FILE_TO_WATCH has been modified. Executing command..."
    $COMMAND_TO_EXECUTE
    initial_checksum=$current_checksum
  fi
done