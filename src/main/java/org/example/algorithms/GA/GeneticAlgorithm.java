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
    private Map<Integer, Integer> demands;

    private final int POPULATION_COUNT = 100;
    private final int PENALITY_VALUE = (int)1e9;

    public GeneticAlgorithm(MapBuilder mapBuilder){
        this.graph = mapBuilder.getMapFromData();
        this.valuesFromTestData = mapBuilder.getDataParsedFromTestData();
        this.depotSet = new HashSet<>(this.valuesFromTestData.getMapDepotToNode().values());
        this.demands = valuesFromTestData.getListOfCustomerDemand();
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
        for(int i = 0; i < POPULATION_COUNT; i++){
            List<Integer> currentIndividual = new ArrayList<>(listOfCustomers);
            Collections.shuffle(currentIndividual);
            initialPopulation.add(currentIndividual);
        }

        return initialPopulation;
    }

    private double computeFitness(List<Integer> individual){
        List<Integer> listOfDepotsIndexes = this.valuesFromTestData.getListOfDepotsIndex();
        int numberOfVehicles = this.valuesFromTestData.getNumberOfVehicles();
        int vehicleNumber = 0;
        int numberOfDepots = listOfDepotsIndexes.size();
        int depotNode = listOfDepotsIndexes.get(vehicleNumber % numberOfDepots);

        double currentTime = 0.0;
        int currentDemand = 0;
        int customerBefore = depotNode;
        double totalDistance = 0.0;

        for(int i = 0; i < individual.size(); i++){
            int currentCustomer = individual.get(i);
            int demand = this.demands.get(currentCustomer);
            double distance = this.graph.getEdgeWeight(customerBefore, currentCustomer);
            boolean routeEnds = false;

            if(currentTime < this.visitOpeningTime.get(currentCustomer)){
                currentTime += this.timestep;
                i--;
                continue;
            }

            double arrivalTime = currentTime + (distance / this.timestep);
            double returnTime = arrivalTime + (this.graph.getEdgeWeight(currentCustomer, depotNode) / this.timestep);
            int closingTime= this.valuesFromTestData.getDepotOpenTimeFrame().get(depotNode)[1];

            if(currentDemand + demand <= this.valuesFromTestData.getCapacityLimit() && returnTime <= closingTime){
                currentDemand += demand;
                customerBefore = currentCustomer;
                totalDistance += distance;
                currentTime = arrivalTime;
            } else {
                if(customerBefore != depotNode)
                    totalDistance += this.graph.getEdgeWeight(customerBefore, depotNode);

                vehicleNumber++;
                if(vehicleNumber >= numberOfVehicles)
                    return totalDistance + PENALITY_VALUE;

                depotNode = listOfDepotsIndexes.get(vehicleNumber % numberOfDepots);
                customerBefore = depotNode;
                currentDemand = 0;
                currentTime = 0.0;
                i--;
            }
        }

        if(customerBefore != depotNode){
            totalDistance += this.graph.getEdgeWeight(customerBefore, depotNode);
        }

        return totalDistance;

    }

    private List<Integer> rouletteWheelSelection(List<List<Integer>> currentPopulation){
        double[] scores = new double[currentPopulation.size()];
        double totalFitness = 0.0;

        for(int i = 0; i < currentPopulation.size(); i++){
            double currentScore = computeFitness(currentPopulation.get(i));
            double currentFitness = 1.0 / currentScore;
            scores[i] = currentFitness;
            totalFitness += currentFitness;
        }

        double rouletteSpinResult = (Math.random() * totalFitness);
        double partialSum = 0.0;
        List<Integer> selectedIndividual = new ArrayList<>();

        for(int i = 0; i < currentPopulation.size(); i++){
            partialSum += scores[i];

            if(partialSum >= rouletteSpinResult)
                return currentPopulation.get(i);
            selectedIndividual = currentPopulation.get(i);
        }

        return selectedIndividual;

    }

    private List<Integer> applyOrderCrossover(List<Integer> parent1, List<Integer> parent2){
        int size = parent1.size();

        int position1 = (int) (Math.random() * size);
        int position2 = (int) (Math.random() * size);

        int startPosition = Math.min(position1, position2);
        int endPosition = Math.max(position1, position2);

        List<Integer> parent1Values = new ArrayList<>();
        for(int i = startPosition; i <= endPosition; i++){
            parent1Values.add(parent1.get(i));
        }

        List<Integer> parent2Values = new ArrayList<>();
        for(int i = 0; i < size; i++){
            if(!parent1Values.contains(parent2.get(i))){
                parent2Values.add(parent2.get(i));
            }
        }

        List<Integer> child = new ArrayList<>();
        int currentIndexParent2 = 0;
        int currentIndexParent1 = 0;
        for(int i = 0; i < size; i++){
            if(i < startPosition || i > endPosition){
                child.add(parent2Values.get(currentIndexParent2));
                currentIndexParent2 += 1;
            } else {
                child.add(parent1Values.get(currentIndexParent1));
                currentIndexParent1 += 1;
            }
        }

        return child;
    }

    private List<List<Integer>> crossover(List<List<Integer>> currentPopulation){
        List<List<Integer>> nextGeneration = new ArrayList<>();

        List<Integer> bestIndividual = currentPopulation.getFirst();
        double bestFitness = Double.MAX_VALUE;

        for(List<Integer> individual : currentPopulation){
            double currentFitness = computeFitness(individual);
            if(currentFitness < bestFitness){
                bestIndividual = individual;
                bestFitness = currentFitness;
            }
        }

        // Keeping the best individual from each generation so we don't miss him
        nextGeneration.add(bestIndividual);

        while(nextGeneration.size() < POPULATION_COUNT){

            List<Integer> parent1 = rouletteWheelSelection(currentPopulation);
            List<Integer> parent2 = rouletteWheelSelection(currentPopulation);

            List<Integer> child = applyOrderCrossover(parent1, parent2);
            nextGeneration.add(child);

        }

        return nextGeneration;

    }

    public void runGA(){

    }
}
