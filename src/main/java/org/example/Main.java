package org.example;

import org.example.heuristics.VNS;

public class Main {
    public static void main(String[] args) {
        DataParser currentDataTest = new DataParser("c100bD.dat");
        MapBuilder mapBuilder = new MapBuilder(currentDataTest);
        // mapBuilder.printMap();

        VNS vnsImplementation = new VNS(mapBuilder);
        vnsImplementation.runVNS();
    }
}