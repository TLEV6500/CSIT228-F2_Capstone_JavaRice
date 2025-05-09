module com.example.javarice_capstone.javarice_capstone {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;


    opens com.example.javarice_capstone.javarice_capstone to javafx.fxml;
    exports com.example.javarice_capstone.javarice_capstone;

    opens com.example.javarice_capstone.javarice_capstone.ui.playground to javafx.fxml;
    exports com.example.javarice_capstone.javarice_capstone.ui.playground;
    exports com.example.javarice_capstone.javarice_capstone.datatypes;
    opens com.example.javarice_capstone.javarice_capstone.datatypes to javafx.fxml;
}