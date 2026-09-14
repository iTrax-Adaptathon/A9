public class Availability {

    String personName;
    int startHour;
    int endHour;
    int priority;

    public Availability(String personName, int startHour, int endHour, int priority) {
        this.personName = personName;
        this.startHour = startHour;
        this.endHour = endHour;
        this.priority = priority;
    }

    public void displayAvailability() {
        System.out.println(
                personName + ": "
                        + startHour + ":00 - "
                        + endHour + ":00"
                        + " | Priority: " + priority
        );
    }
}