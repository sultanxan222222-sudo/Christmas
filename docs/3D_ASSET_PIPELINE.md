# Ramadan Dream Launcher — 3D asset preparation

This workflow is an isolated asset-acquisition stage for the Ramadan 3D scene.
It does not replace the current APK workflow and does not change the main branch.

Run **Actions → Prepare Ramadan 3D model assets → Run workflow** on
\`ramadan-islamic-launcher\`.

The script discovers the actual GLB download links from the source pages,
downloads the files, validates the binary glTF 2.0 headers and JSON chunks,
and publishes the models as a workflow artifact. It deliberately fails rather
than substituting a 2D image, a primitive mesh, or an unverified download.

All selected source pages identify the models as CC0. The final asset manifest
records the discovered download URLs and the model's mesh/material/texture
counts. The source models are candidates: they still need visual review,
composition work, material tuning, scale normalization, and integration into
the Android Filament renderer before they can be considered production-ready.

## Candidate models

- Mosque with central dome and two minarets — CC0.
- Brass hurricane lantern — CC0.
- Curled sleeping cat — CC0.
- Broadleaf canopy tree — CC0; chosen for its broad silhouette, not as an
  assertion that it is the exact Jordanian tree from the visual concept.

No existing launcher screens or prayer features are changed by this stage.
