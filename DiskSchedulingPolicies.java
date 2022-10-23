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
    private static void averageOfDistances(List<Integer> locations, int locationsRequestedCount)
    {
        if (locations.isEmpty()) {
            System.out.println("Average tracks traversed per request: 0");
            return;
        }
        int distancesSum = 0;
        for (int i = 1; i < locations.size(); ++i) {
            final int loc1 = locations.get(i - 1);
            final int loc2 = locations.get(i);
            final int distance = Math.abs(loc1 - loc2);
            distancesSum += distance;
        }
        System.out.printf("Sum of distances: %d\n", distancesSum);
        final double averageTracksTraversed = (double) distancesSum / locationsRequestedCount;
        System.out.printf("Average tracks traversed per request: %f\n", averageTracksTraversed);
    }

    private static List<Integer> fifoOrder(List<Integer> requests)
    {
        System.out.printf("FIFO order: %s\n", requests);
        return requests;
    }

    private static List<Integer> sstfOrder(List<Integer> requests)
    {
        final int startingLocation = requests.get(0);
        final List<Integer> sortedLocations = requests.stream()
                .sorted()
                .toList();

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

    /**
     * Following the course material, SCAN goes all the way to the last track and all the way to the first track.
     *
     * Because of this, the result is not exactly the same as in Stallings,
     * where it stops at the highest and lowest location requested.
     */
    private static List<Integer> scanOrder(List<Integer> requests, Integer numberOfTracks)
    {
        if (numberOfTracks == null) {
            throw new IllegalArgumentException();
        }
        final int startingLocation = requests.get(0);
        final List<Integer> sortedLocations = requests.stream()
                .sorted()
                .toList();
        int startingLocationIdx = Collections.binarySearch(sortedLocations, startingLocation);

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

    public static void averageTracksTraversed(String requests, int startingLocation, Policy policy)
    {
        averageTracksTraversed(requests, startingLocation, policy, null);
    }

    public static void averageTracksTraversed(String requests,
                                              int startingLocation,
                                              Policy policy,
                                              Integer numberOfTracks)
    {
        List<Integer> requestsList = Stream.concat(Stream.of(String.valueOf(startingLocation)),
                                                   Arrays.stream(requests.split(" ")))
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        int locationsRequestedCount = requestsList.size() - 1; // do not count starting location
        switch (policy) {
            case FIFO -> averageOfDistances(fifoOrder(requestsList), locationsRequestedCount);
            case SSTF -> averageOfDistances(sstfOrder(requestsList), locationsRequestedCount);
            case SCAN -> averageOfDistances(scanOrder(requestsList, numberOfTracks),
                                            locationsRequestedCount);
            default -> throw new IllegalArgumentException();
        }
    }

    public static void allPolicies(String locationsRequested,
                                   int startingLocation,
                                   int numberOfTracks)
    {
        System.out.printf("""
                Locations requested: %s
                Starting location: %s
                Number of tracks: %s\n
                """, locationsRequested, startingLocation, numberOfTracks);
        averageTracksTraversed(locationsRequested, startingLocation, Policy.FIFO);
        System.out.println();
        averageTracksTraversed(locationsRequested, startingLocation, Policy.SSTF);
        System.out.println();
        averageTracksTraversed(locationsRequested, startingLocation, Policy.SCAN, numberOfTracks);
        System.out.println();
    }

    public static void main(String[] args)
    {
        switch (args.length) {
            case 0 -> {
                System.out.println("No arguments passed, using default values.");
                final String locationsRequested = "55 58 39 18 90 160 150 38 184";
                final int startingLocation = 100;
                final int numberOfTracks = 200;
                allPolicies(locationsRequested, startingLocation, numberOfTracks);
            }
            case 3 -> {
                final String locationsRequested = args[0];
                final int startingLocation = Integer.parseInt(args[1]);
                final int numberOfTracks = Integer.parseInt(args[2]);
                allPolicies(locationsRequested, startingLocation, numberOfTracks);
            }
            default -> {
                System.err.println("Invalid arguments. "
                        + "Usage: disk-scheduling-policies <requests> <starting location> <number of tracks>");
                System.exit(1);
            }
        }
    }
}
