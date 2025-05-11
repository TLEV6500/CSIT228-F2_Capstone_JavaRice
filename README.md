# UNO GAME Project

## TEAM NAME: JavaRice
- Alaman, Romar
- Jacalan, Aaron Rey A.
- Villacin, Tim Leobert E.

## Implementation Details

### Object-Oriented Programming Principles
- Employed classes and interfaces to encapsulate UNO-specific game rules (encapsulation and abstraction).
- Leveraged inheritance for different card types and polymorphism for handling various card actions.
- Implemented interfaces for certain interaction points (e.g., user input handling) and used abstract classes for shared behaviors.

### Java Generics
- Utilized generic collections (e.g., List<Card>, Map<Player, Score>) to store dynamic data and ensure type safety.
- Employed a custom generic helper class to demonstrate reusability in sorting or filtering game elements.

### Multithreading and Concurrency
- Introduced threading for background tasks such as animated card shuffling or timer-based events.
- Synchronized shared resources between multiple threads to prevent race conditions (e.g., game state updates).

### Graphical User Interface
- Built an interactive JavaFX interface with event listeners for drawing and playing cards.
- Ensured the GUI is user-friendly with intuitive layouts, clear instructions, and consistent visual design.

### Database Connectivity
- ??

### Unified Modeling Language (UML)
- Created and maintained Class and Use Case Diagrams aligning with the final code structure.
- Provided a README for the [UML](UML.md). Also located is the [Draw.io](UML/JavaRice_UML_Part1.drawio) file of the UML.

### Design Patterns
- Utilized the Factory pattern through distinct factories (e.g., PlayerFactory, StrategyFactory, GameSetupDialogFactory) to encapsulate object creation logic.
- Centralizing object creation simplifies maintenance and allows for easy extension or modification of supported players, strategies, and game setup options.
- Added somewhat of an Observer pattern since it notifies objects after the end of turn of a player.

## Additional Notes
[Home](docs/home.md)