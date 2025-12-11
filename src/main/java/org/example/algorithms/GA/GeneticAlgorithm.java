package org.example.algorithms.GA;

import org.example.DataParser;
import org.example.MapBuilder;
import org.graph4j.Graph;

import java.util.*;

public class GeneticAlgorithm {
    private Graph graph;
    private DataParser valuesFromTestData;
    private Set<Integer> depotSet;
    private int timestep;
    private Map<Integer, Integer> visitOpeningTime;

    public GeneticAlgorithm(MapBuilder mapBuilder){
        this.graph = mapBuilder.getMapFromData();
        this.valuesFromTestData = mapBuilder.getDataParsedFromTestData();
        this.depotSet = new HashSet<>(this.valuesFromTestData.getMapDepotToNode().values());

        // Essentials for DVRP
        this.timestep = valuesFromTestData.getTimestep();
        this.visitOpeningTime = valuesFromTestData.getVisitOpeningTime();
    }

    private boolean isDepot(int node){
        return this.depotSet.contains(node);
    }

    private List<Integer> filterCustomers(int[] allNodes){
        List<Integer> onlyCustomers = new ArrayList<>();

        for(int node : allNodes){
            if(!isDepot(node)){
                onlyCustomers.add(node);
            }
        }

        return onlyCustomers;
    }

    private List<List<Integer>> initialSolution(){
        List<List<Integer>> initialPopulation = new ArrayList<>();

        int numberOfCustomers = this.valuesFromTestData.getNumberOfVisits();
        List<Integer> listOfCustomers = filterCustomers(this.graph.vertices());

        /*
         This value has been chosen as stated in "A mathematical model for location of temporary relief centers and dynamic routing of aerial rescue vehicles":
            4.6 Parameter adjustment
                "It is worth mentioning that the population size of genetic algorithm is set to 100 using some experiments"
         */
        for(int i = 0; i < 100; i++){
            List<Integer> currentIndividual = new ArrayList<>(listOfCustomers);
            Collections.shuffle(currentIndividual);
            initialPopulation.add(currentIndividual);
        }

        return initialPopulation;
    }

    public void runGA(){

    }
}
