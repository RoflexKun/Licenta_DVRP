package org.example.heuristics;

import org.example.CoordinatePair;
import org.example.DataParser;
import org.example.MapBuilder;
import org.graph4j.Graph;

import java.util.*;

public class VNS {
    private Graph graph;
    private DataParser valuesFromTestData;
    private Map<Integer, Integer> demands;
    private Map<Integer, CoordinatePair> coordinates;
    private int vehicleCapacity;
    private int numberOfVehicles;
    private Set<Integer> depotSet;
    private int timestep;
    private Map<Integer, Integer> visitOpeningTime;
    private Map<Integer, Boolean> visitedNodes;


    public VNS(MapBuilder mapBuilder){
        this.graph = mapBuilder.getMapFromData();
        this.valuesFromTestData = mapBuilder.getDataParsedFromTestData();
        this.demands = valuesFromTestData.getListOfCustomerDemand();
        this.coordinates = valuesFromTestData.getNodeCoordinates();
        this.vehicleCapacity = valuesFromTestData.getCapacityLimit();
        this.numberOfVehicles = valuesFromTestData.getNumberOfVehicles();
        this.depotSet = new HashSet<>(this.valuesFromTestData.getMapDepotToNode().values());

        // Essentials for DVRP
        this.timestep = valuesFromTestData.getTimestep();
        this.visitOpeningTime = valuesFromTestData.getVisitOpeningTime();

    }

    private boolean isDepot(int node){
        return this.depotSet.contains(node);
    }

    private Map<Integer, Boolean> initializeVisitedList(){
        int[] listOfNodes = this.graph.vertices();
        Map<Integer, Boolean> customerNodes = new HashMap<>();

        for(int node : listOfNodes){
            if(!isDepot(node)){
                customerNodes.put(node, false);
            }
        }
        return customerNodes;
    }

    private boolean isRouteOnTime(List<Integer> route){
        double routeTime = 0.0;

        for(int i = 0; i < route.size() - 1; i++){
            int nextNode = route.get(i + 1);

            if(!isDepot(nextNode) && routeTime < this.visitOpeningTime.get(nextNode)){
                routeTime += timestep;
                i--;
            } else {
                double distance = this.graph.getEdgeWeight(route.get(i), nextNode);
                routeTime += distance / timestep;
            }
        }

        if(routeTime > this.valuesFromTestData.getDepotOpenTimeFrame().get(route.getFirst())[1])
            return false;
        else
            return true;
    }

    private int getBestNode(int currentNode, int capacity, Map<Integer, Boolean> visitedNodes, double routeTime, int currentDepotNode) {
        int bestNode = -1;
        double bestArrival = Double.MAX_VALUE;
        int depotClosingTime = this.valuesFromTestData.getDepotOpenTimeFrame().get(currentDepotNode)[1];

        for (int node : this.graph.vertices()) {
            if (isDepot(node)) continue;

            int demand = Math.abs(this.demands.get(node));
            if (visitedNodes.get(node) || demand > capacity) continue;

            double distanceToNode = this.graph.getEdgeWeight(currentNode, node);

            double travelTime = distanceToNode / timestep;
            double arrivalTime = routeTime + travelTime;
            double releaseTime = visitOpeningTime.get(node);


            if (arrivalTime < releaseTime) {
                arrivalTime = releaseTime;
            }

            if (arrivalTime <= depotClosingTime && arrivalTime < bestArrival) {
                bestArrival = arrivalTime;
                bestNode = node;
            }
        }
        return bestNode;
    }


