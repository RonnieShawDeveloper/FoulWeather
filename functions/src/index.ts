// functions/src/index.ts

import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import axios from 'axios';
import { Buffer } from 'buffer';
// --- Firebase Functions v2 specific imports ---
import { onSchedule, ScheduledEvent } from 'firebase-functions/v2/scheduler';

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

// --- Utility Functions (UNCHANGED) ---
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

        // --- OPENAI PROMPT (UNCHANGED as per user request) ---
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

                                         - Begin with: “This weather rant was issued at [time only, e.g., 7:44 PM]”. Incorporate this into the first paragraph.
                                         - Use both the UPDATE and DISCUSSION sections if available. If there are older versions of the discussion, incorporate this information into your forecast.
                                         - DO NOT use AVIATION, MARINE or FIRE WEATHER in your rant specifically. Just incorporate any interesting points into your forecast.
                                         - Split the forecast into short, punchy, aggressive paragraphs with two line breaks.
                                         - Pretend that there are two people and each will take turns reading a paragraph.
                                         - Do not add speaker assignments, just put into paragraphs.
                                         - The first speaker is a woman and the other is a man. They will each take a turn reading the paragraphs.
                                         - Make the summary sound like the two people are having a conversation about the weather.
                                         - Include actual forecast details: storm chances, temperature swings, moisture, patterns, weird atmospheric garbage.
                                         - If there are WATCHES/WARNINGS/ADVISORIES, mention them with derision. If not, accuse the listener of hoping for severe weather disasters and tornadoes.

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
                                         `,
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
                temperature: 1.0,         // 🔥 More sarcasm, wit, unpredictability
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

        // Ensure the API Key is a string and not null/undefined
        if (!GEMINI_API_KEY) {
            functions.logger.error('Missing Gemini API key from Remote Config.');
            return null;
        }

        functions.logger.info(`Gemini TTS Input Character Count: ${formattedText.length}`);

        formattedText = "TTS the following conversation between Speaker1 and Speaker2 in a fast paced humorous sarcastic tone:\n" + formattedText
        const payload = {
            contents: [{ parts: [{ text: formattedText }] }],
            generationConfig: {
                responseModalities: ["AUDIO"],
                speechConfig: {
                    multiSpeakerVoiceConfig: {
                        speakerVoiceConfigs: [
                            { speaker: "Speaker1", voiceConfig: { prebuiltVoiceConfig: { voiceName: "Zephyr" } } },
                            { speaker: "Speaker2", voiceConfig: { prebuiltVoiceConfig: { voiceName: "Fenrir" } } }
                        ]
                    }
                }
            },
            model: "gemini-2.5-flash-preview-tts"
        };

        // --- FIX: Ensure the URL string is not wrapped in Markdown []() ---
        const apiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent?key=${GEMINI_API_KEY}`;

        const response = await axios.post(
            apiUrl, // Use the correctly formed URL string
            payload
        );

        const base64Audio = response.data?.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data;
        return base64Audio ? Buffer.from(base64Audio, 'base64') : null;
    } catch (error) {
        functions.logger.error("Error generating audio:", error);
        return null;
    }
}

