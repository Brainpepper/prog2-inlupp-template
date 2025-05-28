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
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.Optional;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class Gui extends Application {

  private Graph<String> graph = new ListGraph<>();
  private ImageView mapView;
  private Pane mapPane;
  private Stage primaryStage;
  private boolean hasUnsavedChanges = false;
  private String currentMapImagePath = null;

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

    // Event handlers
    exitItem.setOnAction(e -> handleExit());
    newMapItem.setOnAction(e -> handleNewMapItem());
    openItem.setOnAction(e -> handleOpenItem());
    saveItem.setOnAction(e -> handleSaveItem());
    saveImageItem.setOnAction(e -> handleSaveImageItem());

    // Hantera fönsterstängning
    stage.setOnCloseRequest(e -> {
      e.consume();
      handleExit();
    });
  }

  private void handleNewMapItem() {
    // Kontrollera osparade ändringar först
    if (!checkUnsavedChanges()) {
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Välj en kartbild");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Bildfiler", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
    File file = fileChooser.showOpenDialog(primaryStage);

    if (file != null) {
      try {
        // Rensa gamla noder/graf
        graph = new ListGraph<>();

        // Ladda ny bild
        Image newImage = new Image(file.toURI().toString());

        // Kontrollera att bilden laddades korrekt
        if (newImage.isError()) {
          showAlert("Fel", "Kunde inte ladda bilden. Kontrollera att filen är en giltig bildfil.");
          return;
        }

        // Sätt bilden
        mapView.setImage(newImage);
        mapView.setPreserveRatio(true);

        // Anpassa mapPane
        mapPane.setMinSize(newImage.getWidth(), newImage.getHeight());
        mapPane.setPrefSize(newImage.getWidth(), newImage.getHeight());

        // Rensa kartan och lägg till bilden
        mapPane.getChildren().clear();
        mapPane.getChildren().add(mapView);

        // Anpassa fönsterstorlek
        double menuHeight = 60;
        double windowWidth = Math.max(newImage.getWidth(), 400);
        double windowHeight = newImage.getHeight() + menuHeight;
        primaryStage.setWidth(windowWidth);
        primaryStage.setHeight(windowHeight);

        // Spara sökväg och markera som sparat
        currentMapImagePath = file.getAbsolutePath();
        hasUnsavedChanges = false;

        System.out.println("Ny karta inläst: " + file.getName());

      } catch (Exception ex) {
        showAlert("Fel", "Ett fel uppstod när bilden skulle laddas: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }

  private void handleOpenItem() {
    // Kontrollera osparade ändringar först
    if (!checkUnsavedChanges()) {
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Öppna graf-fil");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Graf-filer", "*.graph"));
    File file = fileChooser.showOpenDialog(primaryStage);

    if (file != null) {
      try {
        // Rensa gamla noder/graf
        graph = new ListGraph<>();

        try (java.util.Scanner scanner = new java.util.Scanner(file, "UTF-8")) {
          // Läs första raden - sökväg till bildfil
          if (!scanner.hasNextLine()) {
            throw new Exception("Tom fil eller fel format");
          }

          String imagePath = scanner.nextLine().trim();

          // Hantera både URI-format (file:namn.gif) och vanliga filnamn (namn.gif)
          String actualImagePath = imagePath;
          if (imagePath.startsWith("file:")) {
            actualImagePath = imagePath.substring(5); // Ta bort "file:" prefix
          }

          // Hitta bildfilen
          File imageFile = new File(actualImagePath);
          if (!imageFile.exists()) {
            // Prova relativ sökväg från graf-filens katalog
            imageFile = new File(file.getParent(), actualImagePath);
            if (!imageFile.exists()) {
              throw new Exception("Bildfilen hittades inte: " + actualImagePath);
            }
          }

          // Ladda bakgrundsbilden
          Image newImage = new Image(imageFile.toURI().toString());
          if (newImage.isError()) {
            throw new Exception("Kunde inte ladda bildfilen: " + imageFile.getName());
          }

          // Sätt bilden
          mapView.setImage(newImage);
          mapView.setPreserveRatio(true);

          // Anpassa mapPane
          mapPane.setMinSize(newImage.getWidth(), newImage.getHeight());
          mapPane.setPrefSize(newImage.getWidth(), newImage.getHeight());

          // Rensa kartan och lägg till bilden
          mapPane.getChildren().clear();
          mapPane.getChildren().add(mapView);

          // Anpassa fönsterstorlek
          double menuHeight = 60;
          double windowWidth = Math.max(newImage.getWidth(), 400);
          double windowHeight = newImage.getHeight() + menuHeight;
          primaryStage.setWidth(windowWidth);
          primaryStage.setHeight(windowHeight);

          currentMapImagePath = imageFile.getAbsolutePath();

          // Läs noder och edges
          while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty())
              continue;

            if (line.startsWith("Node:")) {
              // Format: Node: namn;x;y
              String nodeData = line.substring(5).trim();
              String[] parts = nodeData.split(";");
              if (parts.length != 3) {
                throw new Exception("Felaktigt nodformat: " + line);
              }

              String nodeName = parts[0];
              double x = Double.parseDouble(parts[1]);
              double y = Double.parseDouble(parts[2]);

              // Lägg till nod i grafen
              graph.add(nodeName);

              // TODO: Skapa visuell representation av noden
              // addVisualNode(nodeName, x, y);

            } else if (line.startsWith("Edge:")) {
              // Format: Edge: från;till;namn;vikt
              String edgeData = line.substring(5).trim();
              String[] parts = edgeData.split(";");
              if (parts.length != 4) {
                throw new Exception("Felaktigt edge-format: " + line);
              }

              String from = parts[0];
              String to = parts[1];
              String name = parts[2];
              int weight = Integer.parseInt(parts[3]);

              // Lägg till edge i grafen
              graph.connect(from, to, name, weight);

              // TODO: Skapa visuell representation av edge
              // addVisualEdge(from, to, name, weight);
            }
          }
        }

        hasUnsavedChanges = false;
        System.out.println("Graf-fil öppnad: " + file.getName());

      } catch (Exception ex) {
        showAlert("Fel", "Kunde inte öppna graf-filen: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }

  private void handleSaveItem() {
    // Kontrollera att det finns en karta att spara
    if (currentMapImagePath == null) {
      showAlert("Fel", "Ingen karta att spara. Ladda först en karta med 'New Map' eller 'Open'.");
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Graf-filer", "*.graph"));
    File file = fileChooser.showSaveDialog(primaryStage);

    if (file != null) {
      try {
        // Se till att filen har rätt ändelse
        String fileName = file.getAbsolutePath();
        if (!fileName.endsWith(".graph")) {
          file = new File(fileName + ".graph");
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
          // 1. Första raden: sökväg till bildfilen
          writer.println(currentMapImagePath);

          // 2. Andra raden: alla noder (semikolonseparerade)
          StringBuilder nodesLine = new StringBuilder();
          boolean firstNode = true;
          for (String nodeName : graph.getNodes()) {
            if (!firstNode) {
              nodesLine.append(";");
            }

            // TODO: Hämta faktiska koordinater från visuella noder
            // För nu används dummy-koordinater
            double x = 0.0; // Ska hämtas från PlaceNode
            double y = 0.0; // Ska hämtas från PlaceNode

            nodesLine.append(nodeName).append(";").append(x).append(";").append(y);
            firstNode = false;
          }
          writer.println(nodesLine.toString());

          // 3. Resterande rader: alla förbindelser (en per rad)
          for (String fromNode : graph.getNodes()) {
            for (Edge<String> edge : graph.getEdgesFrom(fromNode)) {
              String toNode = edge.getDestination();
              String edgeName = edge.getName();
              int weight = edge.getWeight();

              writer.println(fromNode + ";" + toNode + ";" + edgeName + ";" + weight);
            }
          }
        }

        hasUnsavedChanges = false;
        System.out.println("Graf sparad: " + file.getName());

      } catch (Exception ex) {
        showAlert("Fel", "Kunde inte spara graf-filen: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }

  private void handleSaveImageItem() {
    try {
      // Ta en snapshot av mapPane (kartområdet)
      WritableImage writableImage = mapPane.snapshot(new SnapshotParameters(), null);

      // Konvertera till BufferedImage
      BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);

      // Spara som PNG i projektmappen
      File outputFile = new File("capture.png");
      ImageIO.write(bufferedImage, "png", outputFile);

      System.out.println("Skärmbild sparad som: " + outputFile.getAbsolutePath());

    } catch (Exception ex) {
      showAlert("Fel", "Kunde inte spara skärmbilden: " + ex.getMessage());
      ex.printStackTrace();
    }
  }

  private void handleExit() {
    if (checkUnsavedChanges()) {
      primaryStage.close();
    }
  }

  private boolean checkUnsavedChanges() {
    if (hasUnsavedChanges) {
      Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      alert.setTitle("Warning!");
      alert.setHeaderText(null);
      alert.setContentText("Unsaved changes, continue anyway?");

      ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
      ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
      alert.getButtonTypes().setAll(okButton, cancelButton);

      Optional<ButtonType> result = alert.showAndWait();
      return result.isPresent() && result.get() == okButton;
    }
    return true;
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  // Metoder för att markera osparade ändringar
  public void markUnsavedChanges() {
    hasUnsavedChanges = true;
  }

  public void markSaved() {
    hasUnsavedChanges = false;
  }

  public static void main(String[] args) {
    launch(args);
  }
}