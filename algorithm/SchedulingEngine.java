import java.util.ArrayList;
import java.util.Scanner;

public class SchedulingEngine {

    // Find common availability of all participants
    public static int[] findCommonAvailability(
            ArrayList<Availability> availabilities) {

        int commonStart = availabilities.get(0).startHour;
        int commonEnd = availabilities.get(0).endHour;

        for (int i = 1; i < availabilities.size(); i++) {

            Availability current = availabilities.get(i);

            commonStart = Math.max(
                    commonStart,
                    current.startHour
            );

            commonEnd = Math.min(
                    commonEnd,
                    current.endHour
            );
        }

        return new int[] {
                commonStart,
                commonEnd
        };
    }


    // Calculate score for a time slot
    public static int calculateScore(
            int startHour,
            ArrayList<Availability> availabilities) {

        int totalPriority = 0;

        for (Availability person : availabilities) {
            totalPriority += person.priority;
        }

        // Earlier slots get a higher score
        int score = (24 - startHour) * totalPriority;

        return score;
    }


    // Find possible slots and select the best one
    public static void findBestSlots(
            int commonStart,
            int commonEnd,
            int meetingDuration,
            int buffer,
            ArrayList<Availability> availabilities) {

        int totalTime = meetingDuration + buffer;

        boolean found = false;

        int bestStart = -1;
        int bestScore = -1;

        System.out.println("\nPossible Meeting Slots:");

        for (int start = commonStart;
             start + totalTime <= commonEnd;
             start++) {

            int meetingEnd = start + meetingDuration;

            int bufferEnd = meetingEnd + buffer;

            int score = calculateScore(
                    start,
                    availabilities
            );

            System.out.println(
                    start + ":00 - "
                            + meetingEnd + ":00"
                            + " | Buffer until "
                            + bufferEnd + ":00"
                            + " | Score: "
                            + score
            );

            // Store the highest-scoring slot
            if (score > bestScore) {
                bestScore = score;
                bestStart = start;
            }

            found = true;
        }


        // No suitable slot
        if (!found) {

            System.out.println("\nCONFLICT!");

            System.out.println(
                    "No suitable common slot is available."
            );

            System.out.println(
                    "Try reducing the meeting duration or buffer."
            );

            return;
        }


        // Display best slot
        int bestEnd = bestStart + meetingDuration;

        System.out.println("\nBEST SLOT:");

        System.out.println(
                bestStart + ":00 - "
                        + bestEnd + ":00"
        );

        System.out.println(
                "Score: " + bestScore
        );


        // Display alternative slots
        System.out.println("\nAlternative slots:");

        int alternativesShown = 0;

        for (int start = commonStart;
             start + totalTime <= commonEnd;
             start++) {

            if (start == bestStart) {
                continue;
            }

            int end = start + meetingDuration;

            System.out.println(
                    start + ":00 - "
                            + end + ":00"
            );

            alternativesShown++;

            if (alternativesShown == 3) {
                break;
            }
        }
    }


    // Main program
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        ArrayList<Availability> availabilities =
                new ArrayList<>();


        System.out.println("================================");
        System.out.println("        SYNCSLOT ENGINE");
        System.out.println("================================");


        // Number of participants
        System.out.print(
                "\nEnter number of participants: "
        );

        int numberOfPeople = scanner.nextInt();


        // Get participant data
        for (int i = 0;
             i < numberOfPeople;
             i++) {

            System.out.println(
                    "\nParticipant " + (i + 1)
            );

            System.out.print(
                    "Enter person name: "
            );

            String name = scanner.next();


            System.out.print(
                    "Enter available start hour: "
            );

            int start = scanner.nextInt();


            System.out.print(
                    "Enter available end hour: "
            );

            int end = scanner.nextInt();


            System.out.print(
                    "Enter priority (1=Low, 2=Medium, 3=High): "
            );

            int priority = scanner.nextInt();


            availabilities.add(
                    new Availability(
                            name,
                            start,
                            end,
                            priority
                    )
            );
        }


        // Meeting details
        System.out.print(
                "\nEnter meeting duration (hours): "
        );

        int meetingDuration = scanner.nextInt();


        System.out.print(
                "Enter buffer time (hours): "
        );

        int buffer = scanner.nextInt();


        // Display participant information
        System.out.println(
                "\n================================"
        );

        System.out.println(
                "PARTICIPANT AVAILABILITY"
        );

        System.out.println(
                "================================"
        );


        for (Availability availability :
                availabilities) {

            availability.displayAvailability();
        }


        // Find common availability
        int[] common =
                findCommonAvailability(
                        availabilities
                );

        int commonStart = common[0];

        int commonEnd = common[1];


        System.out.println(
                "\nCommon availability: "
                        + commonStart + ":00 - "
                        + commonEnd + ":00"
        );


        // Find best meeting slots
        findBestSlots(
                commonStart,
                commonEnd,
                meetingDuration,
                buffer,
                availabilities
        );


        scanner.close();
    }
}