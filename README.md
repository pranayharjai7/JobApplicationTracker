# 📋 Job Application Tracker

An intelligent Android application built with **Kotlin** and **Jetpack Compose** that helps you organize and track your job search journey automatically by syncing with your Gmail.

## ✨ Features

-   **🔄 Gmail Sync:** Automatically scans your inbox for job-related emails (applications, interviews, offers).
-   **🤖 AI-Powered Parsing:** Uses **Google Gemini AI** to extract structured data from raw email content (Company, Job Title, Date, Status, etc.).
-   **📊 Dashboard:** A clean, Material 3-based interface to view all your applications at a glance.
-   **🔍 Detailed View:** Dive into the specifics of each application, including parsed notes and recruiter contact info.
-   **📂 Local Storage:** Keeps your data persistent and accessible offline using **Room Database**.
-   **🔒 Secure Auth:** Integrated with **Google Sign-In** for seamless and secure access to your emails.

## 🛠️ Tech Stack

-   **Language:** [Kotlin](https://kotlinlang.org/)
-   **UI Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
-   **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
-   **Database:** [Room](https://developer.android.com/training/data-storage/room)
-   **Networking & APIs:**
    -   Gmail API
    -   Google Play Services Auth
    -   Google AI SDK (Gemini)
-   **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles.

## 🚀 Getting Started

### Prerequisites

-   Android Studio Iguana or newer.
-   A Google Cloud Project with Gmail API enabled.
-   A Gemini API Key from [Google AI Studio](https://aistudio.google.com/).

### Setup

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/pranayharjai7/JobApplicationTracker.git
    ```

2.  **Add your API Keys:**
    Create a `local.properties` file in the root directory and add your Gemini API Key:
    ```properties
    GEMINI_API_KEY=your_gemini_api_key_here
    ```

3.  **Google Services:**
    -   Place your `google-services.json` in the `app/` directory.
    -   Configure your OAuth 2.0 Client ID in the Google Cloud Console.

4.  **Build & Run:**
    Sync the project with Gradle files and run the app on your device or emulator.

## 📸 Screenshots

| Dashboard | Application Detail |
| :---: | :---: |
| ![Dashboard](https://via.placeholder.com/300x600?text=Dashboard) | ![Detail](https://via.placeholder.com/300x600?text=Application+Detail) |

## 🤝 Contributing

Contributions are welcome! If you find any bugs or have feature suggestions, please open an issue or submit a pull request.

## 📜 License

```text
Copyright 2024 Pranay

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
