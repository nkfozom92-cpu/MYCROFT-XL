package com.mycroft.xl;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.hardware.camera2.CameraManager;
import java.util.*;
import java.text.SimpleDateFormat;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public class MainActivity extends Activity {

    LinearLayout root, messages;
    EditText input;
    TextView status;
    TextToSpeech tts;
    SpeechRecognizer recognizer;
    boolean listening = false;
    boolean torch = false;
    String language = "bn-BD";
    static final String BACKEND_URL =
        "https://YOUR-RENDER-SERVICE.onrender.com/chat";

    int bg = Color.rgb(9,13,25);
    int purple = Color.rgb(139,92,246);

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);

        requestPermissions(new String[]{
            "android.permission.RECORD_AUDIO",
            "android.permission.CAMERA"
        }, 100);

        buildUI();

        tts = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("bn", "BD"));
                tts.setSpeechRate(0.92f);
            }
        });

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(Bundle b) {
                    status.setText("Listening...");
                }
                public void onBeginningOfSpeech() {}
                public void onRmsChanged(float r) {}
                public void onBufferReceived(byte[] b) {}
                public void onEndOfSpeech() {
                    status.setText("Processing...");
                }
                public void onError(int e) {
                    status.setText("Ready");
                    listening = false;
                }
                public void onResults(Bundle b) {
                    ArrayList<String> results =
                        b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    listening = false;
                    status.setText("Ready");
                    if (results != null && !results.isEmpty()) {
                        input.setText(results.get(0));
                        sendMessage();
                    }
                }
                public void onPartialResults(Bundle b) {}
                public void onEvent(int a, Bundle b) {}
            });
        }

        addMessage("MYCROFT XL", "Hello! আমি আপনার AI Assistant। কীভাবে সাহায্য করতে পারি?", false);
    }

    void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18,16,18,12);
        root.setBackgroundColor(bg);

        TextView title = new TextView(this);
        title.setText("✦ MYCROFT XL");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setPadding(5,18,5,10);
        root.addView(title);

        status = new TextView(this);
        status.setText("● ONLINE  |  AI ASSISTANT");
        status.setTextColor(0xFF34D399);
        status.setGravity(Gravity.CENTER);
        status.setPadding(5,8,5,15);
        root.addView(status);

        TextView orb = new TextView(this);
        orb.setText("◉");
        orb.setTextSize(75);
        orb.setTextColor(purple);
        orb.setGravity(Gravity.CENTER);
        root.addView(orb);

        TextView subtitle = new TextView(this);
        subtitle.setText("How can I help you today?");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        ScrollView scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0,12,0,12);
        scroll.addView(messages);

        root.addView(scroll, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Ask Mycroft anything...");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setPadding(12,8,12,8);
        input.setBackgroundColor(0xFF20283B);

        bar.addView(input, new LinearLayout.LayoutParams(
            0, 52, 1
        ));

        Button mic = new Button(this);
        mic.setText("🎙");
        mic.setOnClickListener(v -> startVoice());
        bar.addView(mic);

        Button send = new Button(this);
        send.setText("➤");
        send.setOnClickListener(v -> sendMessage());
        bar.addView(send);

        root.addView(bar);

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER);
        tools.setPadding(0,10,0,0);

        addTool(tools,"🔦","Flash");
        addTool(tools,"📷","Camera");
        addTool(tools,"🧮","Calc");
        addTool(tools,"🌐","Search");
        addTool(tools,"🕒","Time");

        root.addView(tools);
        setContentView(root);
    }

    void addTool(LinearLayout row, String icon, String name) {
        Button b = new Button(this);
        b.setText(icon + "\n" + name);
        b.setTextSize(10);
        b.setOnClickListener(v -> tool(name));
        row.addView(b, new LinearLayout.LayoutParams(0,65,1));
    }

    void addMessage(String who, String msg, boolean user) {
        TextView t = new TextView(this);
        t.setText(who + "\n" + msg);
        t.setTextColor(Color.WHITE);
        t.setTextSize(14);
        t.setPadding(15,12,15,12);
        t.setBackgroundColor(user ? 0xFF35305A : 0xFF20283B);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,5,0,5);
        messages.addView(t,p);
    }

    void sendMessage() {
        String q = input.getText().toString().trim();
        if (q.isEmpty()) return;

        addMessage("YOU", q, true);
        input.setText("");

        InputMethodManager imm =
            (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);

        String answer = processCommand(q);

        if (answer != null) {
            addMessage("MYCROFT XL", answer, false);
            speak(answer);
        } else {
            askAI(q);
        }
    }

    void askAI(String question) {
        status.setText("AI is thinking...");
        addMessage("MYCROFT XL", "আপনার প্রশ্নের উত্তর তৈরি হচ্ছে...", false);

        new Thread(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(BACKEND_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(90000);
                connection.setDoOutput(true);
                connection.setRequestProperty(
                    "Content-Type", "application/json; charset=UTF-8"
                );

                JSONObject body = new JSONObject();
                body.put("message", question);

                byte[] bytes = body.toString().getBytes("UTF-8");

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(bytes);
                }

                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] data = new byte[4096];
                int n;

                if (stream != null) {
                    while ((n = stream.read(data)) != -1) {
                        buffer.write(data, 0, n);
                    }
                    stream.close();
                }

                String result = buffer.toString("UTF-8");
                JSONObject json = new JSONObject(result);

                if (code >= 200 && code < 300) {
                    String reply = json.optString(
                        "reply", "দুঃখিত, উত্তর পাওয়া যায়নি।"
                    );

                    runOnUiThread(() -> {
                        addMessage("MYCROFT XL", reply, false);
                        status.setText("● ONLINE | AI CONNECTED");
                        speak(reply);
                    });
                } else {
                    String error = json.optString(
                        "error", "API request failed: " + code
                    );

                    runOnUiThread(() -> {
                        addMessage("SYSTEM", error, false);
                        status.setText("API ERROR");
                    });
                }

            } catch (Exception e) {
                String error = e.getMessage();

                runOnUiThread(() -> {
                    addMessage(
                        "SYSTEM",
                        "AI সংযোগে সমস্যা হয়েছে: " + error,
                        false
                    );
                    status.setText("Connection error");
                });

            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    String processCommand(String q) {
        String s = q.toLowerCase(Locale.ROOT);

        if (s.contains("time") || s.contains("সময়") || s.contains("সময়")) {
            return "Current time: " +
                new SimpleDateFormat("hh:mm a",Locale.getDefault())
                .format(new Date());
        }

        if (s.contains("date") || s.contains("তারিখ")) {
            return "Today: " +
                new SimpleDateFormat("dd MMMM yyyy",Locale.getDefault())
                .format(new Date());
        }

        if (s.contains("battery") || s.contains("চার্জ")) {
            Intent i = registerReceiver(null,
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i != null) {
                int level = i.getIntExtra("level",0);
                return "Battery level: " + level + "%";
            }
        }

        if (s.contains("flash") || s.contains("টর্চ")) {
            tool("Flash");
            return "Flashlight command executed.";
        }

        if (s.contains("camera") || s.contains("ক্যামেরা")) {
            tool("Camera");
            return "Opening camera.";
        }

        if (s.contains("calculator") || s.contains("ক্যালকুলেটর")) {
            tool("Calc");
            return "Opening calculator.";
        }

        if (s.contains("search") || s.contains("খুঁজে")) {
            tool("Search");
            return "Opening web search.";
        }

        if (s.contains("hello") || s.contains("হ্যালো")) {
            return "Hello! আমি Mycroft XL। আপনাকে কীভাবে সাহায্য করব?";
        }

        return null;
    }

    void startVoice() {
        if (recognizer == null) {
            status.setText("Speech recognition unavailable");
            return;
        }

        if (checkSelfPermission("android.permission.RECORD_AUDIO")
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                "android.permission.RECORD_AUDIO"
            },100);
            return;
        }

        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language);
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);

        try {
            listening = true;
            recognizer.startListening(i);
        } catch(Exception e) {
            listening = false;
            status.setText("Voice error");
        }
    }

    void speak(String text) {
        if (tts != null) {
            tts.setLanguage(new Locale("bn","BD"));
            tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"mycroft");
        }
    }

    void tool(String name) {
        try {
            Intent i = null;

            switch(name) {
                case "Camera":
                    i = new Intent("android.media.action.IMAGE_CAPTURE");
                    break;
                case "Calc":
                    i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_APP_CALCULATOR);
                    break;
                case "Search":
                    i = new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.google.com"));
                    break;
                case "Time":
                    i = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
                    break;
                case "Flash":
                    toggleFlash();
                    return;
            }

            if(i != null) startActivity(i);

        } catch(Exception e) {
            addMessage("SYSTEM","This feature is not available on this phone.",false);
        }
    }

    void toggleFlash() {
        try {
            CameraManager cm =
                (CameraManager)getSystemService(CAMERA_SERVICE);
            String id = cm.getCameraIdList()[0];
            torch = !torch;
            cm.setTorchMode(id,torch);
            status.setText(torch ? "Flashlight ON" : "Flashlight OFF");
        } catch(Exception e) {
            addMessage("SYSTEM","Flashlight unavailable.",false);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (recognizer != null) recognizer.destroy();
        super.onDestroy();
    }
}
