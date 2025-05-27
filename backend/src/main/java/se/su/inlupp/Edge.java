package se.su.inlupp;

public class Edge<N> {
  private final N destination;
  private final String name;
  private int weight;

  public Edge(N destination, String name, int weight) {
    if (weight < 0) {
      throw new IllegalArgumentException("Vikten får inte vara negativ");
    }
    this.destination = destination;
    this.name = name;
    this.weight = weight;
  }

  public N getDestination() {
    return destination;
  }

  public String getName() {
    return name;
  }

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    if (weight < 0) {
      throw new IllegalArgumentException("Vikten får inte vara negativ");
    }
    this.weight = weight;
  }

  @Override
  public String toString() {
    return "till " + destination + " med " + name + " tar " + weight;
  }
}
