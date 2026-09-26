public class Patient extends User {
    // Encapsulated private fields
    private String birthdate;
    private String sex;
    private String contactNumber;
    private String address;
    private String discountId; 

    // Constructor
    public Patient(String id, String name, String username, String password, 
                   String birthdate, String sex, String contactNumber, 
                   String address, String discountId) {
        
        // Calls the constructor of the abstract User superclass
        super(id, name, username, password); 
        
        this.birthdate = birthdate;
        this.sex = sex;
        this.contactNumber = contactNumber;
        this.address = address;
        this.discountId = discountId;
    }

    // Example Getter and Setter (Encapsulation)
    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber; // Validation can be added here later
    }

    // Overriding the abstract method from the User class
    @Override
    public void showMenu() {
        System.out.println("\n--- Patient Menu ---");
        System.out.println("1. Update profile");
        System.out.println("2. View available doctors");
        System.out.println("3. Book appointment");
        System.out.println("4. Manage appointments");
        System.out.println("5. View medical records");
        System.out.println("6. View bills and admissions");
        System.out.println("7. Logout");
        System.out.print("Choose an option: ");
    }
}