// --- Core WFO Processing Logic (UNCHANGED - this is your "single service worker") ---
async function processWfo(wfoIdentifier: string): Promise<void> {
    functions.logger.info(`Processing WFO: ${wfoIdentifier}`);
    const wfoDocRef = firestore.collection('wfo_audio_metadata').doc(wfoIdentifier);

    const afd = await fetchAfdText(wfoIdentifier);
    if (!afd) {
        functions.logger.warn(`Skipping ${wfoIdentifier}: No AFD text found or error fetching.`);
        return;
    }

    const wfoDoc = await wfoDocRef.get();
    if (wfoDoc.exists && afd.issuanceTime <= wfoDoc.data()?.lastProcessedIssuanceTime) {
        functions.logger.info(`AFD unchanged for ${wfoIdentifier}. Skipping processing.`);
        return;
    }

    const template = await remoteConfig.getTemplate();
    const prerollAdText = (template.parameters as any)['preroll_ad_text']?.defaultValue?.value as string || '';
    const postrollAdText = (template.parameters as any)['postroll_ad_text']?.defaultValue?.value as string || '';

    const rawSummaryText = await generateSarcasticText(afd.text);
    if (!rawSummaryText) {
        functions.logger.warn(`Skipping ${wfoIdentifier}: Failed to generate sarcastic text.`);
        return;
    }

    const paragraphs = rawSummaryText.split(/\n\s*\n|\n/).filter(p => p.trim());
    let combinedFormattedText = '';

    if (prerollAdText) combinedFormattedText += `Speaker2: ${prerollAdText}\n\n`;
    paragraphs.forEach((p, i) => {
        const speaker = i % 2 === 0 ? 'Speaker1' : 'Speaker2';
        combinedFormattedText += `${speaker}: ${p.trim()}\n`;
    });
    if (postrollAdText) combinedFormattedText += `\n\nSpeaker1: ${postrollAdText}`;

    const rawPcmAudio = await generateAudioFromText(combinedFormattedText);
    if (!rawPcmAudio) {
        functions.logger.warn(`Skipping ${wfoIdentifier}: Failed to generate audio.`);
        return;
    }

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

// --- Hardcoded WFO Batches (by Office Name) ---
// These batches are designed for even distribution across time zones and will be used by the scheduled dispatchers.
const WFO_PROCESSING_BATCHES = [
  // Minute :00 / :30 Run (Batch 1 of 6 WFOs)
  ["Wakefield", "Aberdeen", "Albuquerque", "Boise", "Anchorage", "Honolulu"],
  // Minute :01 / :31 Run (Batch 2 of 6 WFOs)
  ["Albany", "La Crosse", "Boulder", "Eureka", "Fairbanks", "Tiyan (Guam)"],
  // Minute :02 / :32 Run (Batch 3 of 6 WFOs)
  ["Binghamton", "Bismarck", "Billings", "Eugene", "Juneau", "Pago Pago"],
  // Minute :03 / :33 Run (Batch 4 of 6 WFOs)
  ["Boston/Norton", "Birmingham", "Cheyenne", "San Joaquin Valley (Hanford)", "Columbia", "Caribou"],
  // Minute :04 / :34 Run (Batch 5 of 6 WFOs)
  ["Burlington", "Dodge City", "El Paso", "Los Angeles/Oxnard", "Charleston (SC)", "Cleveland"],
  // Minute :05 / :35 Run (Batch 6 of 6 WFOs)
  ["Buffalo", "Duluth", "Flagstaff", "Midland/Odessa", "State College", "Peachtree City"],
  // Minute :06 / :36 Run (Batch 7 of 6 WFOs)
  ["Charleston (SC)", "Des Moines", "Grand Junction", "Medford", "Greenville-Spartanburg", "Portland (ME)"],
  // Minute :07 / :37 Run (Batch 8 of 6 WFOs)
  ["Cleveland", "Detroit/Pontiac", "Missoula", "Monterey", "Huntsville", "Wilmington (NC)"],
  // Minute :08 / :38 Run (Batch 9 of 6 WFOs)
  ["State College", "Quad Cities", "Pocatello", "Spokane", "Wilmington (OH)", "Northern Indiana"],
  // Minute :09 / :39 Run (Batch 10 of 6 WFOs)
  ["Peachtree City", "Kansas City/Pleasant Hill", "Riverton", "Pendleton", "Jacksonville", "Key West"],
  // Minute :10 / :40 Run (Batch 11 of 6 WFOs)
  ["Greenville-Spartanburg", "Grand Forks", "Salt Lake City", "Portland (OR)", "Melbourne", "Miami"],
  // Minute :11 / :41 Run (Batch 12 of 6 WFOs)
  ["Portland (ME)", "Sioux Falls", "Tucson", "Phoenix", "Newport/Morehead City", "New York (Upton)"],
  // Minute :12 / :42 Run (Batch 13 of 6 WFOs)
  ["Huntsville", "Dallas/Fort Worth", "Las Vegas", "Reno", "Pittsburgh", "Mount Holly"],
  // Minute :13 / :43 Run (Batch 14 of 6 WFOs)
  ["Wilmington (NC)", "Hastings", "San Diego", "Raleigh/Durham", "Charleston (WV)", "Roanoke"],
  // Minute :14 / :44 Run (Batch 15 of 6 WFOs)
  ["Wilmington (OH)", "Goodland", "Seattle/Tacoma", "New York (Buffalo)", "San Juan", "Tallahassee"],
  // Minute :15 / :45 Run (Batch 16 of 6 WFOs)
  ["Northern Indiana", "Green Bay", "San Diego", "Tampa Bay Area", "Tallahassee", "North Platte"],
  // Minute :16 / :46 Run (Batch 17 of 6 WFOs)
  ["Jacksonville", "Grand Rapids", "Sacramento", "Lake Charles", "New Orleans/Baton Rouge", "Louisville"],
  // Minute :17 / :47 Run (Batch 18 of 5 WFOs)
  ["Key West", "Wichita", "Chicago", "St. Louis", "Lubbock"],
  // Minute :18 / :48 Run (Batch 19 of 5 WFOs)
  ["Melbourne", "Lincoln (IL)", "Little Rock", "Memphis", "Mobile"],
  // Minute :19 / :49 Run (Batch 20 of 5 WFOs)
  ["Miami", "Indianapolis", "Minneapolis/St. Paul", "Marquette", "Morristown"],
  // Minute :20 / :50 Run (Batch 21 of 5 WFOs)
  ["Newport/Morehead City", "Jackson", "Omaha/Valley", "Nashville", "Norman (Oklahoma City)"]
];

// --- Helper to Map Office Name to WFO Code ---
// This map is needed because processWfo expects the WFO code (e.g., "AKQ"), not the name ("Wakefield").
const OFFICE_NAME_TO_CODE_MAP: { [key: string]: string } = {
    "Albuquerque": "ABQ", "Aberdeen": "ABR", "Anchorage": "AFC", "Fairbanks": "AFG", "Juneau": "AJK",
    "Wakefield": "AKQ", "Albany": "ALY", "Amarillo": "AMA", "Gaylord": "APX", "La Crosse": "ARX",
    "Binghamton": "BGM", "Bismarck": "BIS", "Birmingham": "BMX", "Boise": "BOI", "Boulder": "BOU",
    "Boston/Norton": "BOX", "Brownsville": "BRO", "Burlington": "BTV", "Buffalo": "BUF", "Billings": "BYZ",
    "Columbia": "CAE", "Caribou": "CAR", "Charleston (SC)": "CHS", "Cleveland": "CLE", "Corpus Christi": "CRP",
    "State College": "CTP", "Cheyenne": "CYS", "Dodge City": "DDC", "Duluth": "DLH", "Des Moines": "DMX",
    "Detroit/Pontiac": "DTX", "Quad Cities": "DVN", "Kansas City/Pleasant Hill": "EAX", "Eureka": "EKA",
    "El Paso": "EPZ", "Eugene": "EUG", "Austin/San Antonio": "EWX", "Peachtree City": "FFC", "Grand Forks": "FGF",
    "Flagstaff": "FGZ", "Sioux Falls": "FSD", "Dallas/Fort Worth": "FWD", "Hastings": "GID", "Grand Junction": "GJT",
    "Goodland": "GLD", "Green Bay": "GRB", "Grand Rapids": "GRR", "Greenville-Spartanburg": "GSP", "Tiyan (Guam)": "GUM",
    "Portland (ME)": "GYX", "Honolulu": "HFO", "Houston/Galveston": "HGX", "San Joaquin Valley (Hanford)": "HNX",
    "Huntsville": "HUN", "Wichita": "ICT", "Wilmington (NC)": "ILM", "Wilmington (OH)": "ILN", "Lincoln (IL)": "ILX",
    "Indianapolis": "IND", "Northern Indiana": "IWX", "Jackson": "JAN", "Jacksonville": "JAX", "Key West": "KEY",
    "North Platte": "LBF", "Lake Charles": "LCH", "New Orleans/Baton Rouge": "LIX", "Louisville": "LMK",
    "Chicago": "LOT", "Los Angeles/Oxnard": "LOX", "St. Louis": "LSX", "Lubbock": "LUB", "Sterling": "LWX",
    "Little Rock": "LZK", "Midland/Odessa": "MAF", "Memphis": "MEG", "Miami": "MFL", "Medford": "MFR",
    "Newport/Morehead City": "MHX", "Melbourne": "MLB", "Mobile": "MOB", "Minneapolis/St. Paul": "MPX",
    "Marquette": "MQT", "Morristown": "MRX", "Missoula": "MSO", "Monterey": "MTR", "Nashville": "OHX",
    "New York (Upton)": "OKX", "Norman (Oklahoma City)": "OUN", "Spokane": "OTX", "Paducah": "PAH",
    "Pittsburgh": "PBZ", "Pendleton": "PDT", "Mount Holly": "PHI", "Pocatello": "PIH", "Portland (OR)": "PQR",
    "Pago Pago": "PPG", "Phoenix": "PSR", "Pueblo": "PUB", "Raleigh/Durham": "RAH", "Reno": "REV",
    "Riverton": "RIW", "Charleston (WV)": "RLX", "Roanoke": "RNK", "New York (Buffalo)": "ROC",
    "South Bend": "SBN", "Seattle/Tacoma": "SEW", "Springfield (MO)": "SGF", "San Diego": "SGX",
    "Shreveport": "SHV", "San Angelo": "SJT", "San Juan": "SJU", "Salt Lake City": "SLC",
    "Sacramento": "STO", "Tallahassee": "TAE", "Tampa Bay Area": "TBW", "Topeka": "TOP", "Tulsa": "TSA",
    "Tucson": "TWC", "Rapid City": "UNR", "Las Vegas": "VEF"
};


// --- New Helper Function to Process a Batch of WFOs with Concurrency ---
async function processSpecificWfoBatch(wfoOfficeNames: string[], concurrencyLimit: number): Promise<void> {
    functions.logger.info(`Starting batch processing for ${wfoOfficeNames.length} WFOs with concurrency ${concurrencyLimit}.`);

    const wfoCodesToProcess = wfoOfficeNames.map(name => {
        const code = OFFICE_NAME_TO_CODE_MAP[name];
        if (!code) {
            functions.logger.error(`Error: WFO code not found for office name: ${name}. This office will be skipped.`);
            return null; // Return null if mapping fails
        }
        return code;
    }).filter(Boolean) as string[]; // Filter out any nulls in case of missing map entries

    if (wfoCodesToProcess.length === 0) {
        functions.logger.warn(`No valid WFO codes to process in this batch after mapping. Skipping batch.`);
        return; // Exit if no valid WFOs to process
    }


    const processingQueue = [...wfoCodesToProcess]; // Create a mutable copy of WFO codes
    let activeProcesses = 0;
    const allProcessesFinished = new Promise<void>(resolve => {
        const checkCompletion = setInterval(() => {
            if (processingQueue.length === 0 && activeProcesses === 0) {
                clearInterval(checkCompletion);
                resolve();
            }
        }, 200); // Check every 200ms
    });

    const executeNext = () => {
        while (processingQueue.length > 0 && activeProcesses < concurrencyLimit) {
            const wfoCode = processingQueue.shift()!;
            activeProcesses++;

            processWfo(wfoCode)
                .catch(error => {
                    functions.logger.error(`Error processing WFO ${wfoCode} in batch:`, error);
                })
                .finally(() => {
                    activeProcesses--;
                    // The recursive call to executeNext is managed by the loop and the finally block,
                    // ensuring new tasks are picked up as slots free.
                });
        }
    };

    // Start initial set of processes
    executeNext();

    // Wait for all WFOs in the batch to finish processing
    await allProcessesFinished;
    functions.logger.info(`Finished batch processing for this dispatcher.`);
}

// --- Staggered Scheduled Functions (21 Dispatchers) ---
// Each function processes a specific hardcoded batch of WFOs at a staggered time.
const CONCURRENCY_LIMIT_PER_BATCH = 6; // Max 6 concurrent WFOs per function instance (as per plan)

// Generate and export ONLY the requested scheduled functions dynamically
const BATCH_INDICES_TO_DEPLOY = [15, 19]; // Corresponds to Batch 16 and Batch 20 (0-indexed)

BATCH_INDICES_TO_DEPLOY.forEach(index => {
    const batch = WFO_PROCESSING_BATCHES[index];
    // Schedule minutes: 0, 1, 2, ..., 20 for the first 30-min window
    const minute = index % 30; // Ensures minute stays within 0-29 for wrapping
    const cronSchedule = `${minute},${minute + 30} * * * *`; // e.g., '0,30 * * * *', '1,31 * * * *'
    const functionName = `scheduledSummaryGeneratorBatch${index}`; // Using 0-indexed for simplicity matching array

    if (batch) { // Ensure the batch exists at the given index
        (exports as any)[functionName] = onSchedule(
            { schedule: cronSchedule, timeoutSeconds: 540, memory: '1GiB' }, // 9 minutes timeout, 1GiB memory
            async (_event: ScheduledEvent) => {
                functions.logger.info(`Running ${functionName} for Batch ${index} at minute ${minute}. Processing ${batch.length} WFOs.`);
                await processSpecificWfoBatch(batch, CONCURRENCY_LIMIT_PER_BATCH);
                functions.logger.info(`Completed ${functionName} for Batch ${index}.`);
            }
        );
    } else {
        functions.logger.error(`Error: Attempted to deploy non-existent batch at index ${index}.`);
    }
});
