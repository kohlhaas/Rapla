while inotifywait -e close_write webapps/rapla.war; do echo 'help'; done

# when file change:
# screen -XS rapla_session kill
# screen -dmS rapla_session ./raplaserver.sh run