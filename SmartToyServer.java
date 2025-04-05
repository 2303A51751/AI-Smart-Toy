import static spark.Spark.*;

import com.google.gson.Gson;
import org.vosk.LibVosk;
import org.vosk.Model;
import org.vosk.Recognizer;

import javax.sound.sampled.*;
import java.util.HashMap;

public class SmartToyServer {
    static int difficulty = 1;
    static String lastCorrectWord = "cat";

    public static void main(String[] args) {
        LibVosk.init();

        staticFiles.location("/public");

        get("/speak", (req, res) -> {
            String recognized = recognizeSpeech();
            boolean isCorrect = recognized.contains(lastCorrectWord);

            if (isCorrect && difficulty < 3) {
                difficulty++;
            }

            HashMap<String, String> result = new HashMap<>();
            result.put("recognized", recognized);
            result.put("status", isCorrect ? "correct" : "wrong");
            result.put("difficulty", String.valueOf(difficulty));
            return new Gson().toJson(result);
        });
    }

    public static String recognizeSpeech() {
        try (Model model = new Model("model/vosk-model-small-en-us-0.15");
                Recognizer recognizer = new Recognizer(model, 16000)) {

            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            byte[] buffer = new byte[4096];
            long startTime = System.currentTimeMillis();
            String finalResult = "";

            while (System.currentTimeMillis() - startTime < 5000) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (recognizer.acceptWaveform(buffer, bytesRead)) {
                    finalResult = recognizer.getResult();
                }
            }

            line.stop();
            line.close();

            return finalResult.toLowerCase();

        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
}
