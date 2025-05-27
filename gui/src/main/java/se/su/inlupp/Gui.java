package se.su.inlupp;

import javafx.application.Application;
//import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.control.*;
import javafx.scene.image.ImageView;
//import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class Gui extends Application {

  private Graph<String> graph = new ListGraph<>();

  @Override
  public void start(Stage stage) {
    stage.setTitle("Karta med graf");

    // File-meny
    MenuBar menuBar = new MenuBar();
    Menu fileMenu = new Menu("File");
    MenuItem newMapItem = new MenuItem("New Map");
    MenuItem openItem = new MenuItem("Open");
    MenuItem saveItem = new MenuItem("Save");
    MenuItem saveImageItem = new MenuItem("Save Image");
    MenuItem exitItem = new MenuItem("Exit");

    fileMenu.getItems().addAll(newMapItem, openItem, saveItem, saveImageItem, new SeparatorMenuItem(), exitItem);
    menuBar.getMenus().add(fileMenu);

    // Knapprad (tom än så länge)
    ToolBar toolBar = new ToolBar(
        new Button("New Place"),
        new Button("New Connection"),
        new Button("Show Connection"),
        new Button("Change Connection"),
        new Button("Find Path"));

    // Placeholder: för kartan
    ImageView mapView = new ImageView();
    Pane mapPane = new Pane(mapView);
    mapPane.setPrefSize(800, 600);

    // Layout
    BorderPane root = new BorderPane();
    root.setTop(new VBox(menuBar, toolBar));
    root.setCenter(mapPane);

    Scene scene = new Scene(root, 900, 700);
    stage.setScene(scene);
    stage.show();

    // Funktion för menyval "Exit"
    exitItem.setOnAction(e -> stage.close());

    // Funktion för menyval "New Map"
    newMapItem.setOnAction(e -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Välj en kartbild");
      fileChooser.getExtensionFilters().add(
          new FileChooser.ExtensionFilter("Bildfiler", "*.png", "*.jpg", "*.jpeg", "*.gif"));
      File file = fileChooser.showOpenDialog(stage);

      if (file != null) {
        // 1. Rensa gamla noder/graf
        graph = new ListGraph<>();

        // 2. Töm tidigare objekt från kartan, men behåll bakgrundsbilden
        mapPane.getChildren().clear();

        // 3. Lägg in ny bakgrundsbild
        ImageView newMapView = new ImageView(new javafx.scene.image.Image(file.toURI().toString()));
        newMapView.setPreserveRatio(true);
        newMapView.setFitWidth(800);
        newMapView.setFitHeight(600);
        mapPane.getChildren().add(newMapView);

        // 4. (Valfritt) Spara filvägen om du ska använda den i "Save"
        // currentMapPath = file.getAbsolutePath();

        System.out.println("Ny karta inläst: " + file.getName());
        System.out.println("Antal noder nu (bör vara 0): " + graph.getNodes().size());
      }

    });

  }

  public static void main(String[] args) {
    launch(args);
  }
}
