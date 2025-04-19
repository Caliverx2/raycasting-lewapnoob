RayCaster in Kotlin
Raycasting is a method employed in 2D games to simulate a three-dimensional effect by projecting rays from the player's position.
These rays help identify walls and objects in the game world, allowing the system to determine the distance to nearby obstacles.
Based on this information, the game renders vertical sections of the screen to create the illusion of depth and perspective.
This technique was notably utilized in iconic titles such as Wolfenstein 3D to deliver a first-person experience while keeping computational requirements minimal.

Algorithm
The Digital Differential Analyzer (DDA) is an algorithm commonly used in raycasting to efficiently determine where a ray intersects a grid-based environment.
Instead of examining every possible point along the ray’s trajectory, DDA progresses through the grid step by step, computing each intersection using straightforward arithmetic.
This approach enables precise and rapid wall detection, making it a popular choice for raycasting-driven games.

About the project
In this project, I exclusively used built-in Kotlin functions, such as Swing.
The project also includes all development files related to this work (*MainBackup.kt*) as well as test files for future projects (*cube3d.kt*).
