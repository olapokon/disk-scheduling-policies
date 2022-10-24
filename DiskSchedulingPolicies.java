import java.util.*;
import java.util.stream.*;

public class DiskSchedulingPolicies {
    enum Policy {
        /**
         * First In First Out
         */
        FIFO,

        /**
         * Shortest Service Time First
         */
        SSTF,

        SCAN,

        CSCAN,

        LOOK,

        CLOOK,
    }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_PURPLE = "\u001B[35m";

    /**
     * @param locations
     *      the complete list of locations that will be visited, including the beginning position of the arm,
     *      they differ depending on the policy being used
     * @param locationsRequestedCount
     *      the number of locations requested,
     *      must not include the beginning position of the arm or any policy-specific locations,
     *      such as SCAN going all the way to the last track
     */
    private static void averageOfDistances(List<Integer> locations, int locationsRequestedCount)
    {
        if (locations.isEmpty()) {
            System.out.println("Average tracks traversed per request: 0");
            return;
        }
        int distancesSum = 0;
        // System.out.println("Total tracks\tLocations");
        // System.out.printf("%d\t\t%d\n", distancesSum, locations.get(0));
        for (int i = 1; i < locations.size(); ++i) {
            final int loc1 = locations.get(i - 1);
            final int loc2 = locations.get(i);
            final int distance = Math.abs(loc1 - loc2);
            distancesSum += distance;
            // System.out.printf("%d\t\t%d\n", distancesSum, loc2);
        }
        System.out.printf("Total track traversals: %d\n", distancesSum);
        final double averageTracksTraversed = (double) distancesSum / locationsRequestedCount;
        System.out.printf("Average tracks traversed per request: %s%f%s\n",
                          ANSI_PURPLE, averageTracksTraversed, ANSI_RESET);
    }

    private static List<Integer> fifoOrder(List<Integer> requests)
    {
        printPolicyOrder(Policy.FIFO, requests);
        return requests;
    }

    private static List<Integer> sstfOrder(List<Integer> requests)
    {
        final int startingLocation = requests.get(0);
        final List<Integer> sortedLocations = requests.stream().sorted().toList();

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

        printPolicyOrder(Policy.SSTF, sstfLocations);
        return sstfLocations;
    }

    /**
     * Returns the order for both SCAN and LOOK policies.
     */
    private static List<Integer> scanOrder(List<Integer> requests,
                                           Integer numberOfTracks,
                                           boolean lookOptimization)
    {
        if (numberOfTracks == null) {
            throw new IllegalArgumentException();
        }
        final int startingLocation = requests.get(0);
        final int lastTrack = numberOfTracks - 1;
        final List<Integer> sortedLocations = requests.stream().sorted().toList();
        final int startingLocationIdx = Collections.binarySearch(sortedLocations, startingLocation);

        final List<Integer> scanLocations = new ArrayList<>();
        // add all locations starting from the starting location to the highest requested location
        scanLocations.addAll(sortedLocations.subList(startingLocationIdx, sortedLocations.size()));
        if (!lookOptimization
                && scanLocations.get(scanLocations.size() - 1) != lastTrack) {
            // if the highest requested location is not the last track
            scanLocations.add(lastTrack);
        }
        // add all locations from the starting location to the lowest requested location
        for (int i = startingLocationIdx - 1; i >= 0; --i) {
            scanLocations.add(sortedLocations.get(i));
        }
        if (!lookOptimization && scanLocations.get(scanLocations.size() - 1) != 0) {
            // if the lowest requested location is not the first track
            scanLocations.add(0);
        }

        printPolicyOrder(lookOptimization ? Policy.LOOK : Policy.SCAN, scanLocations);
        return scanLocations;
    }

    /**
     * Returns the order for both C-SCAN and C-LOOK policies.
     */
    private static List<Integer> cScanOrder(List<Integer> requests,
                                            Integer numberOfTracks,
                                            boolean lookOptimization)
    {
        if (numberOfTracks == null) {
            throw new IllegalArgumentException();
        }
        final int startingLocation = requests.get(0);
        final int lastTrack = numberOfTracks - 1;
        final List<Integer> sortedLocations = requests.stream().sorted().toList();
        final int startingLocationIdx = Collections.binarySearch(sortedLocations, startingLocation);

        final List<Integer> scanLocations = new ArrayList<>();
        // add all locations starting from the starting location to the highest requested location
        scanLocations.addAll(sortedLocations.subList(startingLocationIdx, sortedLocations.size()));
        if (!lookOptimization
                && scanLocations.get(scanLocations.size() - 1) != lastTrack) {
            // if the highest requested location is not the last track
            scanLocations.add(lastTrack);
        }
        if (!lookOptimization && sortedLocations.get(0) != 0) {
            // if the lowest requested location is not the first track
            scanLocations.add(0);
        }
        // add all locations from the lowest requested location to the starting location
        scanLocations.addAll(sortedLocations.subList(0, startingLocationIdx));

        printPolicyOrder(lookOptimization ? Policy.CLOOK : Policy.CSCAN, scanLocations);
        return scanLocations;
    }

    private static void printPolicyOrder(Policy policy, List<Integer> order)
    {
        String policyName = switch (policy) {
            case CSCAN -> "C-SCAN";
            case CLOOK -> "C-LOOK";
            default -> policy.name();
        };
        policyName = ANSI_GREEN + policyName + ANSI_RESET;
        System.out.printf("%s order: %s\n", policyName, order);
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
        final List<Integer> requestsList = Stream
                .concat(Stream.of(String.valueOf(startingLocation)),
                        Arrays.stream(requests.split(" ")))
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .toList();
        final int locationsRequestedCount = requestsList.size() - 1; // do not count starting location
        switch (policy) {
            case FIFO -> averageOfDistances(fifoOrder(requestsList), locationsRequestedCount);
            case SSTF -> averageOfDistances(sstfOrder(requestsList), locationsRequestedCount);
            case SCAN -> averageOfDistances(scanOrder(requestsList, numberOfTracks, false),
                                            locationsRequestedCount);
            case CSCAN -> averageOfDistances(cScanOrder(requestsList, numberOfTracks, false),
                                             locationsRequestedCount);
            case LOOK -> averageOfDistances(scanOrder(requestsList, numberOfTracks, true),
                                            locationsRequestedCount);
            case CLOOK -> averageOfDistances(cScanOrder(requestsList, numberOfTracks, true),
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
        averageTracksTraversed(locationsRequested, startingLocation, Policy.CSCAN, numberOfTracks);
        System.out.println();
        averageTracksTraversed(locationsRequested, startingLocation, Policy.LOOK, numberOfTracks);
        System.out.println();
        averageTracksTraversed(locationsRequested, startingLocation, Policy.CLOOK, numberOfTracks);
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
