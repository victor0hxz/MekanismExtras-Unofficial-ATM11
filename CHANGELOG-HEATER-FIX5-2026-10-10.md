## 1.4.2 fix5 — Corrupt thermal-state recovery

- Recover impossible saved temperatures (non-finite or at least 10^15 K) in Resistive Heaters, Thermal Evaporation Plants and thermodynamic conductor networks before processing or transferring heat.
- Restore conductor tier capacity in legacy saves where the broken codec wrote stored heat into the capacity field.
- Recovery clears only the corrupt stored heat to ambient temperature. Normal high-power heat, configurable FE/tick consumption, reactor temperatures, inventories and fluids are preserved.
- Added a Minecraft regression test for 1 FE/tick over 1,000 creative-heater ticks: 303.43 K, with no infinite heat generation.
- Verified recovery on an actual formed evaporation plant, an actual conductor network and a heater adjacent to another heater. Verified that a valid 10^10 K state is retained.

This build adds recovery for already-corrupt states. The original user's server data and server mod versions have not been inspected; it does not establish every possible source of heat corruption. Install on the server as well as the client for multiplayer. Back up the server world before updating.

The inherited full Java formatting check still has pre-existing violations; compilation, the remaining build checks and Minecraft functional verification passed.
