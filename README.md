# MS Extras - Pathfinding API

A high-performance A* pathfinding API for Minescript.

---

## Setup

```python
from minescript import echo, JavaClass

PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

pathfinder = PathfinderAPI.getProvider().getPathfinder()
```

---

## Finding a Path

All paths are built with a chain starting from `pathfinder.path(goal)` and fired with `.execute().get()`.

```python
goal = GoalBlock(100, 64, 200)

# Ground movement, smoothed (recommended)
result = pathfinder.path(goal).smoothed().execute().get()

# Ground movement, raw A* nodes
result = pathfinder.path(goal).execute().get()

# 3D movement (flying / swimming)
result = pathfinder.path(goal).in3D().smoothed().execute().get()

# From a custom start position
result = pathfinder.path(goal).from(50, 64, 100).smoothed().execute().get()

# With a custom node limit
result = pathfinder.path(goal).smoothed().maxNodes(50000).execute().get()

# All options combined
result = pathfinder.path(goal).from(50, 64, 100).in3D().smoothed().maxNodes(50000).execute().get()
```

---

## Reading the Result

```python
if result.getSuccess():
    echo(f"Found {result.getPathSize()} waypoints in {result.getTimeMs()}ms")

    for i in range(result.getPathSize()):
        pos = result.getPathPoint(i)
        echo(f"  {i}: ({pos.getX()}, {pos.getY()}, {pos.getZ()})")
else:
    echo("No path found")
```

---

## Control

```python
pathfinder.isPathing()  # True if a search is in progress
pathfinder.cancel()     # Abort the current search
```

---

## API Reference

### Builder methods

Start with `pathfinder.path(goal)`, chain any modifiers, end with `.execute()`.

| Method | Description |
|--------|-------------|
| `.from(x, y, z)` | Start from a custom position instead of the player's position |
| `.in3D()` | Use 3D movement (flying / swimming) instead of ground movement |
| `.smoothed()` | Apply path smoothing to reduce unnecessary waypoints |
| `.maxNodes(n)` | Override the node limit (default: 100,000) |
| `.execute()` | Run the search — returns `CompletableFuture<PathResult>` |

### PathResult methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getSuccess()` | `Boolean` | Whether a path was found |
| `getPath()` | `List<BlockPos>` | Full waypoint list |
| `getPathSize()` | `Int` | Number of waypoints |
| `getPathPoint(i)` | `BlockPos` | Waypoint at index `i` |
| `getNodesEvaluated()` | `Int` | A* nodes expanded |
| `getTimeMs()` | `Long` | Computation time in milliseconds |

### Defaults

| Parameter | Default |
|-----------|---------|
| Max nodes | 100,000 |
| Timeout | 10,000 ms |

---

## Examples

### Visualize a path with glass blocks

```python
from minescript import echo, execute, JavaClass

PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

pathfinder = PathfinderAPI.getProvider().getPathfinder()
result = pathfinder.path(GoalBlock(100, 64, 200)).smoothed().execute().get()

if result.getSuccess():
    for i in range(result.getPathSize()):
        pos = result.getPathPoint(i)
        execute(f"/setblock {pos.getX()} {pos.getY()} {pos.getZ()} minecraft:glass")
```

### Retry with a higher node limit

```python
goal = GoalBlock(1000, 64, 1000)
result = pathfinder.path(goal).smoothed().execute().get()

if not result.getSuccess():
    result = pathfinder.path(goal).smoothed().maxNodes(200000).execute().get()

echo(f"Success: {result.getSuccess()}")
```

### Chain waypoints for distant goals

```python
r1 = pathfinder.path(GoalBlock(500, 64, 500)).smoothed().execute().get()

if r1.getSuccess():
    last = r1.getPathPoint(r1.getPathSize() - 1)
    r2 = pathfinder.path(GoalBlock(1000, 64, 1000)).from(last.getX(), last.getY(), last.getZ()).smoothed().execute().get()
```

---

## Troubleshooting

**"No path found" for a reachable goal** — the node limit was hit before the search completed. Retry with `.maxNodes(200000)` or break the route into shorter segments.

**Pathfinding is slow** — use a lower `.maxNodes()` value, or call `cancel()` to abort.

**`NullPointerException`** — the player is not in a world, or chunks around the start/goal are not loaded.
