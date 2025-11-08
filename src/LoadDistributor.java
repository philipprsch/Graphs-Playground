import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public interface LoadDistributor<J extends Comparable<Integer>> {

    public Map<Integer, Set<J>> apply(List<J> jobList, int p, Function<Integer, Double> getJobLoad);

    public default Map<Integer, Set<J>> apply(List<J> jobList, int p) {
        return apply(jobList, p, e -> 1.0);
    }

    public static LoadDistributor<Integer> moduloBased() {
        return new LoadDistributor<Integer>() {
            @Override
            public Map<Integer, Set<Integer>> apply(List<Integer> jobList, int p, Function<Integer, Double> getJobLoad) {
                int jobs = jobList.size();
                Map<Integer, Set<Integer>> processorToJobsMap = new HashMap<>();
                for (int jc = 0; jc < jobs; jc++) {
                    int key = jc % p;
                    processorToJobsMap.computeIfAbsent(key, e -> new HashSet<>());
                    processorToJobsMap.get(key).add(jobList.get(jc));
                }
                return processorToJobsMap;
            }

            @Override
            public String toString() {
                return "Load Interval";
            };
        };


    }

    public static LoadDistributor<Integer> loadIntervalBased() {
        return new LoadDistributor<Integer>() {
            @Override
            public Map<Integer, Set<Integer>> apply(List<Integer> jobList, int p, Function<Integer, Double> getJobLoad) {
                int jobs = jobList.size();
                Map<Integer, Set<Integer>> processorToJobsMap = new HashMap<>();
                double totalLoad = IntStream.range(0, jobs).mapToDouble(getJobLoad::apply).sum();
                double avgLoad = totalLoad / p;
                System.out.println("Distributing " + jobs + " Jobs [totalLoad=" + totalLoad +"] across " + p + " processors [avgLoad=" + avgLoad + "].");
                int currentJobID = jobs - 1;
                int pid = 0;
                while (pid < p) {
                    if (currentJobID < 0) break; //All jobs have been assigned
                    double currentPLoad = 0.0;
                    processorToJobsMap.computeIfAbsent(pid, e -> new HashSet<Integer>());
                    while (currentPLoad < avgLoad && currentJobID >= 0) {
                        processorToJobsMap.get(pid).add(jobList.get(currentJobID));
                        double currentJobLoad = getJobLoad.apply(currentJobID);
                        currentPLoad += currentJobLoad;
                        currentJobID--;
                    }
                    pid++;
                }
                while (currentJobID >= 0) { //Ensure that all jobs are assigned
                    processorToJobsMap.get(p - 1).add(jobList.get(currentJobID));
                    currentJobID--;
                }
                return processorToJobsMap;
            }

            @Override
            public String toString() {
                return "Interleaving";
            }
        };
    }

}
