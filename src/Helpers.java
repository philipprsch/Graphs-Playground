import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Helpers {

    public static void copyToClipboard(String text) {
        try {
            StringSelection selection = new StringSelection(text);
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(selection, null);
        } catch (Exception e) {
            System.out.println("Failed to copy to clipboard.");
        }
    }
    public static <A, B> Map<B, A> reverseMap(Map<A, B> originalMap) {
        Map<B, A> reversedMap = new HashMap<>();
        for (Map.Entry<A, B> entry : originalMap.entrySet()) {
            reversedMap.put(entry.getValue(), entry.getKey());
        }
        return reversedMap;
    }
    /**
     * Joins two maps with a common key type K into a new map mapping P -> D.
     *
     * @param kdMap map from K to D (original flow)
     * @param kpMap map from K to P (edges map from this to incCapNet Graph )
     * @return a new map from P to D, including only entries for which both maps have the same key
     */
    public static <K, D, P> Map<P, D> joinOnKey(
            Map<? super K, ? extends D> kdMap,
            Map<? extends K, ? extends P> kpMap) {

        return kpMap.entrySet().stream()
                .filter(e -> kdMap.containsKey(e.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getValue,             // P
                        e -> kdMap.get(e.getKey()),      // D
                        (d1, d2) -> d1
                ));
    }

    public static <T> List<LinkedList<T>> powerSet(LinkedList<T> Astar) {
        if (Astar.isEmpty()) return List.of(new LinkedList<>());
        LinkedList<T> A = new LinkedList<>(Astar);
        T a = A.pop();

        List<LinkedList<T>> powerSetOfA  = powerSet(A);

        return powerSetOfA.stream().flatMap(M -> {
            LinkedList<T> Mstar = new LinkedList<>(M); Mstar.add(a);
            return Stream.of(M, Mstar);
        }).collect(Collectors.toList());
    }
    public static <T> void printOverlapTable(Set<T> setA, Set<T> setB) {
        // Compute relationships
        Set<T> onlyA = new HashSet<>(setA);
        onlyA.removeAll(setB);

        Set<T> onlyB = new HashSet<>(setB);
        onlyB.removeAll(setA);

        Set<T> both = new HashSet<>(setA);
        both.retainAll(setB);

        // Determine max column width dynamically
        int maxWidth = 10; // minimum width
        for (T item : concat(onlyA, both, onlyB)) {
            maxWidth = Math.max(maxWidth, item.toString().length() + 2);
        }

        String format = "| %-" + maxWidth + "s | %-" + maxWidth + "s | %-" + maxWidth + "s |\n";
        String separator = "+"
                + "-".repeat(maxWidth + 2) + "+"
                + "-".repeat(maxWidth + 2) + "+"
                + "-".repeat(maxWidth + 2) + "+";

        System.out.println(separator);
        System.out.printf(format, "Only A", "Both", "Only B");
        System.out.println(separator);

        // Convert sets to sorted lists (optional)
        List<String> aList = toStringList(onlyA);
        List<String> bothList = toStringList(both);
        List<String> bList = toStringList(onlyB);

        Collections.sort(aList);
        Collections.sort(bothList);
        Collections.sort(bList);

        int rows = Math.max(aList.size(), Math.max(bothList.size(), bList.size()));
        for (int i = 0; i < rows; i++) {
            String a = i < aList.size() ? aList.get(i) : "";
            String c = i < bothList.size() ? bothList.get(i) : "";
            String b = i < bList.size() ? bList.get(i) : "";
            System.out.printf(format, a, c, b);
        }
        System.out.println(separator);
    }

    @SafeVarargs
    private static <T> List<T> concat(Set<T>... sets) {
        List<T> all = new ArrayList<>();
        for (Set<T> s : sets) all.addAll(s);
        return all;
    }

    private static <T> List<String> toStringList(Set<T> set) {
        List<String> list = new ArrayList<>();
        for (T item : set) list.add(item.toString());
        return list;
    }

    public static void traverseNonDecTuples(int n, int k, Function<ArrayList<Integer>, Integer> f) {
        if (n < 1 || k <= 0) return;
        traverseNonDecTuples(n, k,  new ArrayList<>(Collections.nCopies(n, 1)), n - 1, n -1, f);
    }
    public static void traverseNonDecTuples(int n, int k, ArrayList<Integer> current, int pivot, int subPivot, Function<ArrayList<Integer>, Integer> f) {
        f.apply(current);
        if (current.get(subPivot) == k) {
            if (pivot == subPivot) {
                if (pivot == 0) return;
                ArrayList<Integer> fused = new ArrayList<>(current.subList(0, pivot - 1));
                fused.addAll(Collections.nCopies(n - pivot + 1, current.get(pivot - 1) + 1));

                traverseNonDecTuples(n, k, fused, pivot - 1, n - 1, f);
            } else {
                subPivot--;
                int currentSubPivotEntry = current.get(subPivot);
                ArrayList<Integer> newCurrent = new ArrayList<>(current);
                newCurrent.set(subPivot, currentSubPivotEntry + 1);
                traverseNonDecTuples(n, k, newCurrent, pivot, subPivot, f);
            }
        } else {
            int currentSubPivotEntry = current.get(subPivot);
            ArrayList<Integer> newCurrent = new ArrayList<>(current);
            newCurrent.set(subPivot, currentSubPivotEntry + 1);
            traverseNonDecTuples(n, k, newCurrent, pivot, subPivot, f);
        }
    }
    public static void traverseNonDecTuplesNonRec(int n, int k, Function<ArrayList<Integer>, Integer> f) {
        //No tuples to traverse if tuple length (n) is <= 0 or there are no (positive natural) numbers to assign ([k] is empty)
        if (n < 1 || k <= 0) return;
        int pivot = n - 1;
        int subPivot = n - 1;
        ArrayList<Integer> current = new ArrayList<>(Collections.nCopies(n, 1));
        while (true) {
            f.apply(current);
            if (current.get(subPivot) == k) {
                if (pivot == subPivot) {
                    if (pivot == 0) break;
                    ArrayList<Integer> fused = new ArrayList<>(current.subList(0, pivot - 1));
                    fused.addAll(Collections.nCopies(n - pivot + 1, current.get(pivot - 1) + 1));

                    //traverseNonDecTuples(n, k, fused, pivot - 1, n - 1, f);
                    current = fused;
                    pivot--;
                    subPivot = n - 1;
                } else {
                    subPivot--;
                    int currentSubPivotEntry = current.get(subPivot);
                    ArrayList<Integer> newCurrent = new ArrayList<>(current);
                    newCurrent.set(subPivot, currentSubPivotEntry + 1);
                    //traverseNonDecTuples(n, k, newCurrent, pivot, subPivot, f);

                    current = newCurrent;
                }
            } else {
                int currentSubPivotEntry = current.get(subPivot);
                ArrayList<Integer> newCurrent = new ArrayList<>(current);
                newCurrent.set(subPivot, currentSubPivotEntry + 1);
                //traverseNonDecTuples(n, k, newCurrent, pivot, subPivot, f);
                current = newCurrent;
            }
        }
    }
    public static ArrayList<Integer> CknToDnk(ArrayList<Integer> c, int k) {
        ArrayList<Integer> res = new ArrayList<>(Collections.nCopies(k, 0));
        for (int f : c) {
            if (!(f >= 1 && f <= k)) throw new IllegalArgumentException("c entry out of bounds for " + k + " classes");
            res.set(f - 1, res.get(f - 1) + 1);
        }
        return res;
    }
    static String formatLimited(String text, int maxLen) {
        if (text.length() > maxLen) {
            // Leave room for the "..."
            return text.substring(0, Math.max(0, maxLen - 3)) + "...";
        }
        return text;
    }
    public static void main(String[] args) {
//        traverseNonDecTuplesNonRec(31, 82, (tuple) -> {
//            System.out.println(tuple);
//            return 0;
//        });
        //System.out.println(powerSet(new LinkedList<>(List.of(1, 2, 3))));
        //System.out.println(Arrays.toString(getSimilarExpLoadClassesBounds(0, 100, 3, 1.0)));

//        Map<Integer, Set<Integer>> map = distributeLoad(200, 12, jobID -> {
//            return (double) ((jobID + 1) * (100 + 65));
//        });
        //printProcessorToJobsMap(map, );

        //Test Double Formating

        String output = String.format("%5.2f", Double.parseDouble("1.1221119E10") * 100.0 / Double.parseDouble("1.21572676E11")) + "%";
        System.out.println(output);
    }
}
