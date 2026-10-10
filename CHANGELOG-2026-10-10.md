## 1.4.2 fix4

- Fixed heat capacitor serialization: heat and heat capacity are now saved separately.
- Restored the physical heat capacity of Resistive Heaters loaded from affected saves, preserving stored heat.
- Prevented creative upgrades from setting adjustable heater output to zero. The creative upgrade remains without a crafting recipe.
- Verified sided energy input, heating, adjacent heat transfer and persistence in an isolated server.
- Verified compatibility with Mekanism Version Locked 2.2 and a real Naquadah Reactor formation.

The full legacy Java formatting check still reports existing formatting violations; compilation and Minecraft functional checks passed.
