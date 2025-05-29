package se.su.inlupp;

import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.File;
import java.util.Optional;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Node;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

class PlaceNode extends StackPane {
  private final String name;
  private final Circle circle;
  private final Label label;
  private boolean selected = false;

  public PlaceNode(String name, double x, double y) {
    this.name = name;
    this.circle = new Circle(8, Color.LIGHTBLUE);
    this.circle.setStroke(Color.BLACK);
    this.label = new Label(name);
    getChildren().addAll(circle, label);
    setLayoutX(x - circle.getRadius());
    setLayoutY(y - circle.getRadius());
    setOnMouseClicked(this::handleClick);
  }

  private void handleClick(MouseEvent event) {
    toggleSelected();
    event.consume();
  }

  public void toggleSelected() {
    selected = !selected;
    circle.setFill(selected ? Color.RED : Color.LIGHTBLUE);
    Gui.onPlaceNodeSelected(this);
  }

  public boolean isSelected() {
    return selected;
  }

  public String getName() {
    return name;
  }

  public double getCenterX() {
    return getLayoutX() + circle.getRadius();
  }

  public double getCenterY() {
    return getLayoutY() + circle.getRadius();
  }

}

public class Gui extends Application {
  private Graph<String> graph = new ListGraph<>();
  private ImageView mapView;
  private Pane mapPane;
  private Stage primaryStage;
  private boolean hasUnsavedChanges = false;
  private String currentMapImagePath = null;
  private static List<PlaceNode> selectedNodes = new ArrayList<>();
  private Button newPlaceBtn; // gör knappen tillgänglig för andra metoder i Gui

  @Override
  public void start(Stage stage) {
    this.primaryStage = stage;
    stage.setTitle("Karta med graf");

    // fil meny
    MenuBar menuBar = new MenuBar();
    Menu fileMenu = new Menu("File");
    MenuItem newMapItem = new MenuItem("New Map");
    MenuItem openItem = new MenuItem("Open");
    MenuItem saveItem = new MenuItem("Save");
    MenuItem saveImageItem = new MenuItem("Save Image");
    MenuItem exitItem = new MenuItem("Exit");

    fileMenu.getItems().addAll(newMapItem, openItem, saveItem, saveImageItem, new SeparatorMenuItem(), exitItem);
    menuBar.getMenus().add(fileMenu);

    // toolbar
    Button findPathBtn = new Button("Find Path");
    Button showConnBtn = new Button("Show Connection");
    newPlaceBtn = new Button("New Place");
    Button newConnBtn = new Button("New Connection");
    Button changeConnBtn = new Button("Change Connection");
    ToolBar toolBar = new ToolBar(findPathBtn, showConnBtn, newPlaceBtn, newConnBtn, changeConnBtn);

    // kartområde
    mapView = new ImageView();
    mapPane = new Pane(mapView);
    mapPane.setPrefSize(800, 600);

    // layout
    BorderPane root = new BorderPane();
    root.setTop(new VBox(menuBar, toolBar));
    root.setCenter(mapPane);

    Scene scene = new Scene(root, 900, 700);
    stage.setScene(scene);
    stage.show();

    // fil meny handlers
    exitItem.setOnAction(e -> handleExit());
    newMapItem.setOnAction(e -> handleNewMapItem());
    openItem.setOnAction(e -> handleOpenItem());
    saveItem.setOnAction(e -> handleSaveItem());
    saveImageItem.setOnAction(e -> handleSaveImageItem());

    // toolbar handlers
    newPlaceBtn.setOnAction(e -> activateNewPlaceMode());
    newConnBtn.setOnAction(e -> handleNewConnection());
    showConnBtn.setOnAction(e -> handleShowConnection());
    changeConnBtn.setOnAction(e -> handleChangeConnection());
    findPathBtn.setOnAction(e -> handleFindPath());

    // hantera exit
    stage.setOnCloseRequest(e -> {
      e.consume();
      handleExit();
    });
  }

