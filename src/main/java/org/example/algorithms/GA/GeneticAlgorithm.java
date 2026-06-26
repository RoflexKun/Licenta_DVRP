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
    private Map<Integer, Integer> lastVehiclePosition;
    private Map<Integer, Integer> currentVehicleTime;
    private Set<Integer> activeClients;
    private Map<Integer, Integer> currentVehicleCapacity;
    private List<List<Integer>> plannedRoutes;

    private final int POPULATION_COUNT = 100;
    private final int PENALITY_VALUE = (int)1e9;
    private final int MAX_EVALUATIONS = 1000000;
    private int fitnessUsageCounter = 0;

    private Map<List<Integer>, Double> fitnessCache = new HashMap<>();

    public GeneticAlgorithm(MapBuilder mapBuilder, Map<Integer, Integer> lastVehiclePosition, Map<Integer, Integer> currentVehicleTime, Set<Integer> activeClients, Map<Integer, Integer> currentVehicleCapacity, List<List<Integer>> plannedRoutes){
        this.graph = mapBuilder.getMapFromData();
        this.valuesFromTestData = mapBuilder.getDataParsedFromTestData();
        this.depotSet = new HashSet<>(this.valuesFromTestData.getMapDepotToNode().values());
        this.demands = valuesFromTestData.getListOfCustomerDemand();

        // Essentials for DVRP
        this.timestep = valuesFromTestData.getTimestep();
        this.visitOpeningTime = valuesFromTestData.getVisitOpeningTime();
        this.lastVehiclePosition = lastVehiclePosition;
        this.currentVehicleTime = currentVehicleTime;
        this.activeClients = activeClients;
        this.currentVehicleCapacity = currentVehicleCapacity;
        this.plannedRoutes = plannedRoutes;
    }

    private boolean isDepot(int node){
        return this.depotSet.contains(node);
    }

    private List<List<Integer>> initialSolution(){
        List<List<Integer>> initialPopulation = new ArrayList<>();

        int numberOfCustomers = this.valuesFromTestData.getNumberOfVisits();
        List<Integer> listOfCustomers = new ArrayList<>(this.activeClients);

        if(listOfCustomers.isEmpty()) {
            return initialPopulation;
        }

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
        this.fitnessUsageCounter++;

        if(this.fitnessCache.containsKey(individual)){
            return this.fitnessCache.get(individual);
        }

        List<Integer> listOfDepotsIndexes = this.valuesFromTestData.getListOfDepotsIndex();
        int numberOfVehicles = this.valuesFromTestData.getNumberOfVehicles();
        int numberOfDepots = this.valuesFromTestData.getNumberOfDepots();
        int vehicleNumber = 0;
        double totalDistance = 0.0;
        int unservedClients = 0;

        int depotNode = listOfDepotsIndexes.get(vehicleNumber % numberOfDepots);
        double currentTime = this.currentVehicleTime.get(vehicleNumber);
        int currentCapacity = this.currentVehicleCapacity.get(vehicleNumber);
        int currentNode = this.lastVehiclePosition.get(vehicleNumber);

        for(int i = 0; i < individual.size(); i++){
            int nextCustomer = individual.get(i);

            if (!this.activeClients.contains(nextCustomer)) {
                continue;
            }

            int demand = Math.abs(this.demands.get(nextCustomer));
            double distance = (currentNode == nextCustomer) ? 0.0 : this.graph.getEdgeWeight(currentNode, nextCustomer);

            double arrivalTime = currentTime + (distance / this.timestep);

            if (arrivalTime < this.visitOpeningTime.get(nextCustomer)) {
                double diff = this.visitOpeningTime.get(nextCustomer) - arrivalTime;
                arrivalTime += Math.ceil(diff / this.timestep) * this.timestep;
            }

            double returnTime = arrivalTime + (this.graph.getEdgeWeight(nextCustomer, depotNode) / this.timestep);
            int closingTime = this.valuesFromTestData.getDepotOpenTimeFrame().get(depotNode)[1];

            if(demand <= currentCapacity && returnTime <= closingTime){
                currentCapacity -= demand;
                totalDistance += distance;
                currentTime = arrivalTime;
                currentNode = nextCustomer;
            }
            else {
                double timeToReturnNow = currentTime + (this.graph.getEdgeWeight(currentNode, depotNode) / this.timestep);

                if (demand > currentCapacity && timeToReturnNow <= closingTime) {
                    if (currentNode != depotNode) {
                        totalDistance += this.graph.getEdgeWeight(currentNode, depotNode);
                    }
                    currentTime = timeToReturnNow;
                    currentNode = depotNode;
                    currentCapacity = valuesFromTestData.getCapacityLimit();

                    double distanceFromDepot = this.graph.getEdgeWeight(depotNode, nextCustomer);
                    double arrivalTimeFromDepot = currentTime + (distanceFromDepot / this.timestep);
                    if (arrivalTimeFromDepot < this.visitOpeningTime.get(nextCustomer)) {
                        double diff = this.visitOpeningTime.get(nextCustomer) - arrivalTimeFromDepot;
                        arrivalTimeFromDepot += Math.ceil(diff / this.timestep) * this.timestep;
                    }
                    double returnTimeFromDepot = arrivalTimeFromDepot + (this.graph.getEdgeWeight(nextCustomer, depotNode) / this.timestep);

                    if (demand <= currentCapacity && returnTimeFromDepot <= closingTime) {
                        currentCapacity -= demand;
                        totalDistance += distanceFromDepot;
                        currentTime = arrivalTimeFromDepot;
                        currentNode = nextCustomer;
                    } else {

                        vehicleNumber++;
                        if(vehicleNumber >= numberOfVehicles) {
                            unservedClients++;
                            vehicleNumber--;
                        } else {
                            depotNode = listOfDepotsIndexes.get(vehicleNumber % numberOfDepots);
                            currentTime = this.currentVehicleTime.get(vehicleNumber);
                            currentCapacity = this.currentVehicleCapacity.get(vehicleNumber);
                            currentNode = this.lastVehiclePosition.get(vehicleNumber);
                            i--;
                        }
                    }
                }
                else {
                    if(currentNode != depotNode)
                        totalDistance += this.graph.getEdgeWeight(currentNode, depotNode);

                    vehicleNumber++;
                    if(vehicleNumber >= numberOfVehicles){
                        unservedClients++;
                        vehicleNumber--;
                    } else {
                        depotNode = listOfDepotsIndexes.get(vehicleNumber % numberOfDepots);
                        currentTime = this.currentVehicleTime.get(vehicleNumber);
                        currentCapacity = this.currentVehicleCapacity.get(vehicleNumber);
                        currentNode = this.lastVehiclePosition.get(vehicleNumber);
                        i--;
                    }
                }
            }
        }

        if(currentNode != depotNode){
            totalDistance += this.graph.getEdgeWeight(currentNode, depotNode);
        }

        double finalFitness = totalDistance + (unservedClients * 100000.0);

        this.fitnessCache.put(individual, finalFitness);

        return finalFitness;
    }

    private List<List<Integer>> decodeToRoutes(List<Integer> bestIndividual) {
        List<List<Integer>> solution = new ArrayList<>();
        int numberOfVehicles = this.valuesFromTestData.getNumberOfVehicles();
        for(int i = 0; i < numberOfVehicles; i++){
            solution.add(new ArrayList<>());
        }

        List<Integer> listOfDepotsIndexes = this.valuesFromTestData.getListOfDepotsIndex();
        int numberOfDepots = listOfDepotsIndexes.size();
        int vehicleNumber = 0;

        int depotNode = listOfDepotsIndexes.get(vehicleNumber % numberOfDepots);
        double currentTime = this.currentVehicleTime.get(vehicleNumber);
        int currentCapacity = this.currentVehicleCapacity.get(vehicleNumber);
        int currentNode = this.lastVehiclePosition.get(vehicleNumber);

        solution.get(vehicleNumber).add(currentNode);

        for(int i = 0; i < bestIndividual.size(); i++){
            int nextCustomer = bestIndividual.get(i);

            if (!this.activeClients.contains(nextCustomer)) {
                continue;
            }

            int demand = Math.abs(this.demands.get(nextCustomer));
            double distance = (currentNode == nextCustomer) ? 0.0 : this.graph.getEdgeWeight(currentNode, nextCustomer);

            double arrivalTime = currentTime + (distance / this.timestep);

            if (arrivalTime < this.visitOpeningTime.get(nextCustomer)) {
                double diff = this.visitOpeningTime.get(nextCustomer) - arrivalTime;
                arrivalTime += Math.ceil(diff / this.timestep) * this.timestep;
            }

            double returnTime = arrivalTime + (this.graph.getEdgeWeight(nextCustomer, depotNode) / this.timestep);
            int closingTime = this.valuesFromTestData.getDepotOpenTimeFrame().get(depotNode)[1];

            if(demand <= currentCapacity && returnTime <= closingTime){
                currentCapacity -= demand;
                currentTime = arrivalTime;
                currentNode = nextCustomer;
                solution.get(vehicleNumber).add(nextCustomer);
            } else {
                double timeToReturnNow = currentTime + (this.graph.getEdgeWeight(currentNode, depotNode) / this.timestep);

                if (demand > currentCapacity && timeToReturnNow <= closingTime) {
                    if (currentNode != depotNode) {
                        solution.get(vehicleNumber).add(depotNode);
                    }
                    currentTime = timeToReturnNow;
                    currentNode = depotNode;
                    currentCapacity = valuesFromTestData.getCapacityLimit();

                    double distanceFromDepot = this.graph.getEdgeWeight(depotNode, nextCustomer);
                    double arrivalTimeFromDepot = currentTime + (distanceFromDepot / this.timestep);
                    if (arrivalTimeFromDepot < this.visitOpeningTime.get(nextCustomer)) {
                        double diff = this.visitOpeningTime.get(nextCustomer) - arrivalTimeFromDepot;
                        arrivalTimeFromDepot += Math.ceil(diff / this.timestep) * this.timestep;
                    }
                    double returnTimeFromDepot = arrivalTimeFromDepot + (this.graph.getEdgeWeight(nextCustomer, depotNode) / this.timestep);

                    if (demand <= currentCapacity && returnTimeFromDepot <= closingTime) {
                        currentCapacity -= demand;
                        currentTime = arrivalTimeFromDepot;
                        currentNode = nextCustomer;
                        solution.get(vehicleNumber).add(nextCustomer);
                    } else {
                        vehicleNumber++;
                        if(vehicleNumber >= numberOfVehicles){
                            vehicleNumber--;
                        } else {
                            depotNode = listOfDepotsIndexes.get(vehicleNumber % numberOfDepots);
                            currentTime = this.currentVehicleTime.get(vehicleNumber);
                            currentCapacity = this.currentVehicleCapacity.get(vehicleNumber);
                            currentNode = this.lastVehiclePosition.get(vehicleNumber);
                            solution.get(vehicleNumber).add(currentNode);
                            i--;
                        }
                    }
                }
                else {
                    if(currentNode != depotNode){
                        solution.get(vehicleNumber).add(depotNode);
                    }

                    vehicleNumber++;
                    if(vehicleNumber >= numberOfVehicles){
                        vehicleNumber--;
                    } else {
                        depotNode = listOfDepotsIndexes.get(vehicleNumber % numberOfDepots);
                        currentTime = this.currentVehicleTime.get(vehicleNumber);
                        currentCapacity = this.currentVehicleCapacity.get(vehicleNumber);
                        currentNode = this.lastVehiclePosition.get(vehicleNumber);
                        solution.get(vehicleNumber).add(currentNode);
                        i--;
                    }
                }
            }
        }

        for(int i = 0; i < numberOfVehicles; i++){
            List<Integer> route = solution.get(i);
            int assignedDepot = listOfDepotsIndexes.get(i % numberOfDepots);

            if(route.isEmpty()) {
                route.add(this.lastVehiclePosition.get(i));
            }

            if(route.size() == 1 || !isDepot(route.get(route.size() - 1))) {
                route.add(assignedDepot);
            }
        }

        return solution;
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

    private List<Integer> findBestSolution(List<List<Integer>> currentPopulation){
        List<Integer> bestIndividual = currentPopulation.getFirst();
        double bestFitness = Double.MAX_VALUE;

        for(List<Integer> individual : currentPopulation){
            double currentFitness = computeFitness(individual);
            if(currentFitness < bestFitness){
                bestIndividual = individual;
                bestFitness = currentFitness;
            }
        }

        return bestIndividual;
    }

    private List<List<Integer>> crossover(List<List<Integer>> currentPopulation){
        List<List<Integer>> nextGeneration = new ArrayList<>();
        List<Integer> bestIndividual = findBestSolution(currentPopulation);

        nextGeneration.add(bestIndividual);

        while(nextGeneration.size() < POPULATION_COUNT){

            List<Integer> parent1 = rouletteWheelSelection(currentPopulation);
            List<Integer> parent2 = rouletteWheelSelection(currentPopulation);

            List<Integer> child = applyOrderCrossover(parent1, parent2);
            nextGeneration.add(child);

        }

        return nextGeneration;

    }

    private List<List<Integer>> twoOpt(List<List<Integer>> population){
        int bestIndex = 0;
        double minFitness = Double.MAX_VALUE;

        for (int i = 0; i < population.size(); i++) {
            double f = computeFitness(population.get(i));
            if (f < minFitness) {
                minFitness = f;
                bestIndex = i;
            }
        }

        List<Integer> bestRoute = new ArrayList<>(population.get(bestIndex));
        double bestDistance = computeFitness(bestRoute);
        boolean improvement = true;

        while(improvement){
            improvement = false;

            for(int i = 0; i < bestRoute.size() - 1; i++){
                for(int j = i + 1; j < bestRoute.size(); j++){
                    List<Integer> newRoute = new ArrayList<>(bestRoute);

                    Collections.reverse(newRoute.subList(i, j + 1));

                    double newDistance = computeFitness(newRoute);

                    if(newDistance < bestDistance){
                        bestRoute = newRoute;
                        bestDistance = newDistance;
                        improvement = true;

                        break;
                    }
                }
                if(improvement)
                    break;
            }
        }

        population.set(bestIndex, bestRoute);

        return population;

    }

    private void validateAllClients(List<List<Integer>> bestRoutes, Set<Integer> activeClients) {
        Set<Integer> visited = new HashSet<>();
        for(List<Integer> route : bestRoutes){
            visited.addAll(route);
        }
        visited.remove(0);

        List<Integer> missingClients = new ArrayList<>();
        for (Integer client : activeClients) {
            if(!visited.contains(client)) {
                missingClients.add(client);
            }
        }

        if(missingClients.isEmpty())
            return;

        int mIdx = 0;
        for (List<Integer> route : bestRoutes) {
            if (mIdx >= missingClients.size())
                break;

            if (route.size() == 1 && route.get(0) == 0) {
                int count = 0;

                while (mIdx < missingClients.size() && count < 5) {
                    route.add(missingClients.get(mIdx));
                    mIdx++;
                    count++;
                }
                route.add(0);
            }
        }

        while (mIdx < missingClients.size()) {
            List<Integer> route0 = bestRoutes.get(0);
            if (route0.get(route0.size() - 1) != 0) {
                route0.add(0);
            }

            int count = 0;
            while (mIdx < missingClients.size() && count < 5) {
                route0.add(missingClients.get(mIdx));
                mIdx++;
                count++;
            }
            route0.add(0);
        }
    }

    public List<List<Integer>> runGA(){
        this.fitnessUsageCounter = 0;
        this.fitnessCache.clear();
        List<List<Integer>> currentPopulation = initialSolution();
        List<Integer> theBestIndividual = new ArrayList<>();
        Mutations mutations = new Mutations();
        double bestDistance = Double.MAX_VALUE;
        int iteration = 0;
        while(this.fitnessUsageCounter < MAX_EVALUATIONS){
            iteration += 1;
            //System.out.println("Current number of iterations: " + iteration);
            //System.out.print("FFE Consumed: " + this.fitnessUsageCounter + "/" + MAX_EVALUATIONS);
            currentPopulation = crossover(currentPopulation);

            currentPopulation = mutations.mutation(currentPopulation);

            currentPopulation = twoOpt(currentPopulation);

            List<Integer> bestIndividual = findBestSolution(currentPopulation);
            double bestFitness = computeFitness(bestIndividual);
            //System.out.println(bestIndividual);
            //System.out.println("Best distance: " + bestFitness);
            if (bestFitness < bestDistance){
                bestDistance = bestFitness;
                theBestIndividual = new ArrayList<>(bestIndividual);
            }
        }

        List<List<Integer>> bestRoutesFinal = decodeToRoutes(theBestIndividual);
        this.validateAllClients(bestRoutesFinal, this.activeClients);
        return bestRoutesFinal;
    }
}
