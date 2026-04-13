package org.example;

public class CoordinatePair {
    private final double firstCoordinate;
    private final double secondCoordinate;

    public CoordinatePair(double firstCoordinate, double secondCoordinate){
        this.firstCoordinate = firstCoordinate;
        this.secondCoordinate = secondCoordinate;
    }

    public double getFirstCoordinate() {
        return firstCoordinate;
    }

    public double getSecondCoordinate() {
        return secondCoordinate;
    }

    public double distanceToNode(CoordinatePair secondPair){
        return Math.sqrt(Math.pow(this.firstCoordinate - secondPair.getFirstCoordinate(), 2) + Math.pow(this.secondCoordinate - secondPair.getSecondCoordinate(), 2));
    }
}