  // new place
  private void activateNewPlaceMode() {
    mapPane.setCursor(Cursor.CROSSHAIR);

    // inaktivera knappen
    newPlaceBtn.setDisable(true);

    mapPane.setOnMouseClicked(evt -> {
      double x = evt.getX();
      double y = evt.getY();
      TextInputDialog dialog = new TextInputDialog();
      dialog.setTitle("New Place");
      dialog.setHeaderText(null);
      dialog.setContentText("Name of place:");
      Optional<String> result = dialog.showAndWait();
      result.ifPresent(name -> {
        if (name.trim().isEmpty()) {
          showAlert("Error", "Name cannot be empty.");
          return;
        }
        addVisualNode(name.trim(), x, y);
        markUnsavedChanges();
      });

      // återställ muspekare
      mapPane.setCursor(Cursor.DEFAULT);
      mapPane.setOnMouseClicked(null);

      // aktivera knappen igen
      newPlaceBtn.setDisable(false);
    });
  }

  private void addVisualNode(String name, double x, double y) {
    PlaceNode node = new PlaceNode(name, x, y);
    mapPane.getChildren().add(node);
    graph.add(name);
  }

  public static void onPlaceNodeSelected(PlaceNode node) {
    if (node.isSelected()) {
      selectedNodes.add(node);
      if (selectedNodes.size() > 2) {
        PlaceNode last = selectedNodes.removeLast();
        last.toggleSelected();
      }
    } else {
      selectedNodes.remove(node);
    }
  }

  // new connection
  private void handleNewConnection() {
    if (selectedNodes.size() != 2) {
      showAlert("Error!", "Two places must be selected to create a new connection.");
      return;
    }

    PlaceNode a = selectedNodes.get(0), b = selectedNodes.get(1);
    if (graph.getEdgeBetween(a.getName(), b.getName()) != null) {
      showAlert("Error!", "A connection already exists between these places.");
      return;
    }

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("New Connection");
    dialog.setHeaderText("Connection from " + a.getName() + " to " + b.getName());

    GridPane grid = new GridPane();
    TextField nameField = new TextField(), weightField = new TextField();

    grid.add(new Label("Name:"), 0, 0);
    grid.add(nameField, 1, 0);
    grid.add(new Label("Time:"), 0, 1);
    grid.add(weightField, 1, 1);

    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    dialog.getDialogPane().setContent(grid);
    ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
    dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

    dialog.showAndWait().ifPresent(action -> {
      if (action == okButtonType) {
        try {
          String name = nameField.getText().trim();
          String timeText = weightField.getText().trim();

          if (name.isEmpty()) {
            showAlert("Error", "Name cannot be empty.");
            return;
          } else if (timeText.isEmpty()) {
            showAlert("Error", "Time cannot be empty.");
            return;
          }

          int time = Integer.parseInt(timeText);
          graph.connect(a.getName(), b.getName(), name, time);
          drawEdge(a, b);
          showAlert("Success", "Connection created successfully.");
          markUnsavedChanges();
        } catch (NumberFormatException ex) {
          showAlert("Error", "Invalid time value.");
        }
      }
    });
  }

  private void drawEdge(PlaceNode a, PlaceNode b) {
    Line line = new Line(a.getCenterX(), a.getCenterY(), b.getCenterX(), b.getCenterY());
    line.setStrokeWidth(2);
    mapPane.getChildren().add(line);
  }

  // Show connection
  private void handleShowConnection() {
    if (selectedNodes.size() != 2) {
      showAlert("Error", "Two places must be selected to show a connection.");
      return;
    }

    PlaceNode a = selectedNodes.get(0), b = selectedNodes.get(1);
    Edge<String> edge = graph.getEdgeBetween(a.getName(), b.getName());

    if (edge == null) {
      showAlert("Error", "No connection exists between " + a.getName() + " and " + b.getName());
    } else {
      Dialog<String> dialog = new Dialog<>();
      dialog.setTitle("Connection");
      dialog.setHeaderText("Connection from " + a.getName() + " to " + b.getName());

      GridPane grid = new GridPane();

      grid.setHgap(10);
      grid.setVgap(10);
      grid.setPadding(new Insets(20, 150, 10, 10));

      Label nameLabel = new Label("Name:");
      TextField nameField = new TextField(edge.getName());
      nameField.setEditable(false);

      Label timeLabel = new Label("Time:");
      TextField timeField = new TextField(String.valueOf(edge.getWeight()));
      timeField.setEditable(false);

      grid.add(nameLabel, 0, 0);
      grid.add(nameField, 1, 0);
      grid.add(timeLabel, 0, 1);
      grid.add(timeField, 1, 1);

      dialog.getDialogPane().setContent(grid);

      ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
      ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
      dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

      dialog.showAndWait();
    }

  }

