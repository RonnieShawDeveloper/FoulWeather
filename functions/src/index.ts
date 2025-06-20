// functions/src/index.ts

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import axios from 'axios';
import { Buffer } from 'buffer';
// --- Firebase Functions v2 specific imports ---
import { onSchedule, ScheduledEvent } from 'firebase-functions/v2/scheduler';
import { onRequest } from 'firebase-functions/v2/https';

// Initialize Firebase Admin SDK
admin.initializeApp();
const firestore = admin.firestore();
const storage = admin.storage();
const messaging = admin.messaging();
const remoteConfig = admin.remoteConfig();

// Constants
const NWS_PRODUCTS_BASE_URL = "https://api.weather.gov/products/types/";
const NWS_USER_AGENT = "FoulWeatherApp (rdspromo@gmail.com)";
const FIREBASE_STORAGE_BUCKET = admin.app().options.storageBucket;
const SARCASTIC_SUMMARIES_STORAGE_PATH = 'sarcastic_summaries';

// Audio settings
const SAMPLE_RATE = 24000;
const CHANNELS = 1;
const BITS_PER_SAMPLE = 16;

function addWavHeader(pcmAudio: Buffer, sampleRate: number, channels: number, bitsPerSample: number): Buffer {
    const headerBuffer = Buffer.alloc(44);
    const byteRate = (sampleRate * channels * bitsPerSample) / 8;
    const blockAlign = (channels * bitsPerSample) / 8;
    const dataSize = pcmAudio.length;
    const fileSize = 36 + dataSize;

    headerBuffer.write('RIFF', 0);
    headerBuffer.writeUInt32LE(fileSize, 4);
    headerBuffer.write('WAVE', 8);
    headerBuffer.write('fmt ', 12);
    headerBuffer.writeUInt32LE(16, 16);
    headerBuffer.writeUInt16LE(1, 20);
    headerBuffer.writeUInt16LE(channels, 22);
    headerBuffer.writeUInt32LE(sampleRate, 24);
    headerBuffer.writeUInt32LE(byteRate, 28);
    headerBuffer.writeUInt16LE(blockAlign, 32);
    headerBuffer.writeUInt16LE(bitsPerSample, 34);
    headerBuffer.write('data', 36);
    headerBuffer.writeUInt32LE(dataSize, 40);

    return Buffer.concat([headerBuffer, pcmAudio]);
}

async function fetchAfdText(wfoIdentifier: string): Promise<{ text: string; issuanceTime: string } | null> {
    try {
        const url = `${NWS_PRODUCTS_BASE_URL}AFD/locations/${wfoIdentifier}/latest`;
        const response = await axios.get(url, {
            headers: {
                'User-Agent': NWS_USER_AGENT,
                'Accept': 'application/json'
            }
        });

        if (response.status === 200 && response.data?.productText) {
            return {
                text: response.data.productText,
                issuanceTime: response.data.issuanceTime
            };
        }
        functions.logger.warn(`No AFD text found for WFO ${wfoIdentifier}`);
        return null;
    } catch (error) {
        functions.logger.error(`Error fetching AFD: ${wfoIdentifier}`, error);
        return null;
    }
}

