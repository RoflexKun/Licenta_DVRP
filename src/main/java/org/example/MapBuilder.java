package org.example;

import org.graph4j.Graph;
import org.graph4j.GraphBuilder;

public class MapBuilder {
    private DataParser dataParsedFromTestData;
    private Graph mapFromData;

    public MapBuilder(DataParser dataParser){
        this.dataParsedFromTestData = dataParser;
        buildGraph();

    }

    private void buildGraph(){
        this.mapFromData = GraphBuilder.empty().buildGraph();
        for(Integer node : dataParsedFromTestData.getNodeCoordinates().keySet()) {
            this.mapFromData.addLabeledVertex(node);
        }
        for(Integer iNodeIndex : this.mapFromData.vertices()){
            Integer iNode = (Integer) this.mapFromData.getVertexLabel(iNodeIndex);
            for(Integer jNodeIndex : this.mapFromData.vertices()){
                Integer jNode = (Integer) this.mapFromData.getVertexLabel(jNodeIndex);
                if (!iNode.equals(jNode)){
                    this.mapFromData.addEdge(iNode, jNode, dataParsedFromTestData
                            .getNodeCoordinates()
                            .get(iNode)
                            .distanceToNode(dataParsedFromTestData
                                    .getNodeCoordinates()
                                    .get(jNode)));
                }
            }
        }
    }

    public void printMap(){
        for(Integer i : this.mapFromData.vertices()){
            for(Integer j : this.mapFromData.vertices()){
                if(i < j){
                    System.out.println(this.mapFromData.getVertexLabel(i) + " - " + this.mapFromData.getVertexLabel(j) + " : " + this.mapFromData.getEdgeWeight(i, j));
                }
            }
        }
    }

    public Graph getMapFromData() {
        return mapFromData;
    }

    public DataParser getDataParsedFromTestData() {
        return dataParsedFromTestData;
    }
}
