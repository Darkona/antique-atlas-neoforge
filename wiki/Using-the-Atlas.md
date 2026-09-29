# Using the Atlas

## Opening it

- Press **M**. To change the key, go to Controls → Antique Atlas → Open Atlas.
- Or right-click while you hold an atlas book (see below).

The map shows only the areas you explored. Chunks appear when you see them. Structures appear after you stand in them or look at them.

## Moving around

- **Drag** with the mouse to move the map.
- **Scroll** to zoom in and out.
- The arrow button (bottom right) moves the map back to you.
- The icons at the bottom left change between the dimensions you explored.

## The bookmarks

On the right edge of the book:

| Bookmark | What it does |
|---|---|
| **Add marker** | Click it, then click on the map. Choose an icon, a colour and a label, then click **Done**. |
| **Delete marker** | Click it, then click one of your markers to remove it. |
| **Hide / show markers** | Hides all markers on the map, or shows them again. |
| **Scale** | Shows how many chunks one tile covers. Click it to reset the zoom. |
| **Export map as image** | Saves the whole explored area of the dimension on screen as a PNG image. |

The left edge has the list of your markers. Click a marker in the list to go to it.

**Middle-click** a marker to copy its coordinates (`x y z`) to the clipboard.

### Exporting the map

The export bookmark saves the whole explored area of the dimension on screen, not only the visible part. The file goes to `screenshots/atlas/<dimension>-<date>.png` in your game folder. Click the link in the chat to open it.

- The image uses 16 pixels per tile and starts from your current zoom. If the area does not fit in 4096 x 4096 pixels, the export zooms out (2, 4, 8... chunks per tile) until it fits. The chat message shows the scale of the image.
- The image includes markers, unless you hid them with **Hide markers**. It does not include player icons.
- Unexplored areas are blank page.

![Adding a marker](images/atlas-marker-editor.png)

## The atlas in your hands

An atlas is a **book named `Antique Atlas`**, or the translation of that name in the language of your game. Rename a book at an anvil, or take one from the Tools & Utilities creative tab.

- Hold the atlas to see the map around you, as with a vanilla map. When your other hand is empty, you hold it with both hands.
- Right-click it to open the full atlas.
- **Sneak and right-click** to zoom the handheld map out: 1, 2 or 4 chunks per tile.

![Holding the atlas](images/atlas-in-hand.png)

With `requireItem = true` in the [configuration](Configuration), the **M** key works only while you carry an atlas.
