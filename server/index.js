const express = require("express");

const app = express();
app.use(express.json({ limit: "1mb" }));

const PORT = process.env.PORT || 3000;
const API_KEY = process.env.OPENAI_API_KEY;

app.get("/", (req, res) => {
  res.json({
    app: "MYCROFT XL API",
    status: "online"
  });
});

app.post("/chat", async (req, res) => {
  try {
    if (!API_KEY) {
      return res.status(500).json({
        error: "Server API key is not configured"
      });
    }

    const message = String(req.body.message || "").trim();

    if (!message || message.length > 10000) {
      return res.status(400).json({
        error: "Message is empty or too long"
      });
    }

    const response = await fetch(
      "https://api.openai.com/v1/responses",
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${API_KEY}`
        },
        body: JSON.stringify({
          model: "gpt-6-astra",
          instructions:
            "You are MYCROFT XL, a helpful AI assistant. " +
            "Understand Bengali and English. Reply in the same language " +
            "used by the user. Be friendly, accurate and concise.",
          input: message,
          store: false
        })
      }
    );

    const data = await response.json();

    if (!response.ok) {
      console.error("OpenAI API error:", data);
      return res.status(response.status).json({
        error: data.error?.message || "OpenAI request failed"
      });
    }

    let reply = "";

    for (const item of (data.output || [])) {
      if (item.type === "message") {
        for (const part of (item.content || [])) {
          if (part.type === "output_text") {
            reply += part.text;
          }
        }
      }
    }

    if (!reply) {
      reply = "দুঃখিত, AI কোনো উত্তর দিতে পারেনি।";
    }

    res.json({ reply });

  } catch (error) {
    console.error(error);
    res.status(500).json({
      error: "Server connection error"
    });
  }
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`MYCROFT XL API running on port ${PORT}`);
});
