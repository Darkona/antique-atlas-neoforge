# Playing Together

For a shared map, the **server needs Surveyor**. It does not need Antique Atlas. Each player who wants the map installs Antique Atlas and Surveyor.

By default:

- everything that a player explores appears on the atlas of every player;
- markers are shared, and you see the other players on the map (faded when they are offline or in a different dimension).

The server decides which players you see. With the default Surveyor settings, everyone sees everyone, anywhere in the world, but spectators and invisible players stay hidden. To see only your group, the server sets `positions = "GROUP"` and `globalSharing = false`. See [Surveyor: Who sees whom](https://github.com/Darkona/surveyor-neoforge/wiki/Playing-Together#who-sees-whom-on-the-map).

On a public server, the owner can put the players in groups instead. Then `/surveyor share <player>` shares your map with a friend. All of this is set on the server, in Surveyor. See [Surveyor: Playing Together](https://github.com/Darkona/surveyor-neoforge/wiki/Playing-Together).

On a server **without** Surveyor the atlas still works. It shows only what you explore yourself.
