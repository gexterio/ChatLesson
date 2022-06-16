module com.gexterio.webchat {
    requires javafx.controls;
    requires javafx.fxml;


    exports com.gexterio.webchat.client;
    opens com.gexterio.webchat.client to javafx.fxml;
}