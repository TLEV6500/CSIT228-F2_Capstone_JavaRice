module com.example.javarice_capstone.javarice_capstone {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.javarice_capstone.javarice_capstone to javafx.fxml;
    exports com.example.javarice_capstone.javarice_capstone;
}