import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class MotorPH {

    static final String EMPLOYEE_FILE = "src/resources/Employee Details.csv";
    static final String ATTENDANCE_FILE = "src/resources/Attendance Record.csv";
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // Login credentials
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        // Validate login
        if (!password.equals("12345")) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        if (!username.equals("employee") && !username.equals("payroll_staff")) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        // Open the appropriate menu based on the role
        if (username.equals("employee")) {
            runEmployee(scanner);
        } else {
            runPayrollStaff(scanner);
        }
    }

    static void runEmployee(Scanner scanner) {

        // Employee menu
        System.out.println("\n1. Enter your employee number");
        System.out.println("2. Exit the program");

        String choice = scanner.nextLine().trim();

        if (choice.equals("2")) {
            return;
        }

        if (!choice.equals("1")) {
            System.out.println("Invalid choice.");
            return;
        }

        // Read and validate employee number
        System.out.print("\nEnter employee number: ");
        String employeeNumber = scanner.nextLine().trim();

        String[] employeeRow = findEmployee(employeeNumber);

        if (employeeRow == null) {
            System.out.println("Employee number does not exist.");
            return;
        }

        // Display employee information
        System.out.println("\n======================================");
        System.out.println("Employee Number: " + employeeRow[0].trim());
        System.out.println("Employee Name: " + employeeRow[2].trim() + " " + employeeRow[1].trim());
        System.out.println("Birthday: " + employeeRow[3].trim());
        System.out.println("======================================");
    }

    static void runPayrollStaff(Scanner scanner) {

        // Payroll staff main menu
        System.out.println("\n1. Process Payroll");
        System.out.println("2. Exit the program");

        String choice = scanner.nextLine().trim();

        if (choice.equals("2")) {
            return;
        }

        if (!choice.equals("1")) {
            System.out.println("Invalid choice.");
            return;
        }

        // Payroll processing options
        System.out.println("\n1. One employee");
        System.out.println("2. All employees");
        System.out.println("3. Exit the program");

        String subChoice = scanner.nextLine().trim();

        if (subChoice.equals("3")) {
            return;
        }

        if (subChoice.equals("1")) {

            // Read and validate employee number
            System.out.print("\nEnter the employee number: ");
            String employeeNumber = scanner.nextLine().trim();

            String[] employeeRow = findEmployee(employeeNumber);

            if (employeeRow == null) {
                System.out.println("Employee number does not exist.");
                return;
            }

            // Display employee information
            System.out.println("\n======================================");
            System.out.println("Employee #: " + employeeRow[0].trim());
            System.out.println("Employee Name: " + employeeRow[2].trim() + " " + employeeRow[1].trim());
            System.out.println("Birthday: " + employeeRow[3].trim());
            System.out.println("======================================");

            // Get hourly rate from employee record
            double hourlyRate = parseHourlyRate(employeeRow);

            if (hourlyRate < 0) {
                System.out.println("Hourly rate not found.");
                return;
            }

            // Process payroll from June to December
            for (int month = 6; month <= 12; month++) {

                int daysInMonth = YearMonth.of(2024, month).lengthOfMonth();

                // First cutoff: days 1 to 15
                double hoursFirstCutoff = getTotalHours(employeeNumber, month, 1, 15);
                double grossFirstCutoff = hoursFirstCutoff * hourlyRate;
                double netFirstCutoff = grossFirstCutoff;

                System.out.println("\nCutoff Date: " + monthName(month) + " 1 to 15");
                System.out.println("Total Hours Worked: " + hoursFirstCutoff);
                System.out.println("Gross Salary: " + grossFirstCutoff);
                System.out.println("Net Salary: " + netFirstCutoff);

                // Second cutoff: days 16 to end of month
                double hoursSecondCutoff = getTotalHours(employeeNumber, month, 16, daysInMonth);
                double grossSecondCutoff = hoursSecondCutoff * hourlyRate;

                System.out.println("\nCutoff Date: " + monthName(month) + " 16 to " + daysInMonth);
                System.out.println("Total Hours Worked: " + hoursSecondCutoff);
                System.out.println("Gross Salary: " + grossSecondCutoff);

                // Monthly deduction basis
                double monthlyNetBasis = grossFirstCutoff + grossSecondCutoff;

                // Mandatory deductions
                double sss = getSSS(monthlyNetBasis);
                double philHealth = getPhilHealth(monthlyNetBasis);
                double pagIbig = getPagIbig(monthlyNetBasis);

                // Tax computation
                double taxableIncome = monthlyNetBasis - (sss + philHealth + pagIbig);
                double withholdingTax = getWithholdingTax(taxableIncome);

                double totalDeductions = sss + philHealth + pagIbig + withholdingTax;
                double netSecondCutoff = grossSecondCutoff - totalDeductions;

                // Display second cutoff deductions and final net pay
                System.out.println("Each Deduction:");
                System.out.println("    SSS: " + sss);
                System.out.println("    PhilHealth: " + philHealth);
                System.out.println("    Pag-IBIG: " + pagIbig);
                System.out.println("    Tax: " + withholdingTax);
                System.out.println("Total Deductions: " + totalDeductions);
                System.out.println("Net Salary: " + netSecondCutoff);
                System.out.println("---------------------------------------");
            }

        } else if (subChoice.equals("2")) {

            // Process payroll for all employees
            printAllEmployeesPayroll();

        } else {
            System.out.println("Invalid choice.");
        }
    }

    static String[] findEmployee(String employeeNumberToFind) {

        // Search employee record in the employee file
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {

            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] data = line.split(",");

                if (data[0].trim().equals(employeeNumberToFind)) {
                    return data;
                }
            }

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        return null;
    }

    static double parseHourlyRate(String[] employeeRow) {

        // Read hourly rate from the last column
        try {
            String rateString = employeeRow[employeeRow.length - 1].trim();
            rateString = rateString.replace("\"", "").replace(",", "");
            return Double.parseDouble(rateString);

        } catch (Exception e) {
            return -1;
        }
    }

    static void printAllEmployeesPayroll() {

        // Read all employees and compute payroll for each one
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {

            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] employeeRow = line.split(",");

                String employeeNumber = employeeRow[0].trim();
                String lastName = employeeRow[1].trim();
                String firstName = employeeRow[2].trim();
                String birthday = employeeRow[3].trim();

                System.out.println("======================================");
                System.out.println("Employee #: " + employeeNumber);
                System.out.println("Employee Name: " + firstName + " " + lastName);
                System.out.println("Birthday: " + birthday);
                System.out.println("======================================");

                double hourlyRate = parseHourlyRate(employeeRow);

                if (hourlyRate < 0) {
                    System.out.println("Hourly rate not found.");
                    continue;
                }

                // Process payroll from June to December
                for (int month = 6; month <= 12; month++) {

                    int daysInMonth = YearMonth.of(2024, month).lengthOfMonth();

                    // First cutoff
                    double hoursFirstCutoff = getTotalHours(employeeNumber, month, 1, 15);
                    double grossFirstCutoff = hoursFirstCutoff * hourlyRate;
                    double netFirstCutoff = grossFirstCutoff;

                    System.out.println("\nCutoff Date: " + monthName(month) + " 1 to 15");
                    System.out.println("Total Hours Worked: " + hoursFirstCutoff);
                    System.out.println("Gross Salary: " + grossFirstCutoff);
                    System.out.println("Net Salary: " + netFirstCutoff);

                    // Second cutoff
                    double hoursSecondCutoff = getTotalHours(employeeNumber, month, 16, daysInMonth);
                    double grossSecondCutoff = hoursSecondCutoff * hourlyRate;

                    System.out.println("\nCutoff Date: " + monthName(month) + " 16 to " + daysInMonth);
                    System.out.println("Total Hours Worked: " + hoursSecondCutoff);
                    System.out.println("Gross Salary: " + grossSecondCutoff);

                    // Monthly deduction basis
                    double monthlyNetBasis = grossFirstCutoff + grossSecondCutoff;

                    double sss = getSSS(monthlyNetBasis);
                    double philHealth = getPhilHealth(monthlyNetBasis);
                    double pagIbig = getPagIbig(monthlyNetBasis);

                    double taxableIncome = monthlyNetBasis - (sss + philHealth + pagIbig);
                    double withholdingTax = getWithholdingTax(taxableIncome);

                    double totalDeductions = sss + philHealth + pagIbig + withholdingTax;
                    double netSecondCutoff = grossSecondCutoff - totalDeductions;

                    System.out.println("Each Deduction:");
                    System.out.println("    SSS: " + sss);
                    System.out.println("    PhilHealth: " + philHealth);
                    System.out.println("    Pag-IBIG: " + pagIbig);
                    System.out.println("    Tax: " + withholdingTax);
                    System.out.println("Total Deductions: " + totalDeductions);
                    System.out.println("Net Salary: " + netSecondCutoff);
                    System.out.println("\n--------------------------------------");
                }
            }

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    static double getTotalHours(String employeeNumber, int month, int dayStart, int dayEnd) {

        double totalHours = 0;

        // Read attendance records and sum hours within the selected cutoff
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {

            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) continue;

                String[] data = line.split(",");

                if (!data[0].trim().equals(employeeNumber)) continue;

                String[] dateParts = data[3].trim().split("/");
                int recordMonth = Integer.parseInt(dateParts[0]);
                int recordDay = Integer.parseInt(dateParts[1]);
                int recordYear = Integer.parseInt(dateParts[2]);

                if (recordYear != 2024) continue;
                if (recordMonth != month) continue;
                if (recordDay < dayStart || recordDay > dayEnd) continue;

                LocalTime login = LocalTime.parse(data[4].trim(), TIME_FORMAT);
                LocalTime logout = LocalTime.parse(data[5].trim(), TIME_FORMAT);

                totalHours += computeHours(login, logout);
            }

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        return totalHours;
    }

    static double computeHours(LocalTime login, LocalTime logout) {

        // Time rules for credited working hours
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(17, 0);
        LocalTime graceTime = LocalTime.of(8, 10);

        if (logout.isAfter(endTime)) logout = endTime;
        if (!logout.isAfter(startTime)) return 0;
        if (!login.isAfter(graceTime)) login = startTime;
        if (!logout.isAfter(login)) return 0;

        long minutesWorked = Duration.between(login, logout).toMinutes();

        // Remove one hour lunch break
        if (minutesWorked > 60) minutesWorked -= 60;
        else minutesWorked = 0;

        double hours = minutesWorked / 60.0;

        if (hours > 8.0) hours = 8.0;

        return hours;
    }

    // Deduction functions

    static double getSSS(double monthlyNetBasis) {

        // SSS contribution based on salary bracket
        if (monthlyNetBasis < 3250.0) return 135.0;
        if (monthlyNetBasis <= 3749.99) return 157.5;
        if (monthlyNetBasis <= 4249.99) return 180.0;
        if (monthlyNetBasis <= 4749.99) return 202.5;
        if (monthlyNetBasis <= 5249.99) return 225.0;
        if (monthlyNetBasis <= 5749.99) return 247.5;
        if (monthlyNetBasis <= 6249.99) return 270.0;
        if (monthlyNetBasis <= 6749.99) return 292.5;
        if (monthlyNetBasis <= 7249.99) return 315.0;
        if (monthlyNetBasis <= 7749.99) return 337.5;
        if (monthlyNetBasis <= 8249.99) return 360.0;
        if (monthlyNetBasis <= 8749.99) return 382.5;
        if (monthlyNetBasis <= 9249.99) return 405.0;
        if (monthlyNetBasis <= 9749.99) return 427.5;
        if (monthlyNetBasis <= 10249.99) return 450.0;
        if (monthlyNetBasis <= 10749.99) return 472.5;
        if (monthlyNetBasis <= 11249.99) return 495.0;
        if (monthlyNetBasis <= 11749.99) return 517.5;
        if (monthlyNetBasis <= 12249.99) return 540.0;
        if (monthlyNetBasis <= 12749.99) return 562.5;
        if (monthlyNetBasis <= 13249.99) return 585.0;
        if (monthlyNetBasis <= 13749.99) return 607.5;
        if (monthlyNetBasis <= 14249.99) return 630.0;
        if (monthlyNetBasis <= 14749.99) return 652.5;
        if (monthlyNetBasis <= 15249.99) return 675.0;
        if (monthlyNetBasis <= 15749.99) return 697.5;
        if (monthlyNetBasis <= 16249.99) return 720.0;
        if (monthlyNetBasis <= 16749.99) return 742.5;
        if (monthlyNetBasis <= 17249.99) return 765.0;
        if (monthlyNetBasis <= 17749.99) return 787.5;
        if (monthlyNetBasis <= 18249.99) return 810.0;
        if (monthlyNetBasis <= 18749.99) return 832.5;
        if (monthlyNetBasis <= 19249.99) return 855.0;
        if (monthlyNetBasis <= 19749.99) return 877.5;
        if (monthlyNetBasis <= 20249.99) return 900.0;
        if (monthlyNetBasis <= 20749.99) return 922.5;
        if (monthlyNetBasis <= 21249.99) return 945.0;
        if (monthlyNetBasis <= 21749.99) return 967.5;
        if (monthlyNetBasis <= 22249.99) return 990.0;
        if (monthlyNetBasis <= 22749.99) return 1012.5;
        if (monthlyNetBasis <= 23249.99) return 1035.0;
        if (monthlyNetBasis <= 23749.99) return 1057.5;
        if (monthlyNetBasis <= 24249.99) return 1080.0;
        if (monthlyNetBasis <= 24749.99) return 1102.5;

        return 1125.0;
    }

    static double getPhilHealth(double monthlyNetBasis) {

        // PhilHealth employee share
        double base = monthlyNetBasis;
        if (base < 10000.0) base = 10000.0;
        if (base > 60000.0) base = 60000.0;

        double premium = base * 0.03;
        return premium / 2.0;
    }

    static double getPagIbig(double monthlyNetBasis) {

        // Pag-IBIG contribution with salary threshold and cap
        double rate;
        if (monthlyNetBasis <= 1500.0) rate = 0.01;
        else rate = 0.02;

        double contribution = monthlyNetBasis * rate;

        if (contribution > 100.0) contribution = 100.0;

        return contribution;
    }

    static double getWithholdingTax(double taxableIncome) {

        // Withholding tax based on monthly tax brackets
        if (taxableIncome <= 20833.0) return 0.0;
        if (taxableIncome < 33333.0) return (taxableIncome - 20833.0) * 0.20;
        if (taxableIncome < 66667.0) return 2500.0 + (taxableIncome - 33333.0) * 0.25;
        if (taxableIncome < 166667.0) return 10833.33 + (taxableIncome - 66667.0) * 0.30;
        if (taxableIncome < 666667.0) return 40833.33 + (taxableIncome - 166667.0) * 0.32;

        return 200833.33 + (taxableIncome - 666667.0) * 0.35;
    }

    static String monthName(int month) {

        // Convert month number to month name for display
        switch (month) {
            case 6: return "June";
            case 7: return "July";
            case 8: return "August";
            case 9: return "September";
            case 10: return "October";
            case 11: return "November";
            case 12: return "December";
            default: return "Month " + month;
        }
    }
}