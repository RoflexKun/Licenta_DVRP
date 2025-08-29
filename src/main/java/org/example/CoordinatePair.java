package org.example;

public class CoordinatePair {
    private final Integer firstCoordinate;
    private final Integer secondCoordinate;

    CoordinatePair(Integer firstCoordinate, Integer secondCoordinate){
        this.firstCoordinate = firstCoordinate;
        this.secondCoordinate = secondCoordinate;
    }

    public Integer getFirstCoordinate() {
        return firstCoordinate;
    }

    public Integer getSecondCoordinate() {
        return secondCoordinate;
    }

    public Double distanceToNode(CoordinatePair secondPair){
        return Math.sqrt(Math.pow(this.firstCoordinate - secondPair.getFirstCoordinate(), 2) + Math.pow(this.secondCoordinate - secondPair.getSecondCoordinate(), 2));
    }
}
