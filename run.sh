mvn package
java -Djava.library.path=target/natives -cp target/vlcplayer-1.0-SNAPSHOT-jar-with-dependencies.jar com.outr.mediaplayer.MediaPlayer
