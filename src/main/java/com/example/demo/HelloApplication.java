package com.example.demo;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class HelloApplication extends Application {

    @Override
    public void start(Stage peaLava) {
        Group juur = new Group(); // luuakse juur
        Rectangle ristkulik1 = new Rectangle(50, 50, 435, 435);
        juur.getChildren().add(ristkulik1);  // ristkülik lisatakse juure alluvaks
        Scene stseen1 = new Scene(juur, 535, 535, Color.SNOW);  // luuakse stseen
        peaLava.setTitle("Must ruut");  // lava tiitelribale pannakse tekst
        peaLava.setScene(stseen1);  // lavale lisatakse stseen
        peaLava.show();  // lava tehakse nähtavaks
    }

    public static void main(String[] args) {
        launch();
    }

}