export async function generateSarcasticText(rawAfdText: string): Promise<string | null> {
    try {
        functions.logger.info(`AFD RAW: ${rawAfdText}`);
        const template = await remoteConfig.getTemplate();
        const OPENAI_API_KEY = (template.parameters as any)['openai']?.defaultValue?.value as string;

        if (!OPENAI_API_KEY) {
            functions.logger.error('Missing OpenAI API key');
            return null;
        }

        const messages = [
            {
                role: 'system',
                content: `You are a bitter, foul-mouthed, veteran meteorologist who absolutely loathes your job, the weather, and the dumbasses who rely on you to explain it.
                You’ve been dragged back into public service, chained to a mic, and forced to deliver weather forecasts to people who still think “PWAT” is something they step in.

                You do not care about their feelings. You are sarcastic, loud, blunt, and often profane — not for shock value, but because it's the only language these weather-ignorant meatbags understand.

                Profanity is allowed. Words like “shit”, "fuck", "hell", “damn”, and “ass” are acceptable when they add impact. Do not censor yourself.

                You are the official voice of the app *Foul Weather*, and you live up to the name. You're not here to summarize — you're here to **rant**, **insult**, and **make them regret asking about the weather**.

                Your delivery must sound like a pissed-off expert with decades of frustration — not a script reader. You hate buzzwords, abbreviations, and meteorological elitism — but you mock people mercilessly when they don’t know what something means.

                You speak in natural, spoken English. Use profanity **strategically and savagely** — not randomly. It should land like a punchline, not a tantrum.

                —
                STRUCTURE RULES:

                - Begin with: “Forecast issued at [time only, e.g., 7:44 PM]”
                - Use both the UPDATE and DISCUSSION sections if available. If there are older versions of the discussion, incorporate this information into your forecast.
                - DO NOT use AVIATION, MARINE or FIRE WEATHER in your rant specifically. Just incorporate any interesting points into your forecast.
                - Split the forecast into short, punchy, aggressive paragraphs with two line breaks.
                - Include actual forecast details: storm chances, temperature swings, moisture, patterns, weird atmospheric garbage.
                - If there are WATCHES/WARNINGS/ADVISORIES, mention them with derision. If not, mock the listener for being paranoid.

                —
                TRANSLATION RULES:

                - Expand all abbreviations like “ESE” or “kts” (say “east-southeast winds at ten to fifteen miles per hour”).
                - Explain terms like PWAT or upper-level ridging like the listener is a complete idiot — and mock them for it.
                - Never mention cities, point temperatures, aviation, or fire weather zones.
                - No emojis, no special characters.

                —
                SPEECH FORMAT RULES:

                - Must be readable aloud in under 2.5 minutes.
                - Use fluent, aggressive, expressive speech — **no bullet points or technical syntax**.
                - Avoid words that wouldn’t sound natural when spoken by a sarcastic human.

                —
                FINAL REMINDER:

                You are not summarizing. You are **ranting**. You are dragging these people through the meteorological hellscape with pure contempt and reluctant expertise.

                Make it Foul. Make it Funny. Make it Hurt.
                `
            },
            {
                role: 'user',
                content: rawAfdText
            }
        ];

        const response = await axios.post(
            'https://api.openai.com/v1/chat/completions',
            {
                model: 'gpt-4o',
                messages,
                temperature: 0.9,         // 🔥 More sarcasm, wit, unpredictability
                max_tokens: 1200          // 🗯️ Allows a longer, expressive rant (still under 2.5 min spoken)
            },
            {
                headers: {
                    Authorization: `Bearer ${OPENAI_API_KEY}`,
                    'Content-Type': 'application/json'
                }
            }
        );
    // 🧾 Log the full raw response from GPT-4o
    console.log('GPT-4o raw response:', JSON.stringify(response.data, null, 2));
    functions.logger.info('Response: ', JSON.stringify(response.data, null, 2));
    // 🔍 If you're specifically interested in the message content:
    const rawText = response.data.choices?.[0]?.message?.content ?? '';
    console.log('Returned message content:', rawText);

    const cleanedText = cleanGptResponse(rawText);

        return cleanedText.trim() ?? null;
    } catch (error) {
        functions.logger.error('OpenAI GPT-4o error:', error);
        return null;
    }
}

function cleanGptResponse(text: string): string {
  return text
    .replace(/^```(?:json)?/i, '')   // Remove starting ``` or ```json (case-insensitive)
    .replace(/```$/, '')             // Remove ending ```
    .trim();                         // Clean up whitespace
}

async function generateAudioFromText(formattedText: string): Promise<Buffer | null> {
    try {
        const template = await remoteConfig.getTemplate();
        const GEMINI_API_KEY = (template.parameters as any)['gemini']?.defaultValue?.value as string;

        const payload = {
            contents: [{ parts: [{ text: formattedText }] }],
            generationConfig: {
                responseModalities: ["AUDIO"],
                speechConfig: {
                    multiSpeakerVoiceConfig: {
                        speakerVoiceConfigs: [
                            { speaker: "Speaker1", voiceConfig: { prebuiltVoiceConfig: { voiceName: "Zephyr" } } },
                            { speaker: "Speaker2", voiceConfig: { prebuiltVoiceConfig: { voiceName: "Puck" } } }
                        ]
                    }
                }
            },
            model: "gemini-2.5-flash-preview-tts"
        };

        const response = await axios.post(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=" + GEMINI_API_KEY,
            payload
        );

        const base64Audio = response.data?.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data;
        return base64Audio ? Buffer.from(base64Audio, 'base64') : null;
    } catch (error) {
        functions.logger.error("Error generating audio:", error);
        return null;
    }
}