    private List<List<Integer>> greedySolution() {
        List<List<Integer>> indexOfSolution = new ArrayList<>();
        this.visitedNodes = initializeVisitedList();

        int numberOfDepotsAvailable = this.valuesFromTestData.getListOfDepotsIndex().size();
        for (int i = 0; i < this.numberOfVehicles; i++) {
            double routeTime = 0.0;

            int depotIndex = this.valuesFromTestData.getListOfDepotsIndex().get(i % numberOfDepotsAvailable);
            int depotNode = this.valuesFromTestData.getMapDepotToNode().get(depotIndex);
            int depotClosingTime = this.valuesFromTestData.getDepotOpenTimeFrame().get(depotNode)[1];

            List<Integer> solution = new ArrayList<>();
            solution.add(depotNode);

            int capacity = this.vehicleCapacity;
            int currentNode = depotNode;

            while (true) {
                int nextNode = getBestNode(currentNode, capacity, visitedNodes, routeTime, depotNode);

                if (nextNode == -1) {
                    routeTime += this.timestep;
                } else {
                    if (capacity >= Math.abs(this.demands.get(nextNode))) {
                        double travelTime = this.graph.getEdgeWeight(currentNode, nextNode) / this.timestep;

                        double returnTime = this.graph.getEdgeWeight(nextNode, depotNode) / this.timestep;
                        if (routeTime + travelTime + returnTime > depotClosingTime) {
                            routeTime += this.timestep;
                            continue;
                        }

                        routeTime += travelTime;
                        capacity -= Math.abs(this.demands.get(nextNode));
                        visitedNodes.put(nextNode, true);
                        solution.add(nextNode);
                        currentNode = nextNode;
                    } else {
                        routeTime += this.timestep;
                    }
                }

                double timeToDepot = this.graph.getEdgeWeight(currentNode, depotNode) / this.timestep;
                if (routeTime + timeToDepot > depotClosingTime) break;
            }

            solution.add(depotNode);
            indexOfSolution.add(solution);
        }

        return indexOfSolution;
    }

    private int getCustomerPosition(List<Integer> route){
        List<Integer> positions = new ArrayList<>();
        for(int i = 1; i < route.size() - 1; i++){
            if(!isDepot(route.get(i)))
                positions.add(i);
        }

        if(positions.isEmpty())
            return 1;

        return positions.get(new Random().nextInt(positions.size()));
    }

    private List<List<Integer>> shake(List<List<Integer>> solution, int k){
        Neighborhoods neighborhoods = new Neighborhoods(solution);

        int route1, route2, positionRoute1, positionRoute2;
        List<List<Integer>> currentSolution = neighborhoods.copyCurrentSolution(solution);
        for(int shakes = 0; shakes < 2; shakes++){

            do {
                route1 = (int) (Math.random() * currentSolution.size());
            } while (currentSolution.get(route1).size() <= 2);

            route2 = (int) (Math.random() * currentSolution.size());
            while(route1 == route2 || (k == 0 && currentSolution.get(route2).size() <= 2)){
                route2 = (int) (Math.random() * currentSolution.size());
            }


            positionRoute1 = getCustomerPosition(currentSolution.get(route1));
            positionRoute2 = getCustomerPosition(currentSolution.get(route2));
            int retries = 0;

            if(k == 1){
                currentSolution = neighborhoods.relocateCustomer(route1, positionRoute1, route2, positionRoute2);


                while(!verifyCapacity(currentSolution) && retries < 20){
                    positionRoute1 = getCustomerPosition(currentSolution.get(route1));
                    positionRoute2 = getCustomerPosition(currentSolution.get(route2));
                    currentSolution = neighborhoods.relocateCustomer(route1, positionRoute1, route2, positionRoute2);
                    retries += 1;
                }

                retries = 0;

                while((!isRouteOnTime(currentSolution.get(route1)) || !isRouteOnTime(currentSolution.get(route2))) && retries < 20){
                    positionRoute1 = getCustomerPosition(currentSolution.get(route1));
                    positionRoute2 = getCustomerPosition(currentSolution.get(route2));
                    currentSolution = neighborhoods.relocateCustomer(route1, positionRoute1, route2, positionRoute2);
                    retries += 1;
                }

                if(retries >= 20){
                    currentSolution = neighborhoods.copyCurrentSolution(solution);
                }

            } else if(k == 0){
                currentSolution = neighborhoods.swapCustomers(route1, positionRoute1, route2, positionRoute2);

                while(!verifyCapacity(currentSolution) && retries < 20){
                    positionRoute1 = getCustomerPosition(currentSolution.get(route1));
                    positionRoute2 = getCustomerPosition(currentSolution.get(route2));
                    currentSolution = neighborhoods.swapCustomers(route1, positionRoute1, route2, positionRoute2);
                    retries += 1;
                }

                retries = 0;

                while((!isRouteOnTime(currentSolution.get(route1)) || !isRouteOnTime(currentSolution.get(route2))) && retries < 20){
                    positionRoute1 = getCustomerPosition(currentSolution.get(route1));
                    positionRoute2 = getCustomerPosition(currentSolution.get(route2));
                    currentSolution = neighborhoods.swapCustomers(route1, positionRoute1, route2, positionRoute2);
                    retries += 1;
                }

                if(retries >= 20){
                    currentSolution = neighborhoods.copyCurrentSolution(solution);
                }

            }
        }
        return currentSolution;
    }