  // change connection
  private void handleChangeConnection() {
    if (selectedNodes.size() != 2) {
      showAlert("Error!", "Two places must be selected to change a connection.");
      return;
    }

    PlaceNode a = selectedNodes.get(0), b = selectedNodes.get(1);
    Edge<String> edge = graph.getEdgeBetween(a.getName(), b.getName());

    if (edge == null) {
      showAlert("Error!", "No connection exists between " + a.getName() + " and " + b.getName());
      return;
    } else {
      Dialog<ButtonType> dialog = new Dialog<>();
      dialog.setTitle("Change Connection");
      dialog.setHeaderText("Connection from " + a.getName() + " to " + b.getName());
      GridPane grid = new GridPane();
      grid.setHgap(10);
      grid.setVgap(10);
      grid.setPadding(new Insets(20, 150, 10, 10));

      Label nameLabel = new Label("Name:");
      TextField nameField = new TextField(edge.getName());
      nameField.setEditable(false);

      Label timeLabel = new Label("Time:");
      TextField timeField = new TextField(String.valueOf(edge.getWeight()));
      timeField.setEditable(true);

      grid.add(nameLabel, 0, 0);
      grid.add(nameField, 1, 0);
      grid.add(timeLabel, 0, 1);
      grid.add(timeField, 1, 1);

      dialog.getDialogPane().setContent(grid);

      ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
      ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
      dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

      dialog.showAndWait().ifPresent(action -> {
        if (action == okButtonType) {
          try {
            int newWeight = Integer.parseInt(timeField.getText());
            graph.setConnectionWeight(a.getName(), b.getName(), newWeight);
            showAlert("Success", "Connection time updated to " + newWeight);
            markUnsavedChanges();
          } catch (NumberFormatException ex) {
            showAlert("Error", "Invalid time value.");
          }
        }
      });
    }
  }

  // Find path
  private void handleFindPath() {
    if (selectedNodes.size() != 2) {
      showAlert("Error!", "Two places must be selected to find a path.");
      return;
    }
    PlaceNode a = selectedNodes.get(0), b = selectedNodes.get(1);
    if (!graph.pathExists(a.getName(), b.getName())) {
      showAlert("Error!", "No path exists between " + a.getName() + " and " + b.getName());
      return;
    }
    List<Edge<String>> path = graph.getPath(a.getName(), b.getName());

    Dialog<ButtonType> dialog = new Dialog<>();
    dialog.setTitle("Message");
    dialog.setHeaderText("The Path from " + a.getName() + " to " + b.getName() + ":");

    TextArea pathArea = new TextArea();
    pathArea.setEditable(false);
    pathArea.setPrefRowCount(8);
    pathArea.setPrefColumnCount(50);

    StringBuilder sb = new StringBuilder();
    int total = 0;
    for (Edge<String> e : path) {
      sb.append("to ").append(e.getDestination()).append(" by ")
          .append(e.getName()).append(" takes ").append(e.getWeight()).append("\n");
      total += e.getWeight();
    }
    sb.append("Total ").append(total);

    pathArea.setText(sb.toString());

    dialog.getDialogPane().setContent(pathArea);

    ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().add(okButtonType);

    dialog.showAndWait();
  }

