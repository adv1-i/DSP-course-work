module com.example.courseworkgui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires jfreechart;
    requires javafx.swing;
    requires org.bytedeco.javacpp;
    requires org.bytedeco.fftw;


    opens com.example.courseworkgui to javafx.fxml;
    exports com.example.courseworkgui;
}