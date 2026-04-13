# SAND (Saldo Andalucía) 🚌💳

**SAND** is an Android application designed to interact with public transport cards from the **Consorcio de Transporte Metropolitano de Andalucía** (the "green cards"). 

It allows users to check their current balance and, for educational and research purposes, modify it if the specific security keys are known.

---

## ✨ Features

- **Automatic Detection**: Identify card types (Normal, Joven) automatically by reading specific sectors.
- **Card Management**: Save multiple cards with custom names and colors for easy identification.
- **History Tracking**: Keep a local log of balance changes and readings.
- **Balance Modification**: Write a new balance to the card (requires Sector 9 Key B).
- **Modern UI**: Built with Jetpack Compose and Material 3, featuring a clean carrousel-based interface and dynamic themes.
- **Safety First**: Includes a mandatory legal disclaimer and educational focus.

---

## 🛠 Technical Details

The application targets **Mifare Classic 1K** tags, which are commonly used in transport systems.

### NFC Configuration
- **Sectors**: 8 and 9
- **Blocks**:
  - **34 (Sector 8)**: Contract data (used for type identification).
  - **36 (Sector 9)**: Backup of contract data.
  - **37 (Sector 9)**: Balance (Value Block).
- **Key A (Sector 8)**: `51B3EF60BF56` (Reading contract/type)
- **Key A (Sector 9)**: `99100225D83B` (Reading balance)
- **Key B (Sector 9)**: User-defined (Required for writing/modifying)

### Data Conversion
The balance is stored as an integer in a "Value Block" format.
- **Factor**: 1 unit = 0.005€ (or 1€ = 200 units).
- **Format**: Little Endian.

---

## 🚀 Getting Started

### Prerequisites
- Android device with **NFC support**.
- Android 8.0 (Oreo) or higher.

### Installation
1. Clone the repository.
2. Open the project in **Android Studio**.
3. Build and deploy to your device.

---

## 📖 How to Use

1. **Add a Card**: Tap the `+` icon in the carrousel or simply bring a card close to the back of your phone.
2. **Configure**: Long-press a card in the carrousel to set a custom name, color, or the **Key B** (if you have it).
3. **Check Balance**: Swipe through your saved cards to see the last known balance and history.
4. **Modify Balance**: If Key B is configured, tap the "Modify Balance" button, enter the new amount, and hold the card against the phone to write.

---

## ⚠️ Official Disclaimer

**LEGAL NOTICE AND TERMS OF USE**

- **Strictly Educational Purpose**: This application has been developed solely for academic, research, and security auditing purposes within the field of cybersecurity.
- **Prohibition of Malicious Use**: It is strictly forbidden to use this tool to alter, clone, or modify real public transport cards, evade payments, or perform any fraudulent or illegal activity.
- **Ownership and Permission**: The user agrees to use this application only on NFC cards that they fully own or for which they have explicit written permission from the owner to perform penetration testing.
- **Disclaimer of Liability**: The developer assumes no responsibility whatsoever for any misuse of this software by third parties, nor for any damages, card blocks, or legal or criminal consequences resulting from its use.
- **Acceptance of Risk**: By running and using this application, the user assumes full and absolute responsibility for their actions, confirming that they understand the risks and comply with the laws in force in their country.

---

## 👨‍💻 Authors

- **Marinetto** (Main Developer)
- **CrIsTiiAnPvP** (Contributor)

---
*Developed for educational purposes in the field of Cybersecurity.*
