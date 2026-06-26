package org.example.algorithms.VNS;

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

    private Map<Integer, Integer> lastVehiclePosition;
    private Map<Integer, Integer> currentVehicleTime;
    private Set<Integer> activeClients;
    private Map<Integer, Integer> currentVehicleCapacity;
    private List<List<Integer>> plannedRoutes;

    private final long TIME_LIMIT = 125 * 1000;
    private final int TIME_SLICE = 40;


    public VNS(MapBuilder mapBuilder, Map<Integer, Integer> lastVehiclePosition, Map<Integer, Integer> currentVehicleTime, Set<Integer> activeClients, Map<Integer, Integer> currentVehicleCapacity, List<List<Integer>> plannedRoutes){
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
        this.lastVehiclePosition = lastVehiclePosition;
        this.currentVehicleTime = currentVehicleTime;
        this.activeClients = activeClients;
        this.currentVehicleCapacity = currentVehicleCapacity;
        this.plannedRoutes = plannedRoutes;

    }

    private boolean isDepot(int node){
        return this.depotSet.contains(node);
    }

    private Map<Integer, Boolean> initializeVisitedList(){
        int[] listOfNodes = this.graph.vertices();
        Map<Integer, Boolean> customerNodes = new HashMap<>();

        for(int node : listOfNodes){
            if(!isDepot(node)){
                if(this.activeClients.contains(node)){
                    customerNodes.put(node, false);
                } else {
                    customerNodes.put(node, true);
                }
            }
        }

        for(Integer startNode : this.lastVehiclePosition.values()){
            if(!isDepot(startNode) && customerNodes.containsKey(startNode)){
                customerNodes.put(startNode, true);
            }
        }
        return customerNodes;
    }

    private boolean isRouteOnTime(List<Integer> route, int vehicleIndex){
        if (route.size() <= 2)
            return true;
        double routeTime = this.currentVehicleTime.get(vehicleIndex);
        int currentNode = this.lastVehiclePosition.get(vehicleIndex);

        for(int i = 1; i < route.size(); i++){
            int nextNode = route.get(i);

            double distance = (currentNode == nextNode) ? 0.0 : this.graph.getEdgeWeight(currentNode, nextNode);
            routeTime += distance / this.timestep;

            if(!isDepot(nextNode)){
                while(routeTime < this.visitOpeningTime.get(nextNode)){
                    routeTime += this.timestep;
                }
            }
            currentNode = nextNode;
        }

        if(routeTime > this.valuesFromTestData.getDepotOpenTimeFrame().get(0)[1])
            return false;
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


    private List<List<Integer>> buildInitialSolution() {
        List<List<Integer>> solution = new ArrayList<>();
        this.visitedNodes = initializeVisitedList();

        for(List<Integer> plannedRoute : this.plannedRoutes){
            List<Integer> copy = new ArrayList<>(plannedRoute);

            if (copy.size() > 1 && isDepot(copy.get(copy.size() - 1))){
                copy.remove(copy.size() - 1);
            }

            solution.add(copy);

            for(Integer node : copy){
                if(!isDepot(node))
                    this.visitedNodes.put(node, true);
            }
        }

        int numberOfDepotsAvailable = this.valuesFromTestData.getListOfDepotsIndex().size();

        List<Integer> vehicleOrder = new ArrayList<>();
        for(int i = 0; i < this.numberOfVehicles; i++){
            vehicleOrder.add(i);
        }
        Collections.shuffle(vehicleOrder);

        boolean clientsLeft = true;
        while(clientsLeft) {
            clientsLeft = false;

            for(int i : vehicleOrder) {
                List<Integer> route = solution.get(i);

                int depotIndex = this.valuesFromTestData.getListOfDepotsIndex().get(i % numberOfDepotsAvailable);
                int depotNode = this.valuesFromTestData.getMapDepotToNode().get(depotIndex);
                int depotClosingTime = this.valuesFromTestData.getDepotOpenTimeFrame().get(depotNode)[1];

                int capacity = this.currentVehicleCapacity.get(i);
                double routeTime = this.currentVehicleTime.get(i);
                int currentNode = route.get(0);
                int clientsInCurrentTrip = 0;

                for(int j = 1; j < route.size(); j++){
                    int next = route.get(j);
                    if(isDepot(next)){
                        capacity = this.vehicleCapacity;
                        clientsInCurrentTrip = 0;
                    } else {
                        capacity -= Math.abs(this.demands.get(next));
                        clientsInCurrentTrip++;
                    }

                    routeTime += this.graph.getEdgeWeight(currentNode, next) / this.timestep;
                    if(!isDepot(next)){
                        while(routeTime < visitOpeningTime.get(next)) {
                            routeTime += timestep;
                        }
                    }
                    currentNode = next;
                }

                boolean addedToVehicle = false;

                while(true){

                    int nextNode = -1;
                    if(clientsInCurrentTrip < 5){
                        nextNode = getBestNode(currentNode, capacity, visitedNodes, routeTime, depotNode);
                    }


                    if(nextNode == -1){
                        double timeToDepot = this.graph.getEdgeWeight(currentNode, depotNode) / this.timestep;
                        if (routeTime + timeToDepot < depotClosingTime && !isDepot(currentNode)){
                            route.add(depotNode);
                        }
                        break;
                    }


                    double travelTime = this.graph.getEdgeWeight(currentNode, nextNode) / this.timestep;
                    double arrivalTime = routeTime + travelTime;

                    while(arrivalTime < visitOpeningTime.get(nextNode)){
                        arrivalTime += timestep;
                    }

                    double returnTime = this.graph.getEdgeWeight(nextNode, depotNode) / this.timestep;

                    if(arrivalTime + returnTime > depotClosingTime)
                        break;

                    routeTime = arrivalTime;
                    capacity -= Math.abs(this.demands.get(nextNode));
                    visitedNodes.put(nextNode, true);
                    route.add(nextNode);
                    currentNode = nextNode;
                    addedToVehicle = true;
                    clientsInCurrentTrip++;
                }

                if(addedToVehicle)
                    clientsLeft = true;
            }
        }

        for(int i : vehicleOrder){
            List<Integer> route = solution.get(i);
            int depotIndex = this.valuesFromTestData.getListOfDepotsIndex().get(i % numberOfDepotsAvailable);
            int depotNode = this.valuesFromTestData.getMapDepotToNode().get(depotIndex);

            if (route.size() == 1 || (route.size() > 0 && !isDepot(route.get(route.size() - 1)))) {
                route.add(depotNode);
            }
        }
        return solution;

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

        int activeRoutes = 0;
        for (List<Integer> r : currentSolution) {
            if (r.size() > 2)
                activeRoutes++;
        }

        if (k == 0 && activeRoutes < 2) {
            k = 1;
        }

        for(int shakes = 0; shakes < 2; shakes++){

            route1 = -1;
            int triesRoute1 = 0;
            while(triesRoute1 < 100) {
                int tempRoute1 = (int) (Math.random() * currentSolution.size());
                if (currentSolution.get(tempRoute1).size() > 2) {
                    route1 = tempRoute1;
                    break;
                }
                triesRoute1++;
            }

            if (route1 == -1) {
                break;
            }
            route2 = -1;
            int tries = 0;

            while (tries < 50) {
                int tempRoute2 = (int) (Math.random() * currentSolution.size());

                if(k == 0) {
                    if(tempRoute2 != route1 && currentSolution.get(tempRoute2).size() > 2) {
                        route2 = tempRoute2;
                        break;
                    }
                } else {
                    if(tempRoute2 != route1){
                        route2 = tempRoute2;
                        break;
                    }
                }

                tries++;
            }

            if(route2 == -1){
                if(k == 0){
                    route2 = route1;
                } else {
                    route2 = (route1 + 1) % currentSolution.size();
                }
            }

            int retries = 0;
            boolean valid = false;
            List<List<Integer>> tempSolution = currentSolution;

            while(!valid && retries < 50) {
                positionRoute1 = getCustomerPosition(currentSolution.get(route1));
                positionRoute2 = getCustomerPosition(currentSolution.get(route2));

                if (k == 1) {
                    tempSolution = neighborhoods.relocateCustomer(route1, positionRoute1, route2, positionRoute2);
                } else {
                    tempSolution = neighborhoods.swapCustomers(route1, positionRoute1, route2, positionRoute2);
                }

                valid = verifyCapacity(tempSolution) &&
                        isRouteOnTime(tempSolution.get(route1), route1) &&
                        isRouteOnTime(tempSolution.get(route2), route2);

                retries++;
            }

            if (valid) {
                currentSolution = tempSolution;
            } else {
                currentSolution = neighborhoods.copyCurrentSolution(solution);
            }


        }
        return currentSolution;
    }

    private double totalDistance(List<List<Integer>> solution){
        double totalDistance = 0.0;
        int penalty = 0;

        for(List<Integer> route : solution) {
            if(route.size() <= 2)
                continue;

            int clientsInCurrentTrip = 0;

            for(int i = 0; i < route.size() - 1; i++){
                int node1 = route.get(i);
                int node2 = route.get(i+1);
                totalDistance += (node1 == node2)? 0.0 : graph.getEdgeWeight(node1, node2);

                if(!isDepot(node2)) {
                    clientsInCurrentTrip++;
                } else {
                    clientsInCurrentTrip = 0;
                }

                if (clientsInCurrentTrip > 5){
                    penalty += 10000;
                }
            }
        }

        return totalDistance + penalty;
    }

    private List<List<Integer>> localSearch(List<List<Integer>> solution, long startTime){
        List<List<Integer>> currentSolution = solution;
        Neighborhoods neighborhoods = new Neighborhoods(currentSolution);

        List<List<Integer>> bestSolution = neighborhoods.copyCurrentSolution(currentSolution);
        double bestDistance = totalDistance(currentSolution);
        long maxAllocatedTime = TIME_LIMIT / TIME_SLICE;

        for(int route1 = 0; route1 < currentSolution.size(); route1++) {
            for (int route2 = 0; route2 < currentSolution.size(); route2++) {

                if (currentSolution.get(route1).size() <= 2 && currentSolution.get(route2).size() <= 2)
                    continue;

                for (int i = 1; i < currentSolution.get(route1).size() - 1; i++) {
                    if (isDepot(currentSolution.get(route1).get(i)))
                        continue;

                    for (int j = 1; j < currentSolution.get(route2).size(); j++) {

                        if(route1 == route2 && (i == j || i == j-1))
                            continue;

                        if (System.currentTimeMillis() - startTime >= maxAllocatedTime) {
                            return bestSolution;
                        }

                        int insertIndexJ = (route1 == route2 && i < j) ? j- 1 : j;
                        List<List<Integer>> possibleSolutionRelocate = neighborhoods.relocateCustomer(route1, i, route2, insertIndexJ);
                        //System.out.println("Solution Relocate: " + possibleSolutionRelocate);
                        if (verifyCapacity(possibleSolutionRelocate) &&
                                isRouteOnTime(possibleSolutionRelocate.get(route1), route1) &&
                                isRouteOnTime(possibleSolutionRelocate.get(route2), route2)) {
                            //System.out.println("A relocation has been made");
                            double newDistance = totalDistance(possibleSolutionRelocate);
                            if(newDistance < bestDistance){
                                bestSolution = possibleSolutionRelocate;
                                bestDistance = newDistance;
                            }
                        }
                        if(route1 != route2 || i < j) {
                            if (j < currentSolution.get(route2).size() - 1) {

                                if(!isDepot(currentSolution.get(route2).get(j))) {
                                    List<List<Integer>> possibleSolutionSwap = neighborhoods.swapCustomers(route1, i, route2, j);
                                    if(verifyCapacity(possibleSolutionSwap) &&
                                            isRouteOnTime(possibleSolutionSwap.get(route1), route1) &&
                                            isRouteOnTime(possibleSolutionSwap.get(route2), route2)) {
                                        //System.out.println("A swap has been made");
                                        double newDistance = totalDistance(possibleSolutionSwap);
                                        if (newDistance < bestDistance) {
                                            bestSolution = possibleSolutionSwap;
                                            bestDistance = newDistance;
                                        }
                                    }
                                }

                            }
                        }


                    }
                }
            }
        }

        return bestSolution;
    }


    private boolean verifyCapacity(List<List<Integer>> solution){
        for (int i = 0; i < solution.size(); i++){
            List<Integer> route = solution.get(i);
            int capacityLeft = this.currentVehicleCapacity.get(i);

            for (int j = 1 ; j < route.size(); j++){
                int node = route.get(j);
                if(!isDepot(node)){
                    capacityLeft -= Math.abs(this.demands.get(node));
                } else {
                    capacityLeft = this.vehicleCapacity;
                }


                if(capacityLeft < 0)
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

    public List<List<Integer>> runVNS(){
        List<List<Integer>> currentSolution = buildInitialSolution();
        //System.out.println("Trece");

        boolean hasCustomers = false;
        for (List<Integer> route : currentSolution) {
            if (route.size() > 2) {
                hasCustomers = true;
                break;
            }
        }
        if (!hasCustomers) {
            return currentSolution;
        }
        double bestDistance = totalDistance(currentSolution);

        long startTime = System.currentTimeMillis();

        //System.out.println("Initial Greedy solution: " + currentSolution);
        int kmax = 1;
        int k = 0;
        int iteration = 0;

        while(System.currentTimeMillis() - startTime < (TIME_LIMIT / TIME_SLICE) ){
            iteration += 1;
            List<List<Integer>> shakenSolution = shake(currentSolution, k);
            //System.out.println("Trece de shake");
            updateVisitedNodes(shakenSolution);
            //System.out.println("Shake of iteration: " + iteration);
            List<List<Integer>> improvedSolution = localSearch(shakenSolution, startTime);
            //System.out.println("Trece de local search");
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
            //System.out.println("Iteration " + iteration + ":");
            //System.out.println("Final solution: " + currentSolution);
            //System.out.println("Best distance: " + bestDistance);
        }
        validateAllClients(currentSolution, this.activeClients);
        return currentSolution;

    }
}
