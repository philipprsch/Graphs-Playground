import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.List;
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

    public static <T> List<LinkedList<T>> powerSet(LinkedList<T> Astar) {
        if (Astar.isEmpty()) return List.of(new LinkedList<T>());
        LinkedList<T> A = new LinkedList<>(Astar);
        T a = A.pop();

        List<LinkedList<T>> powerSetOfA  = powerSet(A);

        return powerSetOfA.stream().flatMap(M -> {
            LinkedList<T> Mstar = new LinkedList<>(M); Mstar.add(a);
            return Stream.of(M, Mstar);
        }).collect(Collectors.toList());
    }

    public static void main(String[] args) {
        System.out.println(powerSet(new LinkedList<>(List.of(1, 2, 3))));
    }
}
