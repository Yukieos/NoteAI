## NoteAI

NoteAI is a lightweight Android note-taking app focused on fast tag-based organization and search. It offers a main notes view with tag filters and a sidebar list, plus a detail screen that supports colored tags, live tag creation, and bulk operations.

## Screenshots

Main screen (tags and notes):

<img width="333" height="744" alt="image" src="https://github.com/user-attachments/assets/789d4ebb-80a2-4e5d-a684-485e497249a6" />

Overview of the main workflow and features

- Tags at the top (for example: "1", "omg") act as filters. Long-pressing a tag permanently deletes that tag across the app (not just from a single note). A confirmation dialog is shown to prevent accidental deletion.
- The main screen includes a search button for quickly finding notes.
- A left sidebar shows all notes in a list view. You can open a note either from the sidebar or directly from the main screen to view its details.

When a tag is selected (for example, "omg"), the main list filters to show only notes tagged with that label.

<img width="326" height="744" alt="image" src="https://github.com/user-attachments/assets/157002fa-e909-45df-a547-a79d6f156fea" />

<img width="331" height="743" alt="image" src="https://github.com/user-attachments/assets/038075a0-0e21-496e-b415-00f14df3efd4" />

Search has been tested and works as expected.

## Detail screen

Detail view (note editor and tag manager):

<img width="333" height="746" alt="image" src="https://github.com/user-attachments/assets/71a6cf4b-0e34-45b7-8580-933a4a333e1d" />

- The detail screen contains a title, content body, and tags.
- Tag management is simple and streamlined: you can add tags, choose a tag color, and see tags that have already been created in real time for quick selection.

<img width="342" height="757" alt="image" src="https://github.com/user-attachments/assets/db0af25b-30f1-45f1-bad3-f81246d4d2ea" />

- The top-right corner of the detail page includes Save and Delete actions. Changes must be saved or they will be lost.

## Features

- Tag-based filtering and permanent tag deletion (with confirmation)
- Search across notes
- Sidebar list view for quick access
- Bulk delete: long-press a note to enter multi-select mode, then delete multiple notes with a confirmation dialog
- Real-time tag creation & color selection on the detail page

## Next steps / roadmap

- Improve content rendering in the note body (support for code blocks, rich text, and other inline rendering)
- Add synchronisation or backup options

## Notes

If you'd like the README phrasing adjusted, more detailed usage instructions, or a short "How to run" section for building the Android app, tell me which platform/gradle commands you'd prefer and I will add that.
