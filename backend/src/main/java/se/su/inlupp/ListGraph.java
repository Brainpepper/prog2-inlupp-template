package se.su.inlupp;

import java.util.*;

public class ListGraph<T> implements Graph<T> {

  private final Map<T, List<Edge<T>>> adjacencyList = new HashMap<>();

  @Override
  public void add(T node) {
    adjacencyList.putIfAbsent(node, new ArrayList<>());
  }

  @Override
  public void remove(T node) {
    if (!adjacencyList.containsKey(node)) {
      throw new NoSuchElementException("Noden finns inte i grafen.");
    }
    adjacencyList.remove(node);

    for (List<Edge<T>> edges : adjacencyList.values()) {
      edges.removeIf(edge -> edge.getDestination().equals(node));
    }
  }

  @Override
  public void connect(T node1, T node2, String name, int weight) {
    if (!adjacencyList.containsKey(node1) || !adjacencyList.containsKey(node2)) {
      throw new NoSuchElementException("En eller båda noder saknas i grafen.");
    }

    if (weight < 0) {
      throw new IllegalArgumentException("Vikten får inte vara negativ.");
    }

    // Kontrollera om det redan finns en kant mellan noderna
    for (Edge<T> edge : adjacencyList.get(node1)) {
      if (edge.getDestination().equals(node2)) {
        throw new IllegalStateException("En förbindelse mellan noderna finns redan.");
      }
    }

    // Lägg till kanten åt båda håll (oriktad graf)
    adjacencyList.get(node1).add(new Edge<>(node2, name, weight));
    adjacencyList.get(node2).add(new Edge<>(node1, name, weight));
  }

  @Override
  public void disconnect(T node1, T node2) {
    if (!adjacencyList.containsKey(node1) || !adjacencyList.containsKey(node2)) {
      throw new NoSuchElementException("En eller båda noder saknas i grafen.");
    }

    Edge<T> edge1 = getEdgeBetween(node1, node2);
    Edge<T> edge2 = getEdgeBetween(node2, node1);

    if (edge1 == null || edge2 == null) {
      throw new IllegalStateException("Det finns ingen förbindelse mellan dessa noder.");
    }

    adjacencyList.get(node1).remove(edge1);
    adjacencyList.get(node2).remove(edge2);
  }

  @Override
  public void setConnectionWeight(T node1, T node2, int weight) {
    if (weight < 0) {
      throw new IllegalArgumentException("Vikten får inte vara negativ.");
    }

    Edge<T> edge1 = getEdgeBetween(node1, node2);
    Edge<T> edge2 = getEdgeBetween(node2, node1);

    if (edge1 == null || edge2 == null) {
      throw new NoSuchElementException("Det finns ingen förbindelse mellan dessa noder.");
    }

    edge1.setWeight(weight);
    edge2.setWeight(weight);
  }

  @Override
  public Set<T> getNodes() {
    return new HashSet<>(adjacencyList.keySet()); // Returnerar en kopia av noderna
  }

  @Override
  public Collection<Edge<T>> getEdgesFrom(T node) {
    if (!adjacencyList.containsKey(node)) {
      throw new NoSuchElementException("Noden finns inte i grafen.");
    }
    return new ArrayList<>(adjacencyList.get(node));
  }

  @Override
  public Edge<T> getEdgeBetween(T node1, T node2) {
    if (!adjacencyList.containsKey(node1) || !adjacencyList.containsKey(node2)) {
      throw new NoSuchElementException("En eller båda noder saknas i grafen.");
    }

    for (Edge<T> edge : adjacencyList.get(node1)) {
      if (edge.getDestination().equals(node2)) {
        return edge;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    for (T node : adjacencyList.keySet()) {
      sb.append(node).append(":\n");
      for (Edge<T> edge : adjacencyList.get(node)) {
        sb.append("  ").append(edge.toString()).append("\n");
      }
    }

    return sb.toString();
  }

  @Override
  public boolean pathExists(T from, T to) {
    if (!adjacencyList.containsKey(from) || !adjacencyList.containsKey(to)) {
      return false;
    }

    Set<T> visited = new HashSet<>();
    return dfs(from, to, visited);
  }

  // Djupetförst sökning (DFS) hjälpmetod
  private boolean dfs(T current, T target, Set<T> visited) {
    if (current.equals(target)) {
      return true;
    }

    visited.add(current);

    for (Edge<T> edge : adjacencyList.get(current)) {
      T neighbor = edge.getDestination();
      if (!visited.contains(neighbor)) {
        if (dfs(neighbor, target, visited)) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public List<Edge<T>> getPath(T from, T to) {
    if (!adjacencyList.containsKey(from) || !adjacencyList.containsKey(to)) {
      return null;
    }

    Set<T> visited = new HashSet<>();
    List<Edge<T>> path = new ArrayList<>();
    if (dfsBuildPath(from, to, visited, path)) {
      return path;
    } else {
      return null;
    }
  }

  // Djupetförst sökning (DFS) hjälpmetod
  private boolean dfsBuildPath(T current, T target, Set<T> visited, List<Edge<T>> path) {
    if (current.equals(target)) {
      return true;
    }

    visited.add(current);

    for (Edge<T> edge : adjacencyList.get(current)) {
      T neighbor = edge.getDestination();
      if (!visited.contains(neighbor)) {
        path.add(edge); // lägg till kanten i vägen
        if (dfsBuildPath(neighbor, target, visited, path)) {
          return true;
        }
        path.remove(path.size() - 1); // backtracka
      }
    }

    return false;
  }

}
