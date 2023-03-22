module com.example.stringsearcher {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.stringsearcher to javafx.fxml;
    exports com.example.stringsearcher;
}