  private void handleNewMapItem() {
    // Kontrollera osparade ändringar först
    if (!checkUnsavedChanges()) {
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Choose Map Image");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Image formats", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
    File file = fileChooser.showOpenDialog(primaryStage);

    if (file != null) {
      try {
        // rensa gamla noder
        graph = new ListGraph<>();

        Image newImage = new Image(file.toURI().toString());

        if (newImage.isError()) {
          showAlert("Error!", "Could not load image. Please check the file format and try again.");
          return;
        }

        // sätt ny bild i mapView
        mapView.setImage(newImage);
        mapView.setPreserveRatio(true);

        // justera mapPane efter bilden
        mapPane.setMinSize(newImage.getWidth(), newImage.getHeight());
        mapPane.setPrefSize(newImage.getWidth(), newImage.getHeight());

        // cleara karta och lägg till ny bild från mapView
        mapPane.getChildren().clear();
        mapPane.getChildren().add(mapView);

        // anpassa fönsterstorlek efter bilden
        double menuHeight = 60;
        double windowWidth = Math.max(newImage.getWidth(), 400);
        double windowHeight = newImage.getHeight() + menuHeight;
        primaryStage.setWidth(windowWidth);
        primaryStage.setHeight(windowHeight);

        // spara sökväg och markera som sparat
        currentMapImagePath = file.getAbsolutePath();
        hasUnsavedChanges = false;

      } catch (Exception ex) {
        showAlert("Error!", "An error occurred while loading the image: " + ex.getMessage());
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
    fileChooser.setTitle("Open Graph File");
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Graph-files", "*.graph"));
    File file = fileChooser.showOpenDialog(primaryStage);

    if (file != null) {
      try {
        // Rensa gamla noder och graf
        graph = new ListGraph<>();
        selectedNodes.clear();

        try (java.util.Scanner scanner = new java.util.Scanner(file, "UTF-8")) {
          // läs första raden som är sökvägen till bakgrundsbilden
          if (!scanner.hasNextLine()) {
            throw new Exception("No image path in file");
          }

          String imagePath = scanner.nextLine().trim();

          // hantera "file:" prefix om det finns
          String actualImagePath = imagePath;
          if (imagePath.startsWith("file:")) {
            actualImagePath = imagePath.substring(5);
          }

          // kontrollera sökväg
          File imageFile = new File(actualImagePath);
          if (!imageFile.exists()) {
            // Try relative path from graph file's directory
            imageFile = new File(file.getParent(), actualImagePath);
            if (!imageFile.exists()) {
              throw new Exception("Image file not found: " + actualImagePath);
            }
          }

          // ladda in bilden
          Image newImage = new Image(imageFile.toURI().toString());
          if (newImage.isError()) {
            throw new Exception("Failed to load image file: " + imageFile.getName());
          }

          // justera mapView och mapPane efter den uppladdade bilden
          mapView.setImage(newImage);
          mapView.setPreserveRatio(true);
          mapPane.setMinSize(newImage.getWidth(), newImage.getHeight());
          mapPane.setPrefSize(newImage.getWidth(), newImage.getHeight());

          // cleara gamla mapPane och lägg till ny bild
          mapPane.getChildren().clear();
          mapPane.getChildren().add(mapView);

          // // anpassa fönsterstorlek efter bilden
          double menuHeight = 60;
          double windowWidth = Math.max(newImage.getWidth(), 400);
          double windowHeight = newImage.getHeight() + menuHeight;
          primaryStage.setWidth(windowWidth);
          primaryStage.setHeight(windowHeight);

          currentMapImagePath = imageFile.getAbsolutePath();

          // läs andra raden från filen som innehåller noder
          if (!scanner.hasNextLine()) {
            throw new Exception("No nodes in file");
          }

          String nodesLine = scanner.nextLine().trim();
          if (!nodesLine.isEmpty()) {
            String[] nodeData = nodesLine.split(";");

            // proccessar noder i tre (nodeName, x, y)
            for (int i = 0; i < nodeData.length; i += 3) {
              if (i + 2 < nodeData.length) {
                String nodeName = nodeData[i];
                double x = Double.parseDouble(nodeData[i + 1]);
                double y = Double.parseDouble(nodeData[i + 2]);

                graph.add(nodeName);

                // rendera visuell nod
                addVisualNode(nodeName, x, y);
              }
            }
          }

          // hantera edges
          while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty())
              continue;

            String[] parts = line.split(";");
            if (parts.length != 4) {
              throw new Exception("Wrong Edge-format: " + line);
            }

            String from = parts[0];
            String to = parts[1];
            String name = parts[2];
            int weight = Integer.parseInt(parts[3]);

            // lägg till kanten i grafen
            if (graph.getEdgeBetween(from, to) == null) {
              graph.connect(from, to, name, weight);

              // Create visual representation
              PlaceNode fromNode = findPlaceNodeByName(from);
              PlaceNode toNode = findPlaceNodeByName(to);
              if (fromNode != null && toNode != null) {
                drawEdge(fromNode, toNode);
              }
            }
          }
        }

        hasUnsavedChanges = false;
        System.out.println("Graph-file opened: " + file.getName());

      } catch (Exception ex) {
        showAlert("Error!", "Failed to load graph file: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }

  // hjälpmetod för att hitta en PlaceNode baserat på dess namn
  private PlaceNode findPlaceNodeByName(String name) {
    for (Node child : mapPane.getChildren()) {
      if (child instanceof PlaceNode) {
        PlaceNode placeNode = (PlaceNode) child;
        if (placeNode.getName().equals(name)) {
          return placeNode;
        }
      }
    }
    return null;
  }

  private void handleSaveItem() {

    if (currentMapImagePath == null) {
      showAlert("Error!", "Cannot save - no map is currently loaded. Use 'New Map' or 'Open' to load a map first.");
      return;
    }

    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().add(
        new FileChooser.ExtensionFilter("Graph-files", "*.graph"));
    File file = fileChooser.showSaveDialog(primaryStage);

    if (file != null) {
      try {
        // kolla om filnamnet slutar med .graph, annars lägg till det
        String fileName = file.getAbsolutePath();
        if (!fileName.endsWith(".graph")) {
          file = new File(fileName + ".graph");
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8))) {
          // 1. första raden är sökvägen till bakgrundsbilden
          writer.println(currentMapImagePath);

          // 2. Andra raden innehåller alla noder i format (nodeName;x;y;nodeName;x;y;...)
          StringBuilder nodesLine = new StringBuilder();
          boolean firstNode = true;

          // hämta kordinater för varje PlaceNode i mapPane
          Map<String, PlaceNode> visualNodes = new HashMap<>();
          for (Node child : mapPane.getChildren()) {
            if (child instanceof PlaceNode) {
              PlaceNode placeNode = (PlaceNode) child;
              visualNodes.put(placeNode.getName(), placeNode);
            }
          }

          for (String nodeName : graph.getNodes()) {
            if (!firstNode) {
              nodesLine.append(";");
            }

            PlaceNode visualNode = visualNodes.get(nodeName);
            double x = visualNode != null ? visualNode.getCenterX() : 0.0;
            double y = visualNode != null ? visualNode.getCenterY() : 0.0;

            nodesLine.append(nodeName).append(";").append(x).append(";").append(y);
            firstNode = false;
          }
          writer.println(nodesLine.toString());

          // 3. sist, alla kanter i format (fromNode;toNode;edgeName;weight)
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
        System.out.println("Graph-file saved: " + file.getName());

      } catch (Exception ex) {
        showAlert("Error!", "Failed to load graph file: " + ex.getMessage());
        ex.printStackTrace();
      }
    }
  }

  private void handleSaveImageItem() {
    try {
      // Ta en skärmdhump av mapPane (kartan)
      WritableImage writableImage = mapPane.snapshot(new SnapshotParameters(), null);

      // konvertera till BufferedImage
      BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);

      // spara som PNG i topmappen (projektet)
      File outputFile = new File("capture.png");
      ImageIO.write(bufferedImage, "png", outputFile);

      System.out.println("Screenshot saved as: " + outputFile.getAbsolutePath());

    } catch (Exception ex) {
      showAlert("Error!", "Failed to save screenshot: " + ex.getMessage());
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