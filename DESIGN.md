# DESIGN.md — UI/UX design direction

This file tracks visual/UX design references and decisions, separate from the
GIS data-accuracy rules in [GIS_DATA.md](./GIS_DATA.md). Reference images live
in [`design-refs/`](./design-refs/).

## Reference 1 — Live tracking screen (`design-refs/live-tracking-ui-reference.png`)

A fleet-tracking app screenshot (light + dark variant shown side by side).
**Scoped into Milestone 3 (GPS Tracking) now** — this is the closest existing
reference to "what should the screen look like while a route is being
recorded," which Milestone 3 needs regardless.

Elements worth carrying over into Milestone 3's recording screen:

- A **floating rounded card** anchored to one corner of the map (not a full
  top/bottom bar) showing live recording status — analogous to their "Live
  Status" card: a colored status dot, elapsed time ("Running from last 2hrs
  47mins" → for us, elapsed recording time), a small "updated Xs ago"
  freshness indicator, and a timestamp.
- A **progress/stat row** above or below that card — their version shows
  Distance Left / Speed / Live Temperature; ours would show **Distance
  recorded / Current pace or speed / Elapsed time**, matching what GPS
  Tracking (Milestone 3) actually has to compute, not distance-to-destination
  (we don't have routing yet — that's Milestone 8).
- A **thin progress bar** with a fraction label ("1 of 10 stops completed") —
  we don't have waypoints/checkpoints yet (Milestone 13), so this element is
  **not** pulled into Milestone 3; noted here for when checkpoints exist.
- The **route-so-far line drawn live on the map** (blue line following the
  blue direction-arrow puck) — this maps directly onto Milestone 3's "should
  the recorded track render on the map while recording" question, and the
  answer is now **yes**, styled as a solid, saturated line (visually distinct
  from the muted-gray-dashed *mock* trail line from Milestone 2, so a user
  never confuses "the trail I'm recording right now" with "unverified mock
  trail data").
- Small circular icon buttons bottom-right (map layers, re-center/locate) —
  consistent with the FAB pattern already used in Milestone 1/2, no change
  needed there.

Not carried over: vehicle/shipment-specific chrome (vehicle number, in-transit
badge, origin/destination cities, weather badge) — that's fleet-logistics
domain content, not relevant to a trail app.

## Reference 2 — "Peak" app (`design-refs/peak-app-ui-reference.png`)

A travel/mountain-exploration app concept: full-bleed photography, warm
cinematic color grading, glassmorphic cards over imagery, elegant serif/
display type for headlines, and a 3D topographic map view with a highlighted
route drawn over real terrain.

**Saved as north-star reference, not scoped into any milestone yet.** It
maps onto work that doesn't exist yet:

- The card-based "browse places" screen (hero photo + name + "View Place")
  → relevant once **trail/POI info screens** exist (Milestone 8 route
  engine / Milestone 13 POI data models) — but only once real photos and
  real place data exist. Applying this polish to *mock* data risks making
  fabricated placeholder content look deceptively real, which conflicts with
  this project's data-accuracy rule (see GIS_DATA.md) — so this waits for
  real data, not just for free dev time.
- The 3D terrain overview with a highlighted route drawn on real topography
  → directly the visual target for **Milestone 10 (3D Terrain)**, once a
  real DEM is wired in (Milestone 6). MapLibre Native supports 3D terrain
  rendering + `LineLayer` route overlays, so this is achievable with the
  current stack — just not yet.
- General visual language (typography, color, card corner radii, blur/glass
  effects) → informs an eventual app-wide theming pass, likely worth doing
  once Milestones 3–10 give the app enough real screens/content to be worth
  reskinning consistently, rather than restyling the current bare/functional
  MVP screens piecemeal.

## Sequencing decision (recorded 2026-09-21)

User confirmed: apply Reference 1's live-tracking UI pattern **now**, as part
of Milestone 3. Hold Reference 2 as a saved design reference for later
screens (trail/POI info, Milestone 8/13; 3D terrain, Milestone 10) rather
than doing an app-wide visual redesign before continuing feature work.
