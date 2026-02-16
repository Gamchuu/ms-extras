# MS Extras - Pathfinding API - User Guide

## Overview

This mod provides a pathfinding API which is optimized for path generation.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Basic Usage](#basic-usage)
3. [Advanced Features](#advanced-features)
4. [API Reference](#api-reference)
5. [Examples](#examples)
6. [Performance Tips](#performance-tips)
7. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Minimal Example

```python
from minescript import echo

# Import the API
PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

# Get pathfinder instance
pathfinder = PathfinderAPI.getProvider().getPathfinder()

# Create a goal and find a smoothed path
goal = GoalBlock(100, 64, 200)
future = pathfinder.findSmoothedPathAsync(goal)

# Wait for result
result = future.get()

if result.getSuccess():
    echo(f"Path found with {result.getPathSize()} waypoints")
else:
    echo("No path found")
```

---

## Basic Usage

### 1. Setting Up the Pathfinder

```python
from minescript import echo, execute

# Import required classes
PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

# Get the singleton pathfinder instance
provider = PathfinderAPI.getProvider()
pathfinder = provider.getPathfinder()
```

### 2. Creating Goals

Goals define where you want to navigate. The most common goal is `GoalBlock`:

```python
# Navigate to specific coordinates
goal = GoalBlock(x, y, z)

# Example: Navigate to position (100, 64, 200)
goal = GoalBlock(100, 64, 200)
```

### 3. Finding a Path

#### Ground Mode (Walking)

Use this for normal walking movement:

```python
# Basic smoothed path from player's position
future = pathfinder.findSmoothedPathAsync(goal)
result = future.get()

# With custom node limit (for faster results)
future = pathfinder.findSmoothedPathAsync(goal, 50000)
result = future.get()

# From a custom start position
future = pathfinder.findSmoothedPathAsync(startX, startY, startZ, goal)
result = future.get()
```

### 4. Using the Result

```python
result = future.get()

if result.getSuccess():
    # Get basic information
    numWaypoints = result.getPathSize()
    nodesEvaluated = result.getNodesEvaluated()
    timeMs = result.getTimeMs()
    
    echo(f"Found path with {numWaypoints} waypoints")
    echo(f"Computed in {timeMs}ms ({nodesEvaluated} nodes)")
    
    # Access waypoints
    for i in range(result.getPathSize()):
        pos = result.getPathPoint(i)
        x, y, z = pos.getX(), pos.getY(), pos.getZ()
        echo(f"Waypoint {i}: ({x}, {y}, {z})")
else:
    echo("Pathfinding failed - no path exists")
```

---

## Advanced Features

### Checking Pathfinding Status

```python
# Check if pathfinding is currently running
if pathfinder.isPathing():
    echo("Pathfinding in progress...")
else:
    echo("No pathfinding running")
```

### Cancelling Pathfinding

```python
# Cancel the current pathfinding operation
if pathfinder.isPathing():
    pathfinder.cancel()
    echo("Pathfinding cancelled")
```

### Custom Node Limits

Control the search space to balance speed vs. completeness:

```python
# Fast but may not find distant paths (50k nodes)
future = pathfinder.findSmoothedPathAsync(goal, 50000)

# Default balance (100k nodes)
future = pathfinder.findSmoothedPathAsync(goal)

# Exhaustive search for complex paths (200k nodes)
future = pathfinder.findSmoothedPathAsync(goal, 200000)
```

### Regular vs. Smoothed Paths

Compare the difference:

```python
# Regular A* path (all waypoints)
regularFuture = pathfinder.findPathAsync(goal)
regularResult = regularFuture.get()

# Smoothed path (optimized waypoints)
smoothedFuture = pathfinder.findSmoothedPathAsync(goal)
smoothedResult = smoothedFuture.get()

echo(f"Regular: {regularResult.getPathSize()} waypoints")
echo(f"Smoothed: {smoothedResult.getPathSize()} waypoints")
echo(f"Reduction: {100 - (smoothedResult.getPathSize() * 100 / regularResult.getPathSize()):.1f}%")
```

---

## API Reference

### PathfinderAPI Methods

#### Ground Mode - From Player Position

```kotlin
// Basic pathfinding
findPathAsync(goal: Goal): CompletableFuture<PathResult>
findPathAsync(goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

// Smoothed pathfinding
findSmoothedPathAsync(goal: Goal): CompletableFuture<PathResult>
findSmoothedPathAsync(goal: Goal, maxNodes: Int): CompletableFuture<PathResult>
```

#### Ground Mode - From Custom Start

```kotlin
// Basic pathfinding
findPathAsync(startX, startY, startZ, goal): CompletableFuture<PathResult>
findPathAsync(startX, startY, startZ, goal, maxNodes): CompletableFuture<PathResult>

// Smoothed pathfinding
findSmoothedPathAsync(startX, startY, startZ, goal): CompletableFuture<PathResult>
findSmoothedPathAsync(startX, startY, startZ, goal, maxNodes): CompletableFuture<PathResult>
```

#### 3D Mode - From Player Position

```kotlin
// Basic 3D pathfinding
findPathAsync3D(goal: Goal): CompletableFuture<PathResult>
findPathAsync3D(goal: Goal, maxNodes: Int): CompletableFuture<PathResult>

// Smoothed 3D pathfinding
findSmoothedPathAsync3D(goal: Goal): CompletableFuture<PathResult>
findSmoothedPathAsync3D(goal: Goal, maxNodes: Int): CompletableFuture<PathResult>
```

#### 3D Mode - From Custom Start

```kotlin
// Basic 3D pathfinding
findPathAsync3D(startX, startY, startZ, goal): CompletableFuture<PathResult>
findPathAsync3D(startX, startY, startZ, goal, maxNodes): CompletableFuture<PathResult>

// Smoothed 3D pathfinding
findSmoothedPathAsync3D(startX, startY, startZ, goal): CompletableFuture<PathResult>
findSmoothedPathAsync3D(startX, startY, startZ, goal, maxNodes): CompletableFuture<PathResult>
```

#### Control Methods

```kotlin
isPathing(): Boolean  // Returns true if pathfinding is running
cancel()              // Cancels current pathfinding operation
```

### PathResult Methods

```kotlin
getSuccess(): Boolean              // True if path was found
getPath(): List<BlockPos>          // Complete path as list
getPathSize(): Int                 // Number of waypoints
getPathPoint(index: Int): BlockPos // Get specific waypoint
getNodesEvaluated(): Int          // A* nodes evaluated
getTimeMs(): Long                  // Computation time in milliseconds
```

### Default Values

- **Max nodes**: 100,000
- **Timeout**: 10,000 ms (10 seconds)
- **Max LOS skip**: 12 waypoints

---

## Examples

### Example 1: Navigate to Coordinates

```python
from minescript import echo, execute

PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

pathfinder = PathfinderAPI.getProvider().getPathfinder()

# Cancel any existing pathfinding
if pathfinder.isPathing():
    pathfinder.cancel()

# Find path to coordinates
goal = GoalBlock(100, 64, 200)
future = pathfinder.findSmoothedPathAsync(goal)
echo("Computing path...")

result = future.get()

if result.getSuccess():
    echo(f"✓ Path found with {result.getPathSize()} waypoints")
    echo(f"  Time: {result.getTimeMs()}ms")
    echo(f"  Nodes: {result.getNodesEvaluated()}")
    
    # Show waypoints
    for i in range(result.getPathSize()):
        pos = result.getPathPoint(i)
        echo(f"  {i}: ({pos.getX()}, {pos.getY()}, {pos.getZ()})")
else:
    echo("✗ No path found")
```

### Example 2: Visualize Path with Glass

```python
from minescript import echo, execute

PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

pathfinder = PathfinderAPI.getProvider().getPathfinder()
goal = GoalBlock(100, 64, 200)

# Find smoothed path
future = pathfinder.findSmoothedPathAsync(goal)
result = future.get()

if result.getSuccess():
    echo(f"Placing {result.getPathSize()} glass markers...")
    
    # Place glass at each waypoint
    for i in range(result.getPathSize()):
        pos = result.getPathPoint(i)
        execute(f"/setblock {pos.getX()} {pos.getY()} {pos.getZ()} minecraft:glass")
    
    echo("Path visualization complete!")
```

### Example 4: Path from Custom Start

```python
from minescript import echo

PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

pathfinder = PathfinderAPI.getProvider().getPathfinder()

# Find path from (50, 64, 100) to (200, 64, 300)
goal = GoalBlock(200, 64, 300)
future = pathfinder.findSmoothedPathAsync(50, 64, 100, goal)
result = future.get()

if result.getSuccess():
    start = result.getPathPoint(0)
    end = result.getPathPoint(result.getPathSize() - 1)
    echo(f"Path from ({start.getX()}, {start.getY()}, {start.getZ()})")
    echo(f"     to ({end.getX()}, {end.getY()}, {end.getZ()})")
    echo(f"     via {result.getPathSize()} waypoints")
```

### Example 5: Monitor Progress

```python
import time
from minescript import echo

PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

pathfinder = PathfinderAPI.getProvider().getPathfinder()
goal = GoalBlock(500, 64, 500)  # Distant goal

# Start pathfinding
future = pathfinder.findSmoothedPathAsync(goal)

# Monitor while running
echo("Pathfinding started...")
while pathfinder.isPathing():
    echo("  Still computing...")
    time.sleep(0.5)

# Get result
result = future.get()
echo(f"Finished: {result.getSuccess()}")
```

### Example 6: Retry with Higher Limits

```python
from minescript import echo

PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

pathfinder = PathfinderAPI.getProvider().getPathfinder()
goal = GoalBlock(1000, 64, 1000)

# Try with default limits first
echo("Trying with default limits...")
future = pathfinder.findSmoothedPathAsync(goal)
result = future.get()

if not result.getSuccess():
    # Retry with higher node limit
    echo("Retrying with higher limits...")
    future = pathfinder.findSmoothedPathAsync(goal, 200000)
    result = future.get()

if result.getSuccess():
    echo(f"Success! Path has {result.getPathSize()} waypoints")
else:
    echo("No path exists even with higher limits")
```

### Example 7: Compare Regular vs. Smoothed

```python
from minescript import echo

PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
GoalBlock = JavaClass("gamchu.pathfinder.goals.GoalBlock")

pathfinder = PathfinderAPI.getProvider().getPathfinder()
goal = GoalBlock(100, 64, 200)

# Get regular path
echo("Computing regular path...")
regularFuture = pathfinder.findPathAsync(goal)
regularResult = regularFuture.get()

# Get smoothed path
echo("Computing smoothed path...")
smoothedFuture = pathfinder.findSmoothedPathAsync(goal)
smoothedResult = smoothedFuture.get()

if regularResult.getSuccess() and smoothedResult.getSuccess():
    regularSize = regularResult.getPathSize()
    smoothedSize = smoothedResult.getPathSize()
    reduction = 100 - (smoothedSize * 100 / regularSize)
    
    echo(f"Regular path:  {regularSize} waypoints")
    echo(f"Smoothed path: {smoothedSize} waypoints")
    echo(f"Reduction:     {reduction:.1f}%")
    
    echo("\nFirst 5 waypoints (regular):")
    for i in range(min(5, regularSize)):
        pos = regularResult.getPathPoint(i)
        echo(f"  {i}: ({pos.getX()}, {pos.getY()}, {pos.getZ()})")
    
    echo("\nFirst 5 waypoints (smoothed):")
    for i in range(min(5, smoothedSize)):
        pos = smoothedResult.getPathPoint(i)
        echo(f"  {i}: ({pos.getX()}, {pos.getY()}, {pos.getZ()})")
```

---


### 3. Cancel Unnecessary Operations

```python
# Before starting new pathfinding, cancel old one
if pathfinder.isPathing():
    pathfinder.cancel()

future = pathfinder.findSmoothedPathAsync(goal)
```

### 5. Batch Multiple Requests

If you need paths to multiple locations, compute them sequentially:

```python
goals = [GoalBlock(100, 64, 100), GoalBlock(200, 64, 200), GoalBlock(300, 64, 300)]
results = []

for i, goal in enumerate(goals):
    echo(f"Computing path {i+1}/{len(goals)}...")
    future = pathfinder.findSmoothedPathAsync(goal)
    result = future.get()
    results.append(result)

echo(f"Computed {len(results)} paths")
```

---

## Troubleshooting

### Problem: "No path found" but goal is reachable

**Causes:**
- Node limit too low for the distance
- Timeout reached before finding path
- Goal position is not walkable (e.g., in a wall)

**Solutions:**
```python
# 1. Increase node limit
future = pathfinder.findSmoothedPathAsync(goal, 200000)

# 2. Check if goal position is valid
# Make sure y-coordinate is on solid ground

# 3. Try from a different start position
future = pathfinder.findSmoothedPathAsync(startX, startY, startZ, goal)
```

### Problem: Pathfinding takes too long

**Causes:**
- Goal is very far away
- Complex terrain with many obstacles
- Node limit is too high

**Solutions:**
```python
# 1. Lower the node limit for faster results
future = pathfinder.findSmoothedPathAsync(goal, 50000)

# 2. Use waypoints for very distant goals
waypoint = GoalBlock(halfway_x, halfway_y, halfway_z)
future1 = pathfinder.findSmoothedPathAsync(waypoint)
# Then pathfind from waypoint to final goal

# 3. Cancel if taking too long
import time
future = pathfinder.findSmoothedPathAsync(goal)
time.sleep(5)  # Wait 5 seconds
if pathfinder.isPathing():
    pathfinder.cancel()
    echo("Pathfinding cancelled - taking too long")
```

### Problem: Path goes through unexpected routes

**Causes:**
- A* found the mathematically shortest path
- Terrain makes direct path impossible

**Solutions:**
```python
# This is expected behavior - A* finds the shortest valid path
# If you want to avoid certain areas, you'll need to:

# 1. Check the path and replan if needed
result = future.get()
if result.getSuccess():
    # Check if path goes through unwanted area
    for i in range(result.getPathSize()):
        pos = result.getPathPoint(i)
        if is_dangerous_area(pos.getX(), pos.getZ()):
            echo("Path goes through dangerous area!")
            # Find alternative route or add waypoints
```

### Problem: Path has weird angles or movements

**Causes:**
- You're using regular paths instead of smoothed paths

**Solution:**
```python
# ✗ Don't use regular paths
# future = pathfinder.findPathAsync(goal)

# ✓ Use smoothed paths
future = pathfinder.findSmoothedPathAsync(goal)
```

### Problem: Script crashes with "NullPointerException"

**Causes:**
- Player is not in a world
- Pathfinder not initialized properly

**Solutions:**
```python
from minescript import echo

try:
    PathfinderAPI = JavaClass("gamchu.pathfinder.api.PathfinderAPI")
    pathfinder = PathfinderAPI.getProvider().getPathfinder()
    
    if pathfinder is None:
        echo("Error: Pathfinder not initialized")
    else:
        # Your pathfinding code here
        pass
except Exception as e:
    echo(f"Error: {e}")
```

---

## Credits

Based on A* pathfinding with optimizations inspired by Baritone.
AI was used to genereate parts of this documentation.