    private double totalDistance(List<List<Integer>> solution){
        double totalDistance = 0.0;

        for(List<Integer> route : solution){
            if(route.size() <= 2)
                continue;

            for(int i = 0; i < route.size() - 1; i++){
                int node1 = route.get(i);
                int node2 = route.get(i + 1);
                totalDistance += graph.getEdgeWeight(node1, node2);
            }
        }
        return totalDistance;
    }

    private List<List<Integer>> localSearch(List<List<Integer>> solution){
        List<List<Integer>> currentSolution = solution;
        Neighborhoods neighborhoods = new Neighborhoods(currentSolution);

        List<List<Integer>> bestSolution = neighborhoods.copyCurrentSolution(currentSolution);
        double bestDistance = totalDistance(currentSolution);

        for(int route1 = 0; route1 < currentSolution.size() - 1; route1++) {
            for (int route2 = route1 + 1; route2 < currentSolution.size(); route2++) {

                for (int i = 1; i < currentSolution.get(route1).size() - 1; i++) {
                    for (int j = 1; j < currentSolution.get(route2).size() - 1; j++) {

                        List<List<Integer>> possibleSolutionRelocate = neighborhoods.relocateCustomer(route1, i, route2, j);
                        //System.out.println("Solution Relocate: " + possibleSolutionRelocate);
                        if (!verifyCapacity(possibleSolutionRelocate) ||
                                !isRouteOnTime(possibleSolutionRelocate.get(route1)) ||
                                !isRouteOnTime(possibleSolutionRelocate.get(route2)))
                            continue;

                        double newDistance = totalDistance(possibleSolutionRelocate);
                        if (newDistance < bestDistance) {
                            bestSolution = possibleSolutionRelocate;
                            bestDistance = newDistance;
                        }

                        List<List<Integer>> possibleSolutionSwap = neighborhoods.swapCustomers(route1, i, route2, j);
                        //System.out.println("Solution Swap: " + possibleSolutionSwap);
                        if (!verifyCapacity(possibleSolutionSwap) ||
                                !isRouteOnTime(possibleSolutionSwap.get(route1)) ||
                                !isRouteOnTime(possibleSolutionSwap.get(route2)))
                            continue;

                        newDistance = totalDistance(possibleSolutionSwap);
                        if (newDistance < bestDistance) {
                            bestSolution = possibleSolutionSwap;
                            bestDistance = newDistance;
                        }

                    }
                }
            }
        }

        return bestSolution;
    }


    private boolean verifyCapacity(List<List<Integer>> solution){
        for(List<Integer> route : solution){
            int load = 0;
            for(Integer node : route){
                if(isDepot(node))
                    continue;
                load += Math.abs(this.demands.get(node));
                if(load > this.vehicleCapacity)
                    return false;
            }
        }
        return true;
    }

    private void updateVisitedNodes(List<List<Integer>> solution){
        this.visitedNodes = initializeVisitedList();
        for(List<Integer> route : solution){
            for(int node : route){
                if(!isDepot(node)){
                    this.visitedNodes.put(node, true);
                }
            }
        }
    }

    public void runVNS(){
        List<List<Integer>> currentSolution = greedySolution();
        double bestDistance = totalDistance(currentSolution);
        System.out.println("Initial Greedy solution: " + currentSolution);
        int kmax = 1;
        int k = 0;
        int maxIterations = 500;

        for(int i = 0; i < maxIterations; i++){
            List<List<Integer>> shakenSolution = shake(currentSolution, k);
            updateVisitedNodes(shakenSolution);
            System.out.println("Shake of iteration: " + i);
            List<List<Integer>> improvedSolution = localSearch(shakenSolution);
            updateVisitedNodes(improvedSolution);
            double improvedDistance = totalDistance(improvedSolution);

            if(improvedDistance < bestDistance){
                currentSolution = improvedSolution;
                bestDistance = improvedDistance;
                k = 1;
            } else {
                k++;
                if(k > kmax) {
                    k = 0;
                }
            }
            System.out.println("Iteration " + (i + 1) + ":");
            System.out.println("Final solution: " + currentSolution);
            System.out.println("Best distance: " + bestDistance);
        }

    }
}
