# 🔁 Repeat
> Automatically broadcast scheduled messages to your Minecraft server community.
**Repeat** is the only open-source plugin developed by **CobbleStudio**. It allows server administrators to automatically send recurring messages in chat, making it easier to communicate important information, announcements, rules, events, and reminders to players.

Whether you're managing a small community server or a large network, Repeat helps keep your players informed without requiring manual announcements.

## ✨ Features
- 📢 Automatic chat broadcasts
- ⏱️ Customizable message intervals
- 📝 Unlimited scheduled messages
- 🎨 Color code support
- ⚡ Lightweight and optimized
- 🔧 Easy configuration
- 🌍 Multi-server friendly
- 🆓 Fully open-source

## 🚀 Installation
1. Download the latest release.
2. Place the `.jar` file into your server's `plugins` folder.
3. Restart your server.
4. Configure your messages in the configuration file.
5. Enjoy automated announcements.

## ⚙️ Configuration Example

```yaml
interval: 300

messages:
  - "&6Join our Discord server!"
  - "&aDon't forget to vote for the server!"
  - "&bVisit our website for the latest news."
  - "&cRead the rules before playing."
```

### Configuration Options

| Option | Description |
|----------|----------|
| interval | Time between messages (in seconds) |
| messages | List of messages to broadcast |
| enabled | Enable or disable the plugin |

## 🛠️ Building From Source

### Clone the Repository

```bash
git clone https://github.com/CobbleStudio/Repeat.git
cd Repeat
```

### Build the Plugin

```bash
./gradlew build
```

Generated files can be found in:

```text
build/libs/
```

### Clean Build Files

```bash
./gradlew clean
```

### Run Tests

```bash
./gradlew test
```

### Refresh Dependencies

```bash
./gradlew --refresh-dependencies
```

## 📂 Project Structure

```text
.
├── src/
│   ├── main/
│   └── test/
├── gradle/
├── build.gradle
├── settings.gradle
├── gradlew
└── README.md
```

## 📖 Use Cases
Repeat can be used for:

- Server announcements
- Discord invitations
- Vote reminders
- Event notifications
- Store promotions
- Server rules reminders
- Community updates

## 🤝 Contributing
Contributions are welcome.
If you'd like to improve Repeat:

1. Fork the repository.
2. Create a feature branch.
3. Commit your changes.
4. Open a Pull Request.

⚠️ Direct pushes to the `main` branch are not allowed.

## 🐛 Reporting Issues
Found a bug?
Please open an issue and include:

- Minecraft version
- Server software (Paper, Spigot, Purpur, etc.)
- Plugin version
- Configuration file
- Error logs (if available)

## ❤️ Credits

### Developed By

**CobbleStudio**

### Creator

**M.pieuvre**

## 📜 License
This project is open source.
Please refer to the LICENSE file for more information about usage, modification, and redistribution.

---

<p align="center">
    <strong>🔁 Repeat</strong>
    <br>
    Simple. Lightweight. Automated.
    <br><br>
    Made with ❤️ by CobbleStudio
</p>
