import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * MotorPH basic Payroll System
 *
 * This program reads employee details and attendance records from CSV files
 * and computes payroll from June to December 2024.
 *
 * The system supports two roles:
 * 1. Employee, who can view personal information.
 * 2. Payroll staff, who can process payroll for one employee or all employees.
 *
 * All data are read from CSV files and no rounding of values is applied.
 */
public class MotorPH {

    static final String EMPLOYEE_FILE = "resources/Employee Details.csv";
    static final String ATTENDANCE_FILE = "resources/Attendance Record.csv";
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    /**
     * This is the entry point of the program.
     * It validates the login credentials and opens the correct menu
     * based on the user role.
     */
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        // The program only accepts the predefined system password.
        if (!password.equals("12345")) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        // Only two usernames are allowed to access the system.
        if (!username.equals("employee") && !username.equals("payroll_staff")) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        // The program opens the appropriate menu depending on the user role.
        if (username.equals("employee")) {
            runEmployee(scanner);
        } else {
            runPayrollStaff(scanner);
        }
    }

    /**
     * Handles the employee menu.
     * It allows an employee to enter an employee number
     * and view the corresponding personal information.
     */
    static void runEmployee(Scanner scanner) {

        System.out.println("\n1. Enter your employee number");
        System.out.println("2. Exit the program");

        String choice = scanner.nextLine().trim();

        if (choice.equals("2")) return;

        if (!choice.equals("1")) {
            System.out.println("Invalid choice.");
            return;
        }

        System.out.print("\nEnter employee number: ");
        String empNumber = scanner.nextLine().trim();

        // The employee number is searched in the employee file.
        String[] employeeRow = findEmployee(empNumber);

        if (employeeRow == null) {
            System.out.println("Employee number does not exist.");
            return;
        }

        System.out.println("\n======================================");
        System.out.println("Employee Number: " + employeeRow[0].trim());
        System.out.println("Employee Name: " + employeeRow[2].trim() + " " + employeeRow[1].trim());
        System.out.println("Birthday: " + employeeRow[3].trim());
        System.out.println("======================================");
    }

    /**
     * Handles payroll staff operations.
     * Payroll staff can process payroll for one employee
     * or for all employees in the employee file.
     */
    static void runPayrollStaff(Scanner scanner) {

        System.out.println("\n1. Process Payroll");
        System.out.println("2. Exit the program");

        String choice = scanner.nextLine().trim();

        if (choice.equals("2")) return;

        if (!choice.equals("1")) {
            System.out.println("Invalid choice.");
            return;
        }

        System.out.println("\n1. One employee");
        System.out.println("2. All employees");
        System.out.println("3. Exit the program");

        String subChoice = scanner.nextLine().trim();

        if (subChoice.equals("3")) return;

        // Attendance records are loaded once to avoid repeatedly
        // opening the attendance file during payroll computation.
        String[][] attendanceRows = loadAttendance();

        if (attendanceRows == null) return;

        if (subChoice.equals("1")) {

            System.out.print("\nEnter the employee number: ");
            String empNumber = scanner.nextLine().trim();

            String[] employeeRow = findEmployee(empNumber);

            if (employeeRow == null) {
                System.out.println("Employee number does not exist.");
                return;
            }

            printEmployeePayroll(employeeRow, attendanceRows);

        } else if (subChoice.equals("2")) {

            try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {

                br.readLine();
                String line;

                while ((line = br.readLine()) != null) {

                    if (line.trim().isEmpty()) continue;

                    String[] employeeRow = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                    // Rows with incomplete employee data are skipped.
                    if (employeeRow.length < 4) continue;

                    printEmployeePayroll(employeeRow, attendanceRows);
                }

            } catch (Exception e) {
                System.out.println("Unable to read employee file.");
            }

        } else {
            System.out.println("Invalid choice.");
        }
    }

    /**
     * Searches the employee file
     * and returns the row that matches the given employee number.
     */
    static String[] findEmployee(String empNumber) {

        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {

            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                // Rows with incomplete employee data are skipped.
                if (data.length < 4) continue;

                if (data[0].trim().equals(empNumber)) {
                    return data;
                }
            }

        } catch (Exception e) {
            System.out.println("Unable to read employee file.");
        }

        return null;
    }

    /**
     * Extracts the hourly rate from the employee row.
     */
    static double parseHourlyRate(String[] row) {

        try {
            String rate = row[row.length - 1].trim();
            rate = rate.replace("\"", "").replace(",", "");
            return Double.parseDouble(rate);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Loads all attendance records from the CSV file
     * and stores them in memory so they can be reused
     * during payroll computation.
     */
    static String[][] loadAttendance() {

        int count = 0;

        // The first pass counts valid attendance rows
        // so the program can create a fixed-size array.
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {

            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] data = line.split(",");

                if (data.length >= 6) count++;
            }

        } catch (Exception e) {
            System.out.println("Unable to read attendance file.");
            return null;
        }

        String[][] rows = new String[count][];
        int index = 0;

        // The second pass stores valid attendance rows in memory
        // so they can be reused later in the program.
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {

            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] data = line.split(",");

                if (data.length >= 6) {
                    rows[index] = data;
                    index++;
                }
            }

        } catch (Exception e) {
            System.out.println("Unable to read attendance file.");
            return null;
        }

        return rows;
    }

    /**
     * Computes and displays payroll details
     * for a single employee from June to December.
     */
    static void printEmployeePayroll(String[] employeeRow, String[][] attendanceRows) {

        String empNumber = employeeRow[0].trim();

        System.out.println("\n======================================");
        System.out.println("Employee #: " + empNumber);
        System.out.println("Employee Name: " + employeeRow[2].trim() + " " + employeeRow[1].trim());
        System.out.println("Birthday: " + employeeRow[3].trim());
        System.out.println("======================================");

        double hourlyRate = parseHourlyRate(employeeRow);

        if (hourlyRate < 0) {
            System.out.println("Hourly rate not found.");
            return;
        }

        // Payroll is processed only for the required months of the project.
        for (int month = 6; month <= 12; month++) {

            int daysInMonth = YearMonth.of(2024, month).lengthOfMonth();

            // This section computes payroll for the first cutoff.
            double hoursFirst = getHours(attendanceRows, empNumber, month, 1, 15);
            double grossFirst = hoursFirst * hourlyRate;
            double netFirst = grossFirst;

            System.out.println("\nCutoff Date: " + monthName(month) + " 1 to 15");
            System.out.println("Total Hours Worked: " + hoursFirst);
            System.out.println("Gross Salary: " + grossFirst);
            System.out.println("Net Salary: " + netFirst);

            // This section computes payroll for the second cutoff.
            double hoursSecond = getHours(attendanceRows, empNumber, month, 16, daysInMonth);
            double grossSecond = hoursSecond * hourlyRate;

            System.out.println("\nCutoff Date: " + monthName(month) + " 16 to " + daysInMonth);
            System.out.println("Total Hours Worked: " + hoursSecond);
            System.out.println("Gross Salary: " + grossSecond);

            // Government deductions are based on the total monthly salary.
            double monthlyBasis = grossFirst + grossSecond;

            double sss = getSSS(monthlyBasis);
            double philHealth = getPhilHealth(monthlyBasis);
            double pagIbig = getPagIbig(monthlyBasis);

            // Tax is computed only after mandatory deductions are removed.
            double taxableIncome = monthlyBasis - (sss + philHealth + pagIbig);
            double withholdingTax = getWithholdingTax(taxableIncome);

            double totalDeductions = sss + philHealth + pagIbig + withholdingTax;

            // All deductions are applied during the second cutoff only.
            double netSecond = grossSecond - totalDeductions;

            System.out.println("Each Deduction:");
            System.out.println("    SSS: " + sss);
            System.out.println("    PhilHealth: " + philHealth);
            System.out.println("    Pag-IBIG: " + pagIbig);
            System.out.println("    Tax: " + withholdingTax);
            System.out.println("Total Deductions: " + totalDeductions);
            System.out.println("Net Salary: " + netSecond);
            System.out.println("---------------------------------------");
        }
    }

    /**
     * Filters attendance records and computes
     * the total credited working hours for the given cutoff period.
     */
    static double getHours(String[][] attendanceRows, String empNumber, int month, int startDay, int endDay) {

        double total = 0;

        for (String[] data : attendanceRows) {

            // Records that do not belong to the selected employee are ignored.
            if (!data[0].trim().equals(empNumber)) continue;

            try {
                String[] date = data[3].split("/");

                int m = Integer.parseInt(date[0]);
                int d = Integer.parseInt(date[1]);
                int y = Integer.parseInt(date[2]);

                // The record must match the required year, month, and cutoff range.
                if (y != 2024) continue;
                if (m != month) continue;
                if (d < startDay || d > endDay) continue;

                LocalTime login = LocalTime.parse(data[4].trim(), TIME_FORMAT);
                LocalTime logout = LocalTime.parse(data[5].trim(), TIME_FORMAT);

                total += computeHours(login, logout);

            } catch (Exception e) {
                System.err.println("Skipping malformed attendance row.");
            }
        }

        return total;
    }

    /**
     * Calculates the credited working hours
     * based on the attendance rules.
     */
    static double computeHours(LocalTime login, LocalTime logout) {

        // These values represent the official work schedule.
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(17, 0);
        LocalTime graceTime = LocalTime.of(8, 10);

        // Working time after 5:00 PM is not counted.
        if (logout.isAfter(endTime)) logout = endTime;

        // No hours are credited if logout happens before the shift starts.
        if (!logout.isAfter(startTime)) return 0;

        // A grace period is allowed until 8:10 AM before the employee is considered late.
        if (!login.isAfter(graceTime)) login = startTime;

        // Invalid or negative time ranges are ignored.
        if (!logout.isAfter(login)) return 0;

        long minutesWorked = Duration.between(login, logout).toMinutes();

        // One hour is deducted for lunch if the employee worked more than one hour.
        if (minutesWorked > 60) minutesWorked -= 60;
        else minutesWorked = 0;

        double hours = minutesWorked / 60.0;

        // The maximum credited working hours per day is eight hours.
        if (hours > 8.0) hours = 8.0;

        return hours;
    }

    /**
     * Computes SSS contribution based on salary brackets.
     * The numeric values in the arrays represent salary thresholds
     * and fixed contribution amounts based on the predefined SSS table used in this system for 2024.
     */
    static double getSSS(double salary) {

        double[] limit = {
                3250, 3749.99, 4249.99, 4749.99, 5249.99, 5749.99,
                6249.99, 6749.99, 7249.99, 7749.99, 8249.99, 8749.99,
                9249.99, 9749.99, 10249.99, 10749.99, 11249.99, 11749.99,
                12249.99, 12749.99, 13249.99, 13749.99, 14249.99, 14749.99,
                15249.99, 15749.99, 16249.99, 16749.99, 17249.99, 17749.99,
                18249.99, 18749.99, 19249.99, 19749.99, 20249.99, 20749.99,
                21249.99, 21749.99, 22249.99, 22749.99, 23249.99, 23749.99,
                24249.99, 24749.99
        };

        double[] contribution = {
                135, 157.5, 180, 202.5, 225, 247.5,
                270, 292.5, 315, 337.5, 360, 382.5,
                405, 427.5, 450, 472.5, 495, 517.5,
                540, 562.5, 585, 607.5, 630, 652.5,
                675, 697.5, 720, 742.5, 765, 787.5,
                810, 832.5, 855, 877.5, 900, 922.5,
                945, 967.5, 990, 1012.5, 1035, 1057.5,
                1080, 1102.5
        };

        // The salary is compared against each bracket to find the correct contribution.
        for (int i = 0; i < limit.length; i++) {
            if (salary <= limit[i]) return contribution[i];
        }

        // Maximum SSS contribution for salaries above the highest bracket.
        return 1125;
    }

    /**
     * Computes the PhilHealth employee contribution.
     * The premium is based on three percent of the salary base,
     * and the employee pays half of the total premium.
     */
    static double getPhilHealth(double salary) {

        double base = salary;

        // PhilHealth uses a minimum and maximum salary base.
        if (base < 10000) base = 10000;
        if (base > 60000) base = 60000;

        double premium = base * 0.03;

        return premium / 2;
    }

    /**
     * Computes the Pag-IBIG employee contribution.
     * The employee pays one percent or two percent depending on salary,
     * and the contribution is capped at one hundred pesos.
     */
    static double getPagIbig(double salary) {

        double rate = salary <= 1500 ? 0.01 : 0.02;

        double contribution = salary * rate;

        if (contribution > 100) contribution = 100;

        return contribution;
    }

    /**
     * Computes withholding tax using progressive tax brackets.
     * The numeric values (e.g., 20833, 33333) represent income thresholds,
     * while values like 0.20, 0.25, etc. represent the tax rates applied
     * to each income range.
     * These values are based on a predefined tax table used in this system for 2024.
     */
    static double getWithholdingTax(double income) {

        // Each income bracket applies a different tax rate.
        if (income <= 20833) return 0;
        if (income < 33333) return (income - 20833) * 0.20;
        if (income < 66667) return 2500 + (income - 33333) * 0.25;
        if (income < 166667) return 10833.33 + (income - 66667) * 0.30;
        if (income < 666667) return 40833.33 + (income - 166667) * 0.32;

        return 200833.33 + (income - 666667) * 0.35;
    }

    /**
     * Converts the month number into the month name
     * used in the payroll display.
     */
    static String monthName(int month) {

        switch (month) {
            case 6: return "June";
            case 7: return "July";
            case 8: return "August";
            case 9: return "September";
            case 10: return "October";
            case 11: return "November";
            case 12: return "December";
            default: return "Month";
        }
    }
}
