import java.util.*;
import java.util.stream.*;

public class DiskSchedulingPolicies {
    enum Policy {
        /**
         * First In First Out
         */
        FIFO,

        /**
         * Shortest Seek Time First
         */
        SSTF,

        SCAN,
    }

    /**
     * @param locations
     *      the complete list of locations that will be visited, including the beginning position of the arm,
     *      they differ depending on the policy being used
     * @param locationsRequestedCount
     *      the number of locations requested,
     *      does not include the beginning position of the arm or any policy-specific locations,
     *      such as SCAN going all the way to the last track
     */
    private static void averageOfDistances(List<Integer> locations, int locationsRequestedCount) {
        if (locations.isEmpty()) {
            System.out.println("Average tracks traversed per request: 0");
            return;
        }
        int distancesSum = 0;
        for (int i = 1; i < locations.size(); ++i) {
            final int loc1 = locations.get(i - 1);
            final int loc2 = locations.get(i);
            final int distance = Math.abs(loc1 - loc2);
            // System.out.printf("Loc 1: %s, Loc 2: %s, distance: %d\n", loc1, loc2, distance);
            distancesSum += distance;
        }
        System.out.printf("Sum of distances: %d\n", distancesSum);
        final double averageTracksTraversed = (double) distancesSum / locationsRequestedCount;
        System.out.printf("Average tracks traversed per request: %f\n", averageTracksTraversed);
    }

    private static List<Integer> fifoOrder(List<Integer> requests) {
        System.out.println("FIFO policy");
        System.out.printf("Starting location: %d\n", requests.get(0));
        System.out.printf("FIFO order: %s\n", requests);
        return requests;
    }

    private static List<Integer> sstfOrder(List<Integer> requests) {
        System.out.println("SSTF policy");
        final int startingLocation = requests.get(0);
        final List<Integer> sortedLocations = requests.stream()
                .sorted()
                .toList();
        System.out.printf("Starting location: %d\n", startingLocation);

        final List<Integer> sstfLocations = new ArrayList<>();
        sstfLocations.add(startingLocation);
        final int startingIdx = Collections.binarySearch(sortedLocations, startingLocation);
        int currentIdx = startingIdx;
        int highIdx = startingIdx + 1;
        int lowIdx = startingIdx - 1;
        while (highIdx < sortedLocations.size() || lowIdx >= 0) {
            int highDistance = highIdx < sortedLocations.size()
                    ? sortedLocations.get(highIdx) - sortedLocations.get(currentIdx)
                    : Integer.MAX_VALUE;
            int lowDistance = lowIdx >= 0
                    ? sortedLocations.get(currentIdx) - sortedLocations.get(lowIdx)
                    : Integer.MAX_VALUE;
            if (lowDistance < highDistance) {
                sstfLocations.add(sortedLocations.get(lowIdx));
                currentIdx = lowIdx;
                --lowIdx;
            } else {
                sstfLocations.add(sortedLocations.get(highIdx));
                currentIdx = highIdx;
                ++highIdx;
            }
        }

        System.out.printf("SSTF order: %s\n", sstfLocations);
        return sstfLocations;
    }

    private static List<Integer> scanOrder(List<Integer> requests, Integer numberOfTracks) {
        System.out.println("SCAN policy");
        if (numberOfTracks == null) {
            throw new IllegalArgumentException();
        }
        final int startingLocation = requests.get(0);
        final List<Integer> sortedLocations = requests.stream()
                .sorted()
                .toList();
        int startingLocationIdx = Collections.binarySearch(sortedLocations, startingLocation);
        System.out.printf("Starting location: %d\n", startingLocation);
        System.out.printf("Number of tracks: %d\n", numberOfTracks);

        final List<Integer> scanLocations = new ArrayList<>();
        // add all locations starting from the starting location to the highest requested location
        scanLocations.addAll(sortedLocations.subList(startingLocationIdx, sortedLocations.size()));
        // add the last track
        scanLocations.add(numberOfTracks - 1);
        // add all locations from the starting locations to the lowest requested location
        for (int i = startingLocationIdx - 1; i >= 0; --i) {
            scanLocations.add(sortedLocations.get(i));
        }
        // add the first track
        scanLocations.add(0);

        System.out.printf("SCAN order: %s\n", scanLocations);
        return scanLocations;
    }

    public static void averageTracksTraversed(String requests, Policy policy) {
        averageTracksTraversed(requests, policy, null);
    }

    public static void averageTracksTraversed(String requests, Policy policy, Integer numberOfTracks) {
        List<Integer> requestsList = Arrays.stream(requests.split(" "))
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        int locationsRequestedCount = requestsList.size() - 1; // do not count beginning location
        switch (policy) {
            case FIFO -> averageOfDistances(fifoOrder(requestsList), locationsRequestedCount);
            case SSTF -> averageOfDistances(sstfOrder(requestsList), locationsRequestedCount);
            case SCAN -> averageOfDistances(scanOrder(requestsList, numberOfTracks), locationsRequestedCount);
            default -> throw new IllegalArgumentException();
        }
    }

    public static void main(String[] args) {
        final String locationsRequested = "100 55 58 39 18 90 160 150 38 184";
        final int numberOfTracks = 200;
        System.out.printf("Locations requested, including starting location 100: %s\nNumber of tracks: %s\n\n",
                locationsRequested, numberOfTracks);
        averageTracksTraversed(locationsRequested, Policy.FIFO);
        System.out.println();
        averageTracksTraversed(locationsRequested, Policy.SSTF);
        System.out.println();
        averageTracksTraversed(locationsRequested, Policy.SCAN, numberOfTracks);
        System.out.println();
    }
}
