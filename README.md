# 🐾 Pets Plugin

A comprehensive, lightweight Minecraft pet system built for modern Spigot/Paper servers (1.20+). This plugin introduces cosmetic pets that actively assist players by mining blocks, gaining XP, and leveling up.

## ✨ Features

### ⚔️ Mining System
Pets aren't just for show!
- **Active Mining:** Right-click a block to send your pets to attack it.
- **Block Health:** Different blocks have different durability (e.g., Obsidian takes longer to break than Dirt).
- **Animations:** Pets physically move to the block and animate while attacking.
- **Damage Calculation:** Damage is calculated based on the pet's Strength, Level, and Variant multiplier.

### 📈 Leveling & Holograms
- **XP System:** Pets gain XP every time they damage a block.
- **Holograms:** Every active pet has a floating hologram above its head showing:
  - Current Level
  - Live XP Progress bar (e.g., `50/100`)
- **Scaling:** As pets level up, their damage output increases.

### 🧬 Variants & Rarity
Pets come in different rarities and special visual variants:
- **Rarities:** Common, Uncommon, Rare, Epic, Supreme.
- **Variants:**
  - **Normal:** Standard damage (1.0x).
  - **Gold:** ✨ Gold particles & 1.5x Damage.
  - **Diamond:** 💎 Magic particles & 2.5x Damage.
  - **Rainbow:** 🌈 Rod particles & 5.0x Damage.

### 🥚 Mystery Egg (Gacha) System
- **Egg Stations:** Admins can place physical "Egg Stations" in the world.
- **Hatching:** Players with a Mystery Egg can right-click the station to trigger a 3D hatching animation.
- **RNG:** Pulls a random pet with randomized Rarity and Variant chances.

### 🛡️ Formation System
- **Semi-Circle Logic:** Pets follow the player in a smooth semi-circle formation.
- **Dynamic Rows:** If a player equips more than 5 pets, the formation automatically wraps to create a second row behind the first.

---

## 🛠️ Installation

1. Ensure your server is running **Java 21** and **Minecraft 1.20.4+** (Paper recommended).
2. Download the `Pets.jar`.
3. Place the jar file in your server's `plugins` folder.
4. Restart the server.

---

## 💻 Commands & Permissions

| Command | Permission | Description |
| :--- | :--- | :--- |
| `/pet give <head> [str] [variant]` | `pet.give` | Give yourself a specific pet. <br>Ex: `/pet give MHF_Slime 10 RAINBOW` |
| `/pet remove <number>` | *None* | Unequip a pet by its slot number (1, 2, 3...). |
| `/pet trait <on/off>` | *None* | Toggle the particle effects for your pets. |
| `/pet egg` | `pet.give` | Give yourself a Mystery Egg. |
| `/pet placeegg` | `pet.admin` | Place a physical Egg Hatching Station at your location. |
| `/pet reload` | `pet.admin` | Reload the configuration file. |

---

## ⚙️ Configuration (`config.yml`)

You can customize almost every aspect of the plugin in the `config.yml`.

```yaml
settings:
  max-pets: 4            # Max pets a player can equip
  pet-spacing: 0.8       # Distance between pets in formation
  bob-speed: 0.15        # Speed of the floating animation
  bob-height: 0.15       # Height of the floating animation
  egg-texture: "..."     # Base64 texture for the Mystery Egg item

rarity-strength:
  COMMON: 1
  UNCOMMON: 2
  RARE: 3
  EPIC: 5
  SUPREME: 10

messages:
  prefix: "&8[&6Pets&8] &r"
