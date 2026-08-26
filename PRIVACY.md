# Privacy Policy for Audio To Text

**Effective date:** 2026-08-26

Audio To Text ("the App") is an Android utility that transcribes audio files shared via the "Share" menu. This policy describes how the App handles data when you use it.

This privacy policy is provided to support the Google Play **Data safety** form and the Play Console privacy policy requirements. By using the App you agree to the practices described below.

---

## 1. Overview

**Audio To Text is a fully local, on-device transcription utility.** It is designed so that your audio files and the resulting transcriptions are processed entirely on your device and never leave it. The App does not run a backend, does not collect personal data, and does not transmit your recordings or transcriptions anywhere.

---

## 2. Data handling (on-device)

- **Audio files** you share with the App are decoded and transcribed locally on your device using on-device speech recognition models.
- **Transcriptions** are generated on-device and displayed in the App. From there you can copy them to the clipboard.
- Audio and transcriptions are **never uploaded** to any server, and are **not** stored after processing unless you explicitly save/copy them. The App does not retain audio content.

---

## 3. Network usage

The only network activity performed by the App is **downloading speech recognition models** from the **sherpa-onnx** release archives (hosted on GitHub), triggered explicitly by you from the model catalog inside the App.

- Model downloads are initiated only by your explicit action.
- Downloading a model transmits the model file (and the network address used to fetch it) to the archive host. This is network traffic initiated by you, not a transfer of your audio or transcription.
- Downloaded model files are stored in the App's **internal storage** and are never transmitted to a remote service.

---

## 4. Data collected

**Audio To Text collects no personal or sensitive data.**

| Data type | Collected? | Notes |
|-----------|------------|-------|
| Personal data (name, email, phone) | No | No account required |
| Location | No | Never accessed |
| Recordings (audio) | No (external) | Audio is processed locally and not uploaded |
| Transcripts | No (external) | Kept on-device only |
| Usage statistics / analytics | No | No analytics SDKs |
| Advertising identifiers / Ad ID | No | No ads, no AdMob/Ad-ID |
| Device identifiers | No | Not collected |
| Diagnostics / crash reports | No | No crash/analytics SDK |

The App does **not** use cookies, advertising identifiers, or any tracking mechanism.

---

## 5. API keys and third parties

**API keys / Authorization**

- The App does **not** use any API keys, and does not request authorization to any account or online service.
- No third-party SDKs are embedded for analytics, advertising, or external data collection.

**Third party services**

- The only external service contacted is the sherpa-onnx **GitHub** release archive, used solely to download models. It is used only at your explicit request.

---

## 6. Storage, retention, and deletion

- Model files are stored in the App's **internal storage**. They can be removed by you at any time from the model catalog or by uninstalling the App.
- Audio and transcripts are not persisted by the App. Any data you copy to the system clipboard is handled by the clipboard and your own preferences.
- Uninstalling the App removes its internal data (model files and settings).

The App does not collect personal data, so there is no personal data to retain or delete on our side.

---

## 7. User rights and control

You are in control of your data:

- The App works fully offline; no account or sign-up is required.
- Models downloaded by the App can be deleted individually from the model catalog.
- Unless you have copies of the app, uninstalling deletes all local data.

---

## 8. Compliance

- **Play App Signing**: the App is distributed via Google Play and signed using Play App Signing; this relates only to the integrity, not to data collection.
- **Family Policy (targetSdk 35)**: the App does not collect personal data, and contains no ads, no social features, and no in-app purchases.

---

## Contact

If you have questions about this privacy policy or the App, contact:

**Audio To Text developer**
GitHub: https://github.com/b0sh-net/phone-whisper

---

## License

This project is licensed under the **GNU General Public License v3.0**.

*Changes to this policy will be reflected by updating the "Effective date" above.*