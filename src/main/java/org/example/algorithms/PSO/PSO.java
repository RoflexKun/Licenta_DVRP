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

    private int PARTICLE_NUMBER = 22;
    private int REQUEST_CLUSTER_CENTERS = 2;
    private final int PENALITY_VALUE = (int)1e9;
    private final long TIME_LIMIT = 75 * 1000;


    public PSO(MapBuilder mapBuilder){
        this.valuesFromTestData = mapBuilder.getDataParsedFromTestData();
        this.maxX = valuesFromTestData.getMaxX();
        this.minX = valuesFromTestData.getMinX();
        this.maxY = valuesFromTestData.getMaxY();
        this.minY = valuesFromTestData.getMinY();
        this.dimensions = 2 * this.REQUEST_CLUSTER_CENTERS * valuesFromTestData.getNumberOfVehicles();
        this.nodeCoordinates = valuesFromTestData.getNodeCoordinates();
        this.bestCoordinates = new double[this.dimensions];
        this.depotSet = new HashSet<>(this.valuesFromTestData.getMapDepotToNode().values());
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

    private double computeRouteFitness(List<Integer> route) {

        if(route.size() <= 2)
            return 0.0;

        if(this.fitnessCache.containsKey(route)){
            return this.fitnessCache.get(route);
        }

        double totalDistance = 0.0;
        int currentDemand = 0;
        double currentTime = 0.0;
        int depotNode = route.get(0);

        for (int i = 0; i < route.size() - 1; i++) {
            int currentNode = route.get(i);
            int nextNode = route.get(i + 1);

            int openingTime = isDepot(nextNode) ? 0 : this.visitOpeningTime.get(nextNode);

            if (currentTime < openingTime) {
                currentTime += this.timestep;
                i--;
                continue;
            }

            double distance = this.graph.getEdgeWeight(currentNode, nextNode);
            totalDistance += distance;

            if (nextNode != depotNode) {
                currentDemand += Math.abs(this.demands.get(nextNode));
            }


            currentTime += (distance / this.timestep);
        }

        int closingTime = this.valuesFromTestData.getDepotOpenTimeFrame().get(depotNode)[1];
        double finalFitness = totalDistance;

        if (currentDemand > this.valuesFromTestData.getCapacityLimit() || currentTime > closingTime) {
            finalFitness = totalDistance + PENALITY_VALUE;
        }

        this.fitnessCache.put(new ArrayList<>(route), finalFitness);

        return finalFitness;
    }

    private List<Integer> twoOpt(List<Integer> route){

        if(route.size() <= 3)
            return route;

        List<Integer> bestRoute = new ArrayList<>(route);
        double bestDistance = computeRouteFitness(bestRoute);
        boolean improvement = true;

        while(improvement){
            improvement = false;

            for(int i = 1; i < bestRoute.size() - 2; i++){

                for(int j = i + 1; j < bestRoute.size() - 1; j++){

                    List<Integer> newRoute = new ArrayList<>(bestRoute);

                    Collections.reverse(newRoute.subList(i, j + 1));

                    double newDistance = computeRouteFitness(newRoute);

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

        List<List<Integer>> routes = new ArrayList<>();
        for(int i = 0; i < this.valuesFromTestData.getNumberOfVehicles(); i++){
            routes.add(new ArrayList<>());
            routes.get(i).add(i % depotNumbers);
        }

        double[] clusters = particle.getCoordinates();

        for(Integer nodeIndex : this.nodeCoordinates.keySet()){
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
            routes.get(i).add(routes.get(i).get(0));
        }

        return routes;
    }

    public double runPSO(){

        this.fitnessCache.clear();

        double a = 0.60;
        double l = 2.0;
        double g = 2.20;
        long startTime = System.currentTimeMillis();
        int iteration = 0;

        int iterationsWithoutImprovement = 0;
        double previousBestFitness = this.bestFitness;

        while(System.currentTimeMillis() - startTime < TIME_LIMIT){
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


                double currentParticleFitness = 0.0;
                boolean isValid = true;

                for(List<Integer> route : initialRoutes){

                    if(route.size() <= 2)
                        continue;

                    List<Integer> optimizedRoute = twoOpt(route);

                    double routeFitness = computeRouteFitness(optimizedRoute);

                    currentParticleFitness += routeFitness;
                }

                if(currentParticleFitness < particle.getBestFitness()){
                    particle.setBestFitness(currentParticleFitness);
                    particle.setBestCoordinates(particle.getCoordinates());
                }

                if(currentParticleFitness < bestFitness){
                    this.bestFitness = currentParticleFitness;
                    this.bestCoordinates = particle.getCoordinates().clone();
                }
            }

            System.out.println("Best coordinates" + Arrays.toString(this.bestCoordinates));
            System.out.println("Best distance: " + bestFitness);

            for(Particle particle : this.swarm){
                double[] currentCoordinates = particle.getCoordinates();
                double[] particleBestCoordinates = particle.getBestCoordinates();
                double[] velocity = particle.getVelocity();

                for(int i = 0; i < this.dimensions; i++){

                    velocity[i] =
                            a * velocity[i]
                                    + l * random.nextDouble() * (particleBestCoordinates[i] - currentCoordinates[i])
                                    + g * random.nextDouble() * (this.bestCoordinates[i] - currentCoordinates[i]);

                    currentCoordinates[i] = currentCoordinates[i] + velocity[i];

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

        return this.bestFitness;
    }
}
