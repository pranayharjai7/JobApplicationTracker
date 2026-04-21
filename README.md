# 📋 JobTrack — Smart Job Application Tracker

An intelligent Android application built with **Kotlin** and **Jetpack Compose** that helps you organize and track your job search journey automatically by syncing with your Gmail and using advanced AI for data extraction.

## ✨ Features

-   **🔄 Gmail Sync:** Automatically scans your inbox for job-related emails (applications, interviews, offers) with a modern, animated sync control.
-   **🤖 Flexible AI Provider System:** Powered by a robust backend supporting **Groq (Llama 3)**, **Google Gemini**, and **OpenRouter** with intelligent fallback logic.
-   **🛤️ Journey View:** Visualize the entire chronological timeline of your application stages, from initial submission to final offer or rejection.
-   **🔥 Activity Heatmap:** Track your job search intensity over time with a GitHub-style activity heatmap and time-based filtering.
-   **🔍 Advanced Smart Filtering:** 
    -   **AI-Powered Suggestions:** Get smart filter recommendations based on your activity.
    -   **Multi-Stage Tracking:** Filter by specific application statuses.
    -   **Company Normalization:** Consolidated views for applications to the same company across different roles.
-   **👥 Multi-Account Support:** Connect and manage multiple Gmail accounts simultaneously with a refined account switcher and per-account data isolation.
-   **📊 Dashboard:** A Material 3 interface featuring stats, recent activity, and quick-access filters.
-   **🎨 Modern UI/UX:**
    -   **Splash Screen:** Clean entry experience with branded splash support.
    -   **Privacy-First:** Dedicated Privacy Policy access within the app.
    -   **Localization:** Multi-language support for key UI components.
-   **🔒 Secure & Private:** 
    -   **Biometric Security:** Optional biometric lock (Fingerprint/Face) to protect your job search data.
    -   **On-Device Processing:** Scanning and matching of emails happen primarily on your device.
    -   **Encrypted Storage:** Applications and statuses are stored in a secure on-device **Room Database** encrypted with **SQLCipher** and **AES-256**.
    -   **Data Integrity:** We **never** sell, rent, or trade your Gmail data. Information is only sent to AI providers for extraction and is not used for model training.
    -   **Google Limited Use:** Fully compliant with [Google API Service User Data Policy](https://developers.google.com/terms/api-services-user-data-policy).
    -   **Full Control:** Wipe all data or remove accounts at any time from the app settings.

## 🛠️ Tech Stack

-   **Language:** [Kotlin](https://kotlinlang.org/)
-   **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
-   **AI Integration:** 
    -   Groq SDK / Llama 3
    -   Google AI SDK (Gemini Flash)
    -   OpenRouter API
-   **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
-   **Database:** [Room](https://developer.android.com/training/data-storage/room) with KSP and **SQLCipher** for encryption
-   **Security:** AndroidX Biometric, EncryptedSharedPreferences (Security-Crypto)
-   **Networking:** Gmail API, Retrofit/HttpURLConnection for AI Providers
-   **Architecture:** MVVM with Clean Architecture principles

## 🚀 Getting Started

### Prerequisites

-   Android Studio Iguana or newer.
-   A Google Cloud Project with Gmail API enabled.
-   API Keys for your preferred AI providers (Groq, Gemini, or OpenRouter).

### Setup

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/pranayharjai7/JobApplicationTracker.git
    ```

2.  **Add your API Keys:**
    Create a `local.properties` file in the root directory and add your keys:
    ```properties
    GEMINI_API_KEY=your_gemini_key
    GROQ_API_KEY=your_groq_key
    OPENROUTER_API_KEY=your_openrouter_key
    ```
    *The app will use available keys in order of preference: Groq -> Gemini -> OpenRouter.*

3.  **Google Services:**
    -   Place your `google-services.json` in the `app/` directory.
    -   Configure your OAuth 2.0 Client ID for Android in the Google Cloud Console.

4.  **Build & Run:**
    Sync Gradle and run the app.

## 📸 Screenshots

| Dashboard & Heatmap | Application Journey | Smart Filters |
| :---: | :---: | :---: |
| ![Dashboard](https://via.placeholder.com/300x600?text=Heatmap+&+Dashboard) | ![Journey](https://via.placeholder.com/300x600?text=Application+Timeline) | ![Filters](https://via.placeholder.com/300x600?text=AI+Smart+Filters) |

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit PRs for new AI provider integrations or UI enhancements.

## 📜 License

```text
Copyright 2026 Pranay Harjai

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
