package org.example;

import org.example.algorithms.GA.GeneticAlgorithm;
import org.example.algorithms.VNS.VNS;

public class Main {
    public static void main(String[] args) {
        DataParser currentDataTest = new DataParser("c100bD.dat");
        MapBuilder mapBuilder = new MapBuilder(currentDataTest);
        // mapBuilder.printMap();

        // Commented to test Genetic Algorithm
        //VNS vnsImplementation = new VNS(mapBuilder);
        //vnsImplementation.runVNS();

        GeneticAlgorithm geneticAlgorithmImplementation = new GeneticAlgorithm(mapBuilder);
        geneticAlgorithmImplementation.runGA();
    }
}