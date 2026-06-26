package org.example.algorithms.PSO;

import org.example.CoordinatePair;
import org.example.DataParser;
import org.example.MapBuilder;
import org.graph4j.Graph;

import java.util.*;


public class PSO {

    private DataParser valuesFromTestData;
    private double maxX;
    private double minX;
    private double maxY;
    private double minY;
    private double[] bestCoordinates;
    private double bestFitness = Double.MAX_VALUE;
    private List<Particle> swarm = new ArrayList<>();
    private int dimensions;
    private Map<Integer, CoordinatePair> nodeCoordinates = new HashMap<>();
    private Set<Integer> depotSet;
    private Map<List<Integer>, Double> fitnessCache = new HashMap<>();
    private final Random random = new Random();

    private Graph graph;
    private int timestep;
    private Map<Integer, Integer> visitOpeningTime;
    private Map<Integer, Integer> demands;
    private Map<Integer, Integer> lastVehiclePosition;
    private Map<Integer, Integer> currentVehicleTime;
    private Set<Integer> activeClients;
    private Map<Integer, Integer> currentVehicleCapacity;
    private List<List<Integer>> plannedRoutes;

    private int PARTICLE_NUMBER = 22;
    private int REQUEST_CLUSTER_CENTERS = 2;
    private final int PENALITY_VALUE = (int)1e9;
    private final long TIME_LIMIT = 75 * 1000;
    private final int TIME_SLICE = 40;


    public PSO(MapBuilder mapBuilder, Map<Integer, Integer> lastVehiclePosition, Map<Integer, Integer> currentVehicleTime, Set<Integer> activeClients, Map<Integer, Integer> currentVehicleCapacity, List<List<Integer>> plannedRoutes){
        this.valuesFromTestData = mapBuilder.getDataParsedFromTestData();
        this.maxX = valuesFromTestData.getMaxX();
        this.minX = valuesFromTestData.getMinX();
        this.maxY = valuesFromTestData.getMaxY();
        this.minY = valuesFromTestData.getMinY();
        this.dimensions = 2 * this.REQUEST_CLUSTER_CENTERS * valuesFromTestData.getNumberOfVehicles();
        this.nodeCoordinates = valuesFromTestData.getNodeCoordinates();
        this.bestCoordinates = new double[this.dimensions];
        this.depotSet = new HashSet<>(this.valuesFromTestData.getMapDepotToNode().values());

        this.lastVehiclePosition = lastVehiclePosition;
        this.currentVehicleTime = currentVehicleTime;
        this.activeClients = activeClients;
        this.currentVehicleCapacity = currentVehicleCapacity;
        this.plannedRoutes = plannedRoutes;

        double paddingX = (this.maxX - this.minX) * 0.3;
        double paddingY = (this.maxY - this.minY) * 0.3;

        for(int i = 0; i < this.PARTICLE_NUMBER; i++){
            Particle particle = new Particle(this.dimensions,
                    this.minX - paddingX, this.maxX + paddingX,
                    this.minY - paddingY, this.maxY + paddingY,
                    random);
            this.swarm.add(particle);
        }

        this.graph = mapBuilder.getMapFromData();
        this.demands = valuesFromTestData.getListOfCustomerDemand();
        this.timestep = valuesFromTestData.getTimestep();
        this.visitOpeningTime = valuesFromTestData.getVisitOpeningTime();
    }

    private boolean isDepot(int node){
        return this.depotSet.contains(node);
    }

    private double computeRouteFitness(List<Integer> route, int vehicleIndex) {

        if(route.size() <= 2)
            return 0.0;

        double totalDistance = 0.0;
        int currentCapacity = this.currentVehicleCapacity.get(vehicleIndex);
        double currentTime = this.currentVehicleTime.get(vehicleIndex);
        int depotNode = this.valuesFromTestData.getListOfDepotsIndex().get(vehicleIndex % this.valuesFromTestData.getNumberOfDepots());

        for (int i = 0; i < route.size() - 1; i++) {
            int currentNode = route.get(i);
            int nextNode = route.get(i + 1);

            int openingTime = isDepot(nextNode) ? 0 : this.visitOpeningTime.get(nextNode);
            double distance = (currentNode == nextNode) ? 0.0 : this.graph.getEdgeWeight(currentNode, nextNode);
            if (Double.isNaN(distance)) {
                distance = PENALITY_VALUE;
            }
            double arrivalTime = currentTime + (distance / this.timestep);

            while (arrivalTime < openingTime) {
                arrivalTime += this.timestep;
            }

            currentTime = arrivalTime;
            totalDistance += distance;

            if (!isDepot(nextNode)) {
                currentCapacity -= Math.abs(this.demands.get(nextNode));
            }
        }

        int closingTime = this.valuesFromTestData.getDepotOpenTimeFrame().get(depotNode)[1];
        double finalFitness = totalDistance;

        double capacityViolation = 0.0;
        if(currentCapacity < 0)
            capacityViolation = Math.abs((double) currentCapacity);

        double timeViolation = 0.0;
        if(currentTime > closingTime)
            timeViolation = currentTime - closingTime;

        int clientsInTrip = 0;
        for(int node : route) {
            if(!isDepot(node) && node != this.lastVehiclePosition.get(vehicleIndex))
                clientsInTrip++;
        }

        double clientLimitViolation = 0.0;
        if (clientsInTrip > 5)
            clientLimitViolation = clientsInTrip - 5;

        if (capacityViolation > 0 || timeViolation > 0 || clientLimitViolation > 0) {
            finalFitness = totalDistance + PENALITY_VALUE
                    + (capacityViolation * 10000.0)
                    + (timeViolation * 10000.0)
                    + (clientLimitViolation * 50000.0);
        }

        return finalFitness;
    }

    private List<Integer> twoOpt(List<Integer> route, int vehicleIndex){

        if(route.size() <= 3)
            return route;

        List<Integer> bestRoute = new ArrayList<>(route);
        double bestDistance = computeRouteFitness(bestRoute, vehicleIndex);
        boolean improvement = true;

        while(improvement){
            improvement = false;

            for(int i = 1; i < bestRoute.size() - 2; i++){

                for(int j = i + 1; j < bestRoute.size() - 1; j++){

                    List<Integer> newRoute = new ArrayList<>(bestRoute);

                    Collections.reverse(newRoute.subList(i, j + 1));

                    double newDistance = computeRouteFitness(newRoute, vehicleIndex);

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

        return bestRoute;
    }


    private List<List<Integer>> assignClusters(Particle particle){

        int depotNumbers = this.valuesFromTestData.getNumberOfDepots();
        List<Integer> listOfDepotsIndexes = this.valuesFromTestData.getListOfDepotsIndex();


        List<List<Integer>> routes = new ArrayList<>();
        for(int i = 0; i < this.valuesFromTestData.getNumberOfVehicles(); i++){
            routes.add(new ArrayList<>());
            routes.get(i).add(this.lastVehiclePosition.get(i));
        }

        double[] clusters = particle.getCoordinates();
        Collection<Integer> activePositions = this.lastVehiclePosition.values();

        for(Integer nodeIndex : this.activeClients){
            if(isDepot(nodeIndex) || activePositions.contains(nodeIndex))
                continue;

            CoordinatePair customerCoordinates = this.nodeCoordinates.get(nodeIndex);

            if(isDepot(nodeIndex))
                continue;


            double bestDistanceToCustomer = Double.MAX_VALUE;
            int bestVehicleIndex = 0;

            for(int i=0; i < clusters.length; i += 2){
                CoordinatePair currentClusterCoordinates = new CoordinatePair(clusters[i], clusters[i + 1]);

                double distance = customerCoordinates.distanceToNode(currentClusterCoordinates);

                if(distance < bestDistanceToCustomer){
                    bestDistanceToCustomer = distance;
                    bestVehicleIndex = (i / 2) / this.REQUEST_CLUSTER_CENTERS;
                }
            }

            routes.get(bestVehicleIndex).add(nodeIndex);
        }

        for(int i = 0; i < this.valuesFromTestData.getNumberOfVehicles(); i++){
            int assignedDepot = listOfDepotsIndexes.get(i % depotNumbers);
            routes.get(i).add(assignedDepot);
        }

        return routes;
    }

    private void applySmartRepair(List<List<Integer>> routes) {
        int depotNumbers = this.valuesFromTestData.getNumberOfDepots();
        int numberOfVehicles = this.valuesFromTestData.getNumberOfVehicles();
        int maxCapacity = this.valuesFromTestData.getCapacityLimit();

        boolean needsRepair = true;
        int safetyCounter = 0;

        while (needsRepair && safetyCounter < 100) {
            needsRepair = false;
            safetyCounter++;

            for (int v = 0; v < numberOfVehicles; v++) {
                List<Integer> route = routes.get(v);
                int currentCap = this.currentVehicleCapacity.get(v);
                int assignedDepot = this.valuesFromTestData.getListOfDepotsIndex().get(v % depotNumbers);

                int clientsInTrip = 0;

                int i = 1;
                while (i < route.size()) {
                    int node = route.get(i);

                    if (i == route.size() - 1 && isDepot(node))
                        break;

                    if (isDepot(node) || node == this.lastVehiclePosition.get(v)) {
                        currentCap = maxCapacity;
                        clientsInTrip = 0;
                        i++;
                        continue;
                    }

                    int demand = Math.abs(this.demands.get(node));

                    if (demand > currentCap || clientsInTrip >= 5) {
                        needsRepair = true;

                        if (safetyCounter > 50) {
                            route.add(i, assignedDepot);
                            currentCap = maxCapacity;
                            clientsInTrip = 0;
                            continue;
                        }

                        route.remove(i);
                        int bestVehicle = -1;
                        double bestDist = Double.MAX_VALUE;

                        for (int k = 0; k < numberOfVehicles; k++) {
                            if (k == v)
                                continue;

                            List<Integer> targetRoute = routes.get(k);
                            int lastNode = this.lastVehiclePosition.get(k);

                            if (!targetRoute.isEmpty()) {
                                lastNode = targetRoute.get(targetRoute.size() - 1);
                                if (isDepot(lastNode) && targetRoute.size() > 1) {
                                    lastNode = targetRoute.get(targetRoute.size() - 2);
                                }
                            }

                            double dist = (lastNode == node) ? 0.0 : this.graph.getEdgeWeight(lastNode, node);
                            if (Double.isNaN(dist)) dist = Double.MAX_VALUE;

                            if (dist < bestDist) {
                                bestDist = dist;
                                bestVehicle = k;
                            }
                        }

                        if (bestVehicle != -1) {
                            List<Integer> targetRoute = routes.get(bestVehicle);
                            if (targetRoute.size() > 1 && isDepot(targetRoute.get(targetRoute.size() - 1))) {
                                targetRoute.add(targetRoute.size() - 1, node);
                            } else {
                                targetRoute.add(node);
                            }
                        } else {
                            route.add(i, assignedDepot);
                            currentCap = maxCapacity;
                            clientsInTrip = 0;
                        }

                    } else {
                        currentCap -= demand;
                        clientsInTrip++;
                        i++;
                    }
                }
            }
        }
    }

    private List<List<Integer>> decodeToRoutes(double[] coordinates) {
        int depotNumbers = this.valuesFromTestData.getNumberOfDepots();
        List<Integer> listOfDepotsIndexes = this.valuesFromTestData.getListOfDepotsIndex();
        int numberOfVehicles = this.valuesFromTestData.getNumberOfVehicles();

        List<List<Integer>> finalRoutes = new ArrayList<>();
        for(int i = 0; i < numberOfVehicles; i++){
            finalRoutes.add(new ArrayList<>());
            finalRoutes.get(i).add(this.lastVehiclePosition.get(i));
        }

        Collection<Integer> activePositions = this.lastVehiclePosition.values();

        for(Integer nodeIndex : this.activeClients){
            if(isDepot(nodeIndex) || activePositions.contains(nodeIndex))
                continue;

            CoordinatePair customerCoordinates = this.nodeCoordinates.get(nodeIndex);
            double bestDistance = Double.MAX_VALUE;
            int bestVehicle = 0;

            for(int i = 0; i < coordinates.length; i+=2){
                CoordinatePair clusterCoordinates = new CoordinatePair(coordinates[i], coordinates[i + 1]);
                double distance = customerCoordinates.distanceToNode(clusterCoordinates);
                if(distance < bestDistance) {
                    bestDistance = distance;
                    bestVehicle = (i / 2) / this.REQUEST_CLUSTER_CENTERS;
                }
            }
            finalRoutes.get(bestVehicle).add(nodeIndex);
        }

        for(int i = 0; i < numberOfVehicles; i++){
            List<Integer> route = finalRoutes.get(i);
            int assignedDepot = listOfDepotsIndexes.get(i % depotNumbers);
            List<Integer> optimized = twoOpt(route, i);

            if(!isDepot(optimized.get(optimized.size() - 1))){
                optimized.add(assignedDepot);
            }
            finalRoutes.set(i, optimized);
        }

        applySmartRepair(finalRoutes);

        for(int i = 0; i < numberOfVehicles; i++){
            List<Integer> route = finalRoutes.get(i);
            int assignedDepot = listOfDepotsIndexes.get(i % depotNumbers);

            if (route.isEmpty()) {
                route.add(this.lastVehiclePosition.get(i));
            }

            if (!isDepot(route.get(route.size() - 1))) {
                route.add(assignedDepot);
            }
        }

        return finalRoutes;
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

    public List<List<Integer>> runPSO(){

        this.fitnessCache.clear();

        this.bestFitness = Double.MAX_VALUE;
        for(Particle p : this.swarm) {
            p.setBestFitness(Double.MAX_VALUE);
        }

        double a = 0.60;
        double l = 2.0;
        double g = 2.20;
        long startTime = System.currentTimeMillis();
        int iteration = 0;

        int iterationsWithoutImprovement = 0;
        double previousBestFitness = this.bestFitness;

        while(System.currentTimeMillis() - startTime < (TIME_LIMIT / TIME_SLICE)){
            iteration++;

            if(this.bestFitness < previousBestFitness){
                iterationsWithoutImprovement = 0;
                previousBestFitness = this.bestFitness;
            } else {
                iterationsWithoutImprovement++;
            }

            if(iterationsWithoutImprovement >= 10) {

                int particlesToRestart = (int)(0.4 * PARTICLE_NUMBER);

                for(int i = 0; i < particlesToRestart; i++){
                    int randomIndex = random.nextInt(swarm.size());

                    Particle p = swarm.get(randomIndex);

                    if(p.getBestFitness() > this.bestFitness) {
                        p.randomizePosition();
                    }
                }

                iterationsWithoutImprovement = 0;
            }

            if(iteration >= 100 && iteration % 100 == 0){
                int randomIndex = random.nextInt(swarm.size());
                swarm.get(randomIndex).resetVelocity();
            }

            for(Particle particle : this.swarm){
                List<List<Integer>> initialRoutes = assignClusters(particle);

                List<List<Integer>> optimizedRoutes = new ArrayList<>();
                for(int v = 0; v < initialRoutes.size(); v++){
                    optimizedRoutes.add(twoOpt(initialRoutes.get(v), v));
                }

                applySmartRepair(optimizedRoutes);

                double currentParticleFitness = 0.0;
                for(int v = 0; v < optimizedRoutes.size(); v++){
                    List<Integer> route = optimizedRoutes.get(v);
                    if(route.size() <= 2) continue;

                    double routeFitness = computeRouteFitness(route, v);
                    currentParticleFitness += routeFitness;
                }

                if(currentParticleFitness < particle.getBestFitness()){
                    particle.setBestFitness(currentParticleFitness);
                    particle.setBestCoordinates(particle.getCoordinates().clone());
                }

                if(currentParticleFitness < bestFitness){
                    this.bestFitness = currentParticleFitness;
                    this.bestCoordinates = particle.getCoordinates().clone();
                }
            }

            //System.out.println("Best coordinates" + Arrays.toString(this.bestCoordinates));
            //System.out.println("Best distance: " + bestFitness);

            for(Particle particle : this.swarm){
                double[] currentCoordinates = particle.getCoordinates();
                double[] particleBestCoordinates = particle.getBestCoordinates();
                double[] velocity = particle.getVelocity();

                for(int i = 0; i < this.dimensions; i++){

                    velocity[i] =
                            a * velocity[i]
                                    + l * random.nextDouble() * (particleBestCoordinates[i] - currentCoordinates[i])
                                    + g * random.nextDouble() * (this.bestCoordinates[i] - currentCoordinates[i]);

                    if (Double.isNaN(velocity[i]) || Double.isInfinite(velocity[i])) {
                        velocity[i] = 0.0;
                    }

                    if (velocity[i] > 15.0)
                        velocity[i] = 15.0;

                    if (velocity[i] < -15.0)
                        velocity[i] = -15.0;

                    currentCoordinates[i] = currentCoordinates[i] + velocity[i];

                    if (Double.isNaN(currentCoordinates[i])) {
                        currentCoordinates[i] = this.minX + random.nextDouble() * (this.maxX - this.minX);
                    }

                    if(i % 2 == 0){
                        if(currentCoordinates[i] < this.minX) currentCoordinates[i] = this.minX;
                        if(currentCoordinates[i] > this.maxX) currentCoordinates[i] = this.maxX;
                    } else {
                        if(currentCoordinates[i] < this.minY) currentCoordinates[i] = this.minY;
                        if(currentCoordinates[i] > this.maxY) currentCoordinates[i] = this.maxY;
                    }
                }
            }
        }
        List<List<Integer>> bestRoutesFinal = decodeToRoutes(this.bestCoordinates);
        this.validateAllClients(bestRoutesFinal, this.activeClients);
        return bestRoutesFinal;
    }
}
