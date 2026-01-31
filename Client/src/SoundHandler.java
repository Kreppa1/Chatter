import javax.sound.sampled.*;
import java.util.HashMap;
import java.util.Map;

public class SoundHandler {

    private final Map<String, String> prefixSoundMap = new HashMap<>();
    private final String errorSoundPath = "sounds/error.wav";
    private boolean isPlaying = false; // 🔒 strict lock

    public SoundHandler() {

        register("// User disconnected from your channel", "sounds/neutral_connection_disconnected_currentchannel.wav");
        register("// User entered your channel", "sounds/neutral_connection_connected_currentchannel.wav");
        register("// Channel switched", "sounds/channel_switched.wav");
        register("// You were moved", "sounds/you_were_moved.wav");
        register("// Disconnected", "sounds/disconnected.wav");
        register("// Connected", "sounds/connected.wav");
        register("// Server group assigned", "sounds/servergroup_assigned.wav");
        register("// User was kicked out of your channel", "sounds/neutral_kicked_channel_awayfromcurrentchannel.wav");
        register("// User was kicked to your channel", "sounds/neutral_kicked_channel_tocurrentchannel.wav");
        register("// User in your channel was kicked from the server", "sounds/neutral_kicked_server_currentchannel.wav");

        register("!! You were kicked from the channel", "sounds/you_kicked_channel.wav");
        register("!! You have been kicked from the server", "sounds/you_kicked_server.wav");
        register("!! Insufficient permissions", "sounds/insufficient_permissions.wav");

        register("## Sound resumed", "sounds/sound_resumed.wav");
        register("## Sound muted", "sounds/sound_muted.wav");
    }

    public void register(String prefix, String soundPath) {
        prefixSoundMap.put(prefix.toLowerCase(), soundPath);
    }

    /**
     * Plays sound only if NO sound is currently playing.
     */
    public boolean playForString(String input) {
        if (input == null) return false;

        synchronized (this) {
            if (isPlaying) return false; // strict check
        }

        String lower = input.toLowerCase();

        for (Map.Entry<String, String> entry : prefixSoundMap.entrySet()) {
            if (lower.startsWith(entry.getKey())) {
                playSound(entry.getValue());
                return true;
            }
        }
        if (lower.startsWith("!! ")) {
            playSound(errorSoundPath);
            return true;
        }

        return false;
    }

    private void playSound(String resourcePath) {
        synchronized (this) {
            if (isPlaying) return; // double-check inside
            isPlaying = true; // lock
        }

        try {
            var url = SoundHandler.class.getClassLoader().getResource(resourcePath);
            if (url == null) throw new RuntimeException("Sound not found: " + resourcePath);

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    synchronized (this) {
                        isPlaying = false; // release lock
                    }
                }
            });

            clip.start();

        } catch (Exception e) {
            synchronized (this) { isPlaying = false; } // release lock on error
            throw new RuntimeException("Failed to play sound: " + resourcePath, e);
        }
    }
}
