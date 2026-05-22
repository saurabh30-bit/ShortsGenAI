# ShortsGenAI: Automated Video Studio

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)

ShortsGenAI is a state-of-the-art Android application that fully automates the creation and publishing of short-form vertical video content (YouTube Shorts, Instagram Reels, TikToks). 

It acts as an end-to-end pipeline: from generating viral scripts using AI, to rendering the video via Google Veo, and finally taking control of the device to automatically publish the video on social media networks!

## 🚀 Key Features

*   **AI Script & Prompt Generation:** Uses Google's Gemini API to take a simple user topic (e.g., "5 facts about the ocean") and generate a highly engaging, viral-optimized script alongside precise audio/visual prompts and hashtags.
*   **Veo Video Generation Automation:** Directly integrates with the Gemini Android app to inject the custom prompts into Veo to render high-quality vertical videos.
*   **AutoPublisher Bot (AccessibilityService):** Features a robust UI Automation Engine that uses Android's Accessibility APIs to:
    1.  Detect when a video finishes downloading to local storage.
    2.  Open the Instagram and YouTube apps natively via Android Share Intents (`ACTION_SEND`).
    3.  Auto-paste the generated captions and hashtags.
    4.  Navigate the UI and automatically click the "Share" / "Upload" buttons.
*   **Premium Glassmorphism UI:** Built entirely in Jetpack Compose, the dashboard features dynamic animated mesh gradients, frosted glass components, and premium typography for a state-of-the-art feel.

## 🛠️ Technical Architecture

*   **UI Framework:** Jetpack Compose (Material 3 + Custom Aethestics)
*   **State Management:** StateFlow & ViewModels
*   **Automation Engine:** `AccessibilityService` traversing the View node tree asynchronously.
*   **File Handling:** Direct `java.io.File` polling bridged with `FileProvider` to instantly hand off media across different app sandboxes.

## 📱 How It Works

1.  **Configure:** Input your Gemini API key (or use the built-in one) and your social media credentials.
2.  **Prompt:** Enter a topic in the UI (e.g., "History of Rome").
3.  **Run Automation:** The app triggers the `AutoPublisherService`. It wakes up the Gemini app, types the prompt, waits for Veo to generate and download the `.mp4` file, and then physically opens Instagram and YouTube to post the result on your behalf!

## ⚙️ Installation & Setup

1. Clone the repository.
2. Open the project in **Android Studio**.
3. (Optional) Add your custom API keys in `local.properties`:
   ```properties
   GEMINI_API_KEY="your_api_key_here"
   ```
4. Build and deploy to an Android Emulator or physical device (API 24+).
5. **CRITICAL:** When you launch the app, you MUST click "Grant Accessibility Permission" in the settings panel and enable the **ShortsGenAI Automation Bot** in your Android device's Accessibility settings. The bot cannot drive Instagram or YouTube without this permission!

## 🎨 Branches

*   `main`: Contains the core V2 Automation Bot logic.
*   `v3-professional-ui`: Contains the complete aesthetic overhaul with animations, glassmorphism, and gradients.