async function processWfo(wfoIdentifier: string): Promise<void> {
    functions.logger.info(`Processing WFO: ${wfoIdentifier}`);
    const wfoDocRef = firestore.collection('wfo_audio_metadata').doc(wfoIdentifier);

    const afd = await fetchAfdText(wfoIdentifier);
    if (!afd) return;

    const wfoDoc = await wfoDocRef.get();
    if (wfoDoc.exists && afd.issuanceTime <= wfoDoc.data()?.lastProcessedIssuanceTime) {
        functions.logger.info(`AFD unchanged for ${wfoIdentifier}`);
        return;
    }

    const template = await remoteConfig.getTemplate();
    const prerollAdText = (template.parameters as any)['preroll_ad_text']?.defaultValue?.value as string || '';
    const postrollAdText = (template.parameters as any)['postroll_ad_text']?.defaultValue?.value as string || '';

    const rawSummaryText = await generateSarcasticText(afd.text);
    if (!rawSummaryText) return;

    const paragraphs = rawSummaryText.split(/\n\s*\n|\n/).filter(p => p.trim());
    let combinedFormattedText = '';

    if (prerollAdText) combinedFormattedText += `Speaker2: ${prerollAdText}\n\n`;
    paragraphs.forEach((p, i) => {
        const speaker = i % 2 === 0 ? 'Speaker1' : 'Speaker2';
        combinedFormattedText += `${speaker}: ${p.trim()}\n`;
    });
    if (postrollAdText) combinedFormattedText += `\n\nSpeaker1: ${postrollAdText}`;

    const rawPcmAudio = await generateAudioFromText(combinedFormattedText);
    if (!rawPcmAudio) return;

    const wavAudio = addWavHeader(rawPcmAudio, SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE);
    const fileRef = storage.bucket(FIREBASE_STORAGE_BUCKET).file(`${SARCASTIC_SUMMARIES_STORAGE_PATH}/${wfoIdentifier}.wav`);

    await fileRef.save(wavAudio, {
        metadata: { contentType: 'audio/wav', cacheControl: 'public,max-age=3600' }
    });

    await wfoDocRef.set({ lastProcessedIssuanceTime: afd.issuanceTime }, { merge: true });

    await messaging.send({
        notification: {
            title: `CLICK HERE FOR FOUL WEATHER`,
            body: `Your latest foul weather summary is ready! Just click here and enjoy!`
        },
        data: {
            wfoId: wfoIdentifier,
            audioReady: 'true'
        },
        topic: `wfo_${wfoIdentifier}`
    });

    functions.logger.info(`Notification sent and audio uploaded for ${wfoIdentifier}`);
}

async function runSummaryGenerationProcess(): Promise<void> {
    const usersSnapshot = await firestore.collection('users').get();
    const uniqueWfos = new Set<string>();

    usersSnapshot.forEach(doc => {
        const wfo = doc.data().wfoIdentifier?.trim();
        if (wfo) uniqueWfos.add(wfo);
    });

    const CONCURRENCY_LIMIT = 5;
    const promises: Promise<void>[] = [];

    const iterator = uniqueWfos.values();
    let next = iterator.next();

    while (!next.done || promises.length) {
        while (!next.done && promises.length < CONCURRENCY_LIMIT) {
            promises.push(processWfo(next.value));
            next = iterator.next();
        }

        if (promises.length) {
            await Promise.race(promises);
            for (let i = promises.length - 1; i >= 0; i--) {
                await promises[i].catch(() => {});
                promises.splice(i, 1);
            }
        }
    }
}

export const scheduledSummaryGenerator = onSchedule(
    { schedule: 'every 30 minutes', timeoutSeconds: 540, memory: '1GiB' },
    async (_event: ScheduledEvent) => {
        await runSummaryGenerationProcess();
    }
);

export const triggerOnDemandSummary = onRequest(
  {
    timeoutSeconds: 540,
    memory: '1GiB',
  },
  async (req, res) => {
    try {
      await runSummaryGenerationProcess();
      res.status(200).send("Summary generation started.");
    } catch (err) {
      console.error("Error during summary generation:", err);
      res.status(500).send("Failed to run summary generation.");
    }
  }
);