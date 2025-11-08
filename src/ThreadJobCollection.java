import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class ThreadJobCollection<J extends Comparable<Integer>> { //J is Job Type Parameter, Comparable defines display order

    Map<Integer, List<J>> threadToJobsMap;
    Function<J, Double> getJobLoad;

    Map<Integer, String> threadToNameMap = new HashMap<>();
    Map<Integer, Long> threadToTimeMap = new HashMap<>();

    public void setThreadName(int threadID, String name) {
        threadToNameMap.put(threadID, name);
    }

    public void setThreadTime(int threadID, long time) {
        threadToTimeMap.put(threadID, time);
    }

    private final String description;

    private long totalTime;

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    //Derived per-thread properties (incl. Mappings)
    public double getThreadLoad(int threadID) {
        return threadToJobsMap.get(threadID).stream().mapToDouble(this.getJobLoad::apply).sum();
    }

    //Derived global properties
    public double getTotalLoad() {
        return allJobs.stream().mapToDouble(getJobLoad::apply).sum();
    }

    public long getExpectedThreadTime() {
        return threadToTimeMap.values().stream().mapToLong(e -> e).sum() / threadToTimeMap.size();
    }

    public long getThreadTimesVariance() {
        long EX = getExpectedThreadTime();
        return threadToTimeMap.values().stream().mapToLong(tt -> (EX - tt) * (EX - tt)).sum() / threadToTimeMap.size();
    }

    private final List<J> allJobs;

    public List<J> getAllJobs() {
        return allJobs;
    }
    //Internal derived properties


    public ThreadJobCollection(Map<Integer, Set<J>> thredToJobsMap, Function<J, Double> getJobLoad, String description) {
        this.threadToJobsMap = thredToJobsMap.entrySet().stream().map(e -> Map.entry(e.getKey(), e.getValue().stream().sorted().toList())).collect(Collectors.toMap(
                Map.Entry::getKey, // Key mapper
                Map.Entry::getValue, // Value mapper
                (existing, replacement) -> existing, // Merge function (if keys collide)
                HashMap::new // Supplier for the resulting map
        ));
        this.description = description;
        this.getJobLoad = getJobLoad;
        this.allJobs = thredToJobsMap.values().stream().flatMap(Collection::stream).sorted().toList();
    }

    public ThreadJobCollection(Map<Integer, Set<J>> thredToJobsMap, String description) {
        this(thredToJobsMap, jid -> 0.0, description);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean compact) {

        StringBuilder output = new StringBuilder();
        int maxJobsStringLength = threadToJobsMap.values().stream().map(String::valueOf).max(Comparator.comparingInt(String::length)).orElse("").length();
        final int maxJobsStringLen = (compact ? Math.min(50, maxJobsStringLength) : maxJobsStringLength);
        double totalLoad = getTotalLoad();
        output.append("Description: ").append(description)
                .append(" [Job Count: ").append(allJobs.size()).append("]\n")
                .append("Time: Total=").append(String.format("%6.1f", (totalTime / 1_000_000.0))).append("ms")
                //.append(" Total Load: ").append(totalLoad).append("\n")
                .append(" | Expected=").append(String.format("%6.1f", (getExpectedThreadTime() / 1_000_000.0))).append("ms")
                .append(" | Std.Deviation=").append(String.format("%6.1f", (Math.sqrt(getThreadTimesVariance()) / 1_000_000.0))).append("ms")
                .append("\n");

        for (Map.Entry<Integer, List<J>> entry : threadToJobsMap.entrySet()) {
            int threadID = entry.getKey();
            String jobsString = Helpers.formatLimited(threadToJobsMap.get(threadID).toString(), maxJobsStringLen);
            jobsString = String.format("%-" + maxJobsStringLen + "s", jobsString);

            output.append("Thread ").append(String.format("%2d", threadID))
                    .append(" -> ").append(jobsString)
                    .append(" Load: ")
                    //.append(getThreadLoad(threadID))
                    .append(String.format("%5.2f", getThreadLoad(threadID) * 100.0 / totalLoad)).append("%") //Load as percentage
                    .append(" T: ").append(threadToTimeMap.containsKey(threadID) ? String.format("%6.1f", (threadToTimeMap.get(threadID) / 1_000_000.0)) + "ms (" + (threadToTimeMap.get(threadID) * 100 / totalTime) + "%)" : "?")
                    .append("\n");
        }

        return output.toString();
    }
}
