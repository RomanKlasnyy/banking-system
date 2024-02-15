import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class BankingSystem {
    static final long CREDIT_CARD_PREFIX = 4000000000000000L;
    static final int PIN_BOUND = 10000;

    public static void main(String[] args) {
        String url = "jdbc:sqlite:card.s3db";

        try (Connection conn = DriverManager.getConnection(url)) {
            createTable(conn);

            Random random = new Random();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("1. Create an account");
                System.out.println("2. Log into account");
                System.out.println("0. Exit");
                String choice = scanner.nextLine();

                if ("1".equals(choice)) {
                    long cardNumber = generateCardNumber(random);
                    int pin = random.nextInt(PIN_BOUND);
                    String paddedPin = String.format("%04d", pin);
                    if (createAccount(conn, cardNumber)) {
                        System.out.println("Your card has been created");
                        System.out.println("Your card number:");
                        System.out.println(cardNumber);
                        System.out.println("Your card PIN:");
                        System.out.println(paddedPin);
                    } else {
                        System.out.println("Error: Our program chose the existing credit card. Please, try again.");
                    }
                } else if ("2".equals(choice)) {
                    System.out.println("Enter your card number:");
                    long cardNumber = Long.parseLong(scanner.nextLine());
                    System.out.println("Enter your PIN:");
                    String pin = scanner.nextLine();
                    if (logIn(conn, cardNumber, pin)) {
                        if (performTransactions(conn, cardNumber, scanner)) {
                            break;
                        }
                    } else {
                        System.out.println("Wrong card number or PIN!");
                    }
                } else if ("0".equals(choice)) {
                    System.out.println("Bye!");
                    break;
                }
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void createTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS card (" +
                    "id INTEGER, " +
                    "number TEXT, " +
                    "pin TEXT, " +
                    "balance INTEGER DEFAULT 0);");
        }
    }

    public static long generateCardNumber(Random random) {
        long cardNumber = CREDIT_CARD_PREFIX + random.nextInt(900000000) + 100000000;
        int[] digits = String.valueOf(cardNumber).chars().map(Character::getNumericValue).toArray();
        int sum = 0;
        for (int i = 0; i < digits.length; i++) {
            int digit = digits[i];
            if ((digits.length - i) % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        int checksum = (sum % 10 == 0) ? 0 : (10 - sum % 10);
        return cardNumber * 10 + checksum;
    }

    public static boolean createAccount(Connection conn, long cardNumber) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT number FROM card WHERE number = " + cardNumber);
            return !rs.next();
        }
    }

    public static boolean logIn(Connection conn, long cardNumber, String pin) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT number, pin FROM card WHERE number = " + cardNumber);
            return rs.next() && rs.getString("pin").equals(pin);
        }
    }

    public static boolean performTransactions(Connection conn, long cardNumber, Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("1. Balance");
            System.out.println("2. Add income");
            System.out.println("3. Do transfer");
            System.out.println("4. Close account");
            System.out.println("5. Log out");
            System.out.println("0. Exit");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    displayBalance(conn, cardNumber);
                    break;
                case "2":
                    addIncome(conn, cardNumber, scanner);
                    break;
                case "3":
                    transferMoney(conn, cardNumber, scanner);
                    break;
                case "4":
                    closeAccount(conn, cardNumber);
                    return false;
                case "5":
                    System.out.println("You have successfully logged out!");
                    return true;
                case "0":
                    System.out.println("Bye!");
                    return true;
            }
        }
    }

    public static void displayBalance(Connection conn, long cardNumber) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT balance FROM card WHERE number = " + cardNumber);
            if (rs.next()) {
                System.out.println("Balance: " + rs.getInt("balance"));
            }
        }
    }

    public static void addIncome(Connection conn, long cardNumber, Scanner scanner) throws SQLException {
        System.out.println("Enter income:");
        int income = Integer.parseInt(scanner.nextLine());
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE card SET balance = balance + " + income + " WHERE number = " + cardNumber);
            System.out.println("Income was added!");
        }
    }

    public static void transferMoney(Connection conn, long cardNumber, Scanner scanner) throws SQLException {
        System.out.println("Transfer");
        System.out.println("Enter card number:");
        long targetCard = Long.parseLong(scanner.nextLine());
        if (targetCard == cardNumber) {
            System.out.println("You can't transfer money to the same account!");
            return;
        }
        if (!validateCard(targetCard)) {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
            return;
        }
        System.out.println("Enter how much money you want to transfer:");
        int amount = Integer.parseInt(scanner.nextLine());
        if (hasEnoughBalance(conn, cardNumber, amount)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE card SET balance = balance - " + amount + " WHERE number = " + cardNumber);
                stmt.executeUpdate("UPDATE card SET balance = balance + " + amount + " WHERE number = " + targetCard);
                System.out.println("Transfer completed!");
            }
        } else {
            System.out.println("Not enough money!");
        }
    }

    public static boolean validateCard(long cardNumber) {
        String cardStr = String.valueOf(cardNumber);
        int sum = 0;
        for (int i = 0; i < cardStr.length(); i++) {
            int digit = Character.getNumericValue(cardStr.charAt(i));
            if ((cardStr.length() - i) % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }

    public static boolean hasEnoughBalance(Connection conn, long cardNumber, int amount) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT balance FROM card WHERE number = " + cardNumber);
            return rs.next() && rs.getInt("balance") >= amount;
        }
    }

    public static void closeAccount(Connection conn, long cardNumber) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM card WHERE number = " + cardNumber);
            System.out.println("The account has been closed!");
        }
    }
}
