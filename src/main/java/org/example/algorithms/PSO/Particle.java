package org.example.algorithms.PSO;

import java.util.Random;

public class Particle {
    private double[] coordinates;
    private double[] velocity;
    private double[] bestCoordinates;
    private int dimensions;
    private double bestFitness = Double.MAX_VALUE;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private final Random random;

    private final int MAX_SPEED = 10;

    public Particle(int dimensions, double minX, double maxX, double minY, double maxY, Random random){
        this.coordinates = new double[dimensions];
        this.velocity = new double[dimensions];
        this.dimensions = dimensions;
        this.random = random;

        this.maxX = maxX;
        this.minX = minX;
        this.maxY = maxY;
        this.minY = minY;

        for(int i = 0; i < dimensions; i+=2){
            this.coordinates[i] = minX + random.nextDouble() * (maxX - minX);
            this.coordinates[i+1] = minY + random.nextDouble() * (maxY - minY);
        }

        for(int i = 0; i < dimensions; i ++){
            this.velocity[i] = random.nextDouble() * this.MAX_SPEED * isNegative();
        }

        this.bestCoordinates = this.coordinates.clone();
    }

    public void resetVelocity(){
        for(int i = 0; i < dimensions; i ++){
            this.velocity[i] = random.nextDouble() * this.MAX_SPEED * isNegative();
        }
    }

    public void randomizePosition(){
        for(int i = 0; i < dimensions; i+=2){
            this.coordinates[i] = minX + random.nextDouble() * (maxX - minX);
            this.coordinates[i+1] = minY + random.nextDouble() * (maxY - minY);
        }

        this.bestCoordinates = this.coordinates.clone();
        this.bestFitness = Double.MAX_VALUE;
        resetVelocity();
    }

    private int isNegative(){
        if(random.nextDouble() < 0.5)
            return -1;
        else
            return 1;
    }

    public double getBestFitness() {
        return bestFitness;
    }

    public double[] getBestCoordinates() {
        return bestCoordinates;
    }

    public double[] getCoordinates() {
        return coordinates;
    }

    public double[] getVelocity() {
        return velocity;
    }

    public void setBestCoordinates(double[] currentBestCoordinates){
        this.bestCoordinates = currentBestCoordinates.clone();
    }

    public void setBestFitness(double bestFitness) {
        this.bestFitness = bestFitness;
    }
}
