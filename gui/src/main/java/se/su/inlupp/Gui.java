package se.su.inlupp;

import javafx.application.Application;
//import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
//import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;

public class Gui extends Application {

  private Graph<String> graph = new ListGraph<>();
  private ImageView mapView;
  private Pane mapPane;
  private Stage primaryStage;

  @Override
  public void start(Stage stage) {
    this.primaryStage = stage;
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

    // Knapprad
    ToolBar toolBar = new ToolBar(
        new Button("Find Path"),
        new Button("Show Connection"),
        new Button("New Place"),
        new Button("New Connection"),
        new Button("Change Connection"));

    // Kartområde
    mapView = new ImageView();
    mapPane = new Pane(mapView);
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
    newMapItem.setOnAction(e -> loadNewMap());
  }

  private void loadNewMap() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Välj en kartbild");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Bildfiler", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
    File file = fileChooser.showOpenDialog(primaryStage);

    if (file != null) {
      try {
        // 1. Rensa gamla noder/graf
        graph = new ListGraph<>();

        // 2. Ladda ny bild
        Image newImage = new Image(file.toURI().toString());

        // 3. Kontrollera att bilden laddades korrekt
        if (newImage.isError()) {
          showAlert("Fel", "Kunde inte ladda bilden. Kontrollera att filen är en giltig bildfil.");
          return;
        }

        // 4. Sätt bilden i sin naturliga storlek (ingen skalning)
        mapView.setImage(newImage);
        mapView.setPreserveRatio(true);

        // 5. Sätt mapPane att vara minst lika stor som bilden, men kan bli större
        mapPane.setMinSize(newImage.getWidth(), newImage.getHeight());
        mapPane.setPrefSize(newImage.getWidth(), newImage.getHeight());

        // 6. Rensa eventuella tidigare platser från kartan
        mapPane.getChildren().clear();
        mapPane.getChildren().add(mapView);

        // 7. Anpassa fönstrets storlek till bilden + meny/toolbar
        double menuHeight = 60; // Ungefärlig höjd för meny + toolbar
        double windowWidth = Math.max(newImage.getWidth(), 400); // Minsta bredd 400px
        double windowHeight = newImage.getHeight() + menuHeight;

        // 8. Sätt fönstrets storlek men låt det vara skalbart
        primaryStage.setWidth(windowWidth);
        primaryStage.setHeight(windowHeight);

        System.out.println("Ny karta inläst: " + file.getName());
        System.out.println("Bildstorlek: " + newImage.getWidth() + "x" + newImage.getHeight());
        System.out.println("Fönsterstorlek: " + windowWidth + "x" + windowHeight);
        System.out.println("Antal noder nu (bör vara 0): " + graph.getNodes().size());

      } catch (Exception ex) {
        showAlert("Fel", "Ett fel uppstod när bilden skulle laddas: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
