module com.example.javarice_capstone.javarice_capstone {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.javarice_capstone.javarice_capstone to javafx.fxml;
    exports com.example.javarice_capstone.javarice_capstone;

    opens com.example.javarice_capstone.javarice_capstone.ui.playground to javafx.fxml;
    exports com.example.javarice_capstone.javarice_capstone.ui.playground;

    exports com.example.javarice_capstone.javarice_capstone.datatypes;
    opens com.example.javarice_capstone.javarice_capstone.datatypes to javafx.fxml;

    exports com.example.javarice_capstone.javarice_capstone.Gameplay;
    opens com.example.javarice_capstone.javarice_capstone.Gameplay to javafx.fxml;

    opens com.example.javarice_capstone.javarice_capstone.Factory to javafx.fxml;
}