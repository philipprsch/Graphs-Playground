import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

//public class Network<T, E extends Edge<T>> extends DirectedGraph<T, E>
public class AugmentingNetwork<T> extends Network<T, AugmentingEdge<T>> {

    private static final Logger LOG = Logger.getLogger(AugmentingNetwork.class.getName());

    public AugmentingNetwork() {
        super();
        LOG.setLevel(Level.OFF);
    }
    public static <T, E extends Edge<T>> AugmentingNetwork<T> of(Network<T, E> network, Map<Edge<T>, Double> flow) {
        GraphUtils.GraphMapping<T, E, T, AugmentingEdge<T>, AugmentingNetwork<T>> graphMapping = GraphUtils.mapGraph(
                network,
                Function.identity(),
                (edge, nodeMapping) -> {
                    double newWeight = edge.getWeight() - flow.get(edge);
                    return (newWeight > 0 ? new AugmentingEdge<T>(nodeMapping.get(edge.getFrom()), nodeMapping.get(edge.getTo()), newWeight, edge, false) : null);
                },
                AugmentingNetwork<T>::new,
                (newGraph, nodeMapping, edgeMapping) -> { //Map source and drain points
                    newGraph.setS(nodeMapping.get(network.getS()));
                    newGraph.setT(nodeMapping.get(network.getT()));
                });

        AugmentingNetwork<T> augmentingNetwork = graphMapping.getNewGraph();

        Collection<AugmentingEdge<T>> augNetNewEdges = new LinkedList<>();
        for (AugmentingEdge<T> edge : augmentingNetwork.getEdges()) {
            Double edgeFlow = flow.get(edge.getOriginalEdge());
            if (edgeFlow > 0) {
                augNetNewEdges.add(new AugmentingEdge<>(
                        edge.getTo(),
                        edge.getFrom(),
                        edgeFlow,
                        edge.getOriginalEdge(),
                        true));
            }
        }
        for (AugmentingEdge<T> edge : augNetNewEdges) {
            augmentingNetwork.addExistingEdge(edge);
        }
        return augmentingNetwork;
    }

    public void augmentAlongPath(Map<Edge<T>, Double> flow, Path<T> augmentingPath) {
        augmentAlongPath(flow, augmentingPath, augmentingPath.getMinWeight());
    }

    public void augmentAlongPath(Map<Edge<T>, Double> flow, Path<T> augmentingPath, double gamma) {

        LOG.info("Augmenting along Path: " + augmentingPath.toString() + " with g=" + gamma);
        augmentingPath.getEdges().forEach(e -> {
            AugmentingEdge<T> augEdge = (AugmentingEdge<T>) e;
            assert flow.containsKey(augEdge.getOriginalEdge()) : "Tried to augment non-flow-reg. Edge: " + augEdge.getOriginalEdge().toString() + " of AugEdge: " + augEdge.toString();
            Double newFlow = flow.compute(augEdge.getOriginalEdge(), (k, v) -> {
                return (augEdge.isBackFlow() ? v - gamma : v + gamma);
            });
            //Update the Augmentationnetwork according to the new flow assigned to augEdge.getOriginalEdge()
            if (!augEdge.update(newFlow)) {
                LOG.finest("Must remove path edge: " + augEdge);
                this.removeEdge(augEdge);
            }
            AugmentingEdge<T> counterEdge = (AugmentingEdge<T>) getEdge(augEdge.getTo(), augEdge.getFrom());
            if (counterEdge != null) {
                LOG.finest("Counter edge exists and is: " + counterEdge.toString());
                assert counterEdge.isBackFlow() != augEdge.isBackFlow() : "Counter edge is not backflow";
                //If counter Edge already is in Graph just update it and delete if no longer needed.
                if (!counterEdge.update(newFlow)) {
                    LOG.finest("Must remove counter edge");
                    this.removeEdge(counterEdge);
                } else {
                    LOG.finest("Updated counter edge: " + counterEdge.toString());
                }
            } else {
                LOG.finest("Counter edge does not exist");
                //Experimentally create counter Edge and only add it to Graph if required by flow.
                AugmentingEdge<T> newCounterEdge = new AugmentingEdge<>(augEdge.getTo(), augEdge.getFrom(), 0.0, augEdge.getOriginalEdge(), !augEdge.isBackFlow());
                if (newCounterEdge.update(newFlow)) {
                    LOG.finest("Initialized counter edge: " + newCounterEdge.toString());
                    this.addExistingEdge(newCounterEdge);
                } else {
                    LOG.finest("Did not add counter Edge");
                }
            }

        });
    }
}
