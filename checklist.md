# FLIP Fluid Simulation — Implementation Checklist

## Phase 1: Finish the Grid
- [x] `cellTypesToAir()` — Reset all non-SOLID cells to AIR
- [x] `setCellToFluid(int row, int col)` — Paint a single cell as FLUID
- [x] `updateSnapshot()` — Copy `cellType` to `snapshotType` for thread-safe rendering
- [x] `getSnapshotType()` — Return the snapshot array for EDT access
- [x] `getCellType(int index)` — Return the current cell type (for physics thread)

## Phase 2: Split `Particles.step()` into Stages
- [x] `integrate(float dt)` — Apply gravity and move particles
- [x] `markGridCells(Grid grid)` — Clear grid, then paint active particles as FLUID
- [x] `publish()` — Create the snapshot for rendering
- [x] Refactor `step(float dt, Grid grid, int width, int height)` — Orchestrate the three stages
- [x] Update `Display.java` to pass the `Grid` into `Particles.step()`

## Phase 3: Particles to Grid (P2G)
- [x] Add `u[]` and `v[]` arrays to `Grid` (velocity fields)
- [x] Add `uWeight[]` and `vWeight[]` arrays for accumulation
- [ ] Implement bilinear interpolation for velocity face influence
- [ ] **P2G step**: Accumulate `particle.vx * weight` into `u` faces
- [ ] **P2G step**: Accumulate `particle.vy * weight` into `v` faces
- [ ] **P2G step**: Normalize `u` and `v` by dividing by weights (if weight > 0)
- [ ] Handle edge cases (particles near grid boundaries)
- [ ] Reset `uWeight` and `vWeight` to zero every frame before accumulation

## Phase 4: The Pressure Solve
- [ ] Add `pressure[]` array to `Grid`
- [ ] Implement **divergence** calculation per fluid cell
- [ ] Implement **Jacobi iteration** with over-relaxation (~1.8–1.9)
- [ ] Implement **velocity correction** (subtract pressure gradient from `u` and `v`)
- [ ] Add configurable iteration count (default ~50)
- [ ] Add early exit if divergence drops below a threshold

## Phase 5: Grid to Particles (G2P) — PIC
- [ ] Implement **velocity interpolation** from grid to particle positions
- [ ] Set `particle.vx = interpolated_u` and `particle.vy = interpolated_v`
- [ ] Implement **ghost handling** near SOLID cells (zero or reflect velocity)
- [ ] Test visual output — expect "thick syrup" / honey-like behavior

## Phase 6: FLIP Correction
- [ ] Store the **FLIP residual** (`particle_velocity - interpolated_grid_velocity`)
- [ ] Add residual back to particle velocity instead of full replacement
- [ ] Implement **PIC/FLIP blend** factor (0.1–0.3 for stability)
- [ ] Test visual output — expect "swoosh" and less diffusion

## Phase 7: Wall Handling (Grid-Based)
- [ ] Remove hard-coded particle wall checks from `Particles`
- [ ] Implement grid-based wall collision using SOLID cells
- [ ] Reflect velocity components at SOLID boundaries
- [ ] Test with various wall configurations

## Phase 8: Polish & Optimization
- [ ] Tune over-relaxation factor for stability
- [ ] Optimize P2G/G2P interpolation (consider spatial hashing)
- [ ] Add particle count slider or config
- [ ] Profile performance at 25k particles
- [ ] Add visual debug mode (show velocity vectors, pressure, etc.)