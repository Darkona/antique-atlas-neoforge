# Playing Together

For a shared map, the **server needs Surveyor** — it does not need Antique Atlas. Each player who wants the map installs Antique Atlas and Surveyor.

By default:

- everything anyone explores appears on everyone's atlas;
- markers are shared, and you see other players on the map (faded when they're offline or in another dimension).

Which players you see is up to the server: with Surveyor's default settings everyone sees everyone, anywhere in the world, but spectators and invisible players are hidden. To only see your group, the server sets `positions = "GROUP"` and `globalSharing = false` — see [Surveyor: Who sees whom](https://github.com/Darkona/surveyor-neoforge/wiki/Playing-Together#who-sees-whom-on-the-map).

On a public server the owner can split players into groups instead; then `/surveyor share <player>` shares your map with a friend. All of this is set on the server, in Surveyor — see [Surveyor: Playing Together](https://github.com/Darkona/surveyor-neoforge/wiki/Playing-Together).

On a server **without** Surveyor the atlas still works, but only with what you explore yourself.
