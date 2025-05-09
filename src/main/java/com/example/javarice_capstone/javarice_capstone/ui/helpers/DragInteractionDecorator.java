package com.example.javarice_capstone.javarice_capstone.ui.helpers;

import javafx.scene.Node;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.util.Pair;

public class DragInteractionDecorator {
    public static class XYPos {
        public double x;
        public double y;
        public XYPos() {
            x = y = 0;
        }
        public XYPos(double x, double y) {
            this.x = x;
            this.y = y;
        }
        public XYPos(Pair<Double, Double> xy) {
            if (xy == null) {
                x = y = 0;
                return;
            }
            this.x = xy.getKey();
            this.y = xy.getValue();
        }
        public XYPos copy() {
            return new XYPos(x,y);
        }
        public void shiftBy(double x, double y) {
            this.x += x;
            this.y += y;
        }
    }

    public void makeDraggable(Pane container) {
        NodeRegistry.getInstance().register(container);
//        var existingOnDrag = container.getOnDragDetected();
//        System.out.println("Existing onDrag handler of" + container + ": "+ existingOnDrag);
        container.setOnDragDetected((evt)->{
//            if (existingOnDrag != null) existingOnDrag.handle(evt);
            Dragboard dragboard = container.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(container.getId());
            dragboard.setContent(clipboardContent);
            NodeUserDataManager userDataManager = new NodeUserDataManager();
            userDataManager.setUserDataFieldsFor(
                container,
                new Pair<>(OnDragUserDataKeys.ON_DRAG_MOUSE_XY.name(), new XYPos(evt.getX(), evt.getY())),
                new Pair<>(OnDragUserDataKeys.ON_DRAG_NODE_XY.name(), new XYPos(container.getLayoutX(), container.getLayoutX()))
            );
            container.setOpacity(0.5);
            evt.consume();
        });
        container.setOnDragDone(evt -> {
            if (evt.getTransferMode() == TransferMode.MOVE) {
                ((Pane) container.getParent()).getChildren().remove(container); // Remove from source
                container.setOpacity(1.0);
            }
            evt.consume();
        });
    }

    public void makeDroppable(Pane container) {
        NodeRegistry nr = NodeRegistry.getInstance();
        nr.register(container);
        container.setOnDragOver((evt)->{
            if (evt.getDragboard().hasString()) {
                evt.acceptTransferModes(TransferMode.MOVE);
            }
            evt.consume();
        });
        container.setOnDragDropped((evt)->{
            Dragboard dragboard = evt.getDragboard();
            boolean success = false;

            if (dragboard.hasString()) {
                String nodeId = dragboard.getString();

                Node draggedNode = nr.findNodeById(nodeId);
                if (draggedNode != null) {
                    container.getChildren().add(draggedNode);
                    success = true;
                }
            }

            evt.setDropCompleted(success);
            evt.consume();
        });
    }

    private void translateObject(Node obj, XYPos sourceXY) {
        obj.setLayoutX(sourceXY.x);
        obj.setLayoutY(sourceXY.y);
    }
}
