# UNO GAME Project

## TEAM NAME: JavaRice
- Alaman, Romar
- Jacalan, Aaron Rey A.
- Villacin, Tim Leobert E.

## Implementation Details

### Object-Oriented Programming Principles
- Employed classes and interfaces to encapsulate UNO-specific game rules (encapsulation and abstraction).
- Used Inheritance for different card types (CardNumber and CardAction) and polymorphism for overriding functions which have different usages.
- Implemented interfaces for certain interaction points (e.g., user input handling) and used abstract classes for shared behaviors.

### Java Generics
- ??

### Multithreading and Concurrency
- ??

### Graphical User Interface
- Built an interactive JavaFX interface for the whole game with event listeners for drawing and playing a card.
- Ensured the GUI is user-friendly with simple designs and easy to understand layout. Used some UNO colors in the design of the game.
![Image](https://github.com/user-attachments/assets/287f918a-a0bd-4074-b276-b67e9b98bb82)
  ![Image](https://github.com/user-attachments/assets/1b5a17eb-b42e-4d26-ae0b-5532e11e652f)

### Database Connectivity
- ??

### Unified Modeling Language (UML)
- Created and maintained Class and Use Case Diagrams aligning with the final code structure.
- Provided a README for the [UML](UML.md). Also located is the [Draw.io](UML/JavaRice_UML_Part1.drawio) file of the UML.

### Design Patterns
- Utilized the Factory pattern through distinct factories (e.g., PlayerFactory, StrategyFactory, GameSetupDialogFactory) to encapsulate object creation logic.
- Centralizing creation of Players and the different strategies (for the computer) they can implore simplifies maintenance and allows for easy extension or modification of supported players and strategies (Maybe adding a Trolling Strategy).
- Added somewhat of an Observer pattern since it notifies objects after the end of turn of a player (Not sure its applicable).

### Other Features
- ??

## Additional Notes
[Home](docs/home.md)