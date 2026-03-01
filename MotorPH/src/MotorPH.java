import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/**
 * MotorPH Basic Payroll System
 *
 * This program reads employee details and attendance records
 * from CSV files and computes payroll from June to December 2024.
 *
 * It supports two roles:
 * - Employee: View personal information.
 * - Payroll Staff: Process payroll for one or all employees.
 *
 * All data are read from CSV files.
 * No rounding of values is applied.
 */

public class MotorPH {

    static final String EMPLOYEE_FILE = "Employee Details.csv";
    static final String ATTENDANCE_FILE = "Attendance Record.csv";
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        // LOGIN INPUTS
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter password: ");
        String password = scanner.nextLine().trim();

        // LOGIN CHECK: password must match exactly
        if (!password.equals("12345")) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        // LOGIN CHECK: only 2 valid usernames
        if (!username.equals("employee") && !username.equals("payroll_staff")) {
            System.out.println("Incorrect username and/or password.");
            return;
        }

        // ROUTE USER BASED ON ROLE
        if (username.equals("employee")) {
            runEmployee(scanner);
        } else {
            runPayrollStaff(scanner);
        }
    }

    static void runEmployee(Scanner scanner) {

        // EMPLOYEE MENU OPTIONS
        System.out.println("\n1. Enter your employee number");
        System.out.println("2. Exit the program");

        // READ MENU CHOICE AS STRING TO AVOID InputMismatchException
        String choice = scanner.nextLine().trim();

        // EXIT IF USER SELECTS 2
        if (choice.equals("2")) {
            System.out.println("Program ended.");
            return;
        }

        // VALIDATE MENU CHOICE
        if (!choice.equals("1")) {
            System.out.println("Invalid choice.");
            return;
        }

        // GET EMPLOYEE NUMBER
        System.out.print("\nEnter employee number: ");
        String employeeNumber = scanner.nextLine().trim();

        // SEARCH EMPLOYEE DETAILS FROM CSV
        String[] employeeRow = findEmployee(employeeNumber);

        // HANDLE NOT FOUND EMPLOYEE NUMBER
        if (employeeRow == null) {
            System.out.println("Employee number does not exist.");
            return;
        }

        // DISPLAY EMPLOYEE DETAILS (FROM EMPLOYEE FILE)
        System.out.println("\n======================================");
        System.out.println("Employee Number: " + employeeRow[0].trim());
        System.out.println("Employee Name: " + employeeRow[2].trim() + " " + employeeRow[1].trim());
        System.out.println("Birthday: " + employeeRow[3].trim());
        System.out.println("======================================");
    }

    static void runPayrollStaff(Scanner scanner) {

        // PAYROLL STAFF MENU OPTIONS
        System.out.println("\n1. Process Payroll");
        System.out.println("2. Exit the program");

        //READ MENU CHOICE
        String choice = scanner.nextLine().trim();

        // EXIT IF USER SELECTS 2
        if (choice.equals("2")) {
            System.out.println("Program ended.");
            return;
        }

        // VALIDATE MENU CHOICE
        if (!choice.equals("1")) {
            System.out.println("Invalid choice.");
            return;
        }

        // SUB MENU OPTIONS
        System.out.println("\n1. One employee");
        System.out.println("2. All employees");
        System.out.println("3. Exit the program");

        //READ SUB MENU CHOICE
        String subChoice = scanner.nextLine().trim();

        // EXIT IF USER SELECTS 3
        if (subChoice.equals("3")) {
            System.out.println("Program ended.");
            return;
        }

        // SUBCHOICE 1: ONE EMPLOYEE
        if (subChoice.equals("1")) {

            // GET EMPLOYEE NUMBER
            System.out.print("\nEnter employee number: ");
            String employeeNumber = scanner.nextLine().trim();

            // SEARCH EMPLOYEE DETAILS FROM CSV
            String[] employeeRow = findEmployee(employeeNumber);

            // HANDLE NOT FOUND EMPLOYEE NUMBER
            if (employeeRow == null) {
                System.out.println("Employee number does not exist.");
                return;
            }

            //DISPLAY EMPLOYEE DETAILS (FROM EMPLOYEE FILE
            System.out.println("\n======================================");
            System.out.println("Employee #: " + employeeRow[0].trim());
            System.out.println("Employee Name: " + employeeRow[2].trim() + " " + employeeRow[1].trim());
            System.out.println("Birthday: " + employeeRow[3].trim());
            System.out.println("======================================");

            //GET HOURLY RATE FROM EMPLOYEE ROW (LAST COLUMN)
            double hourlyRate = parseHourlyRate(employeeRow);

            //HANDLE INVALID / MISSING HOURLY RATE
            if (hourlyRate < 0) {
                System.out.println("Hourly rate not found.");
                return;
            }

            // LOOP REQUIRED MONTHS: JUNE (6) TO DECEMBER (12)
            for (int month = 6; month <= 12; month++) {

                // GET LAST DAY OF MONTH (USED FOR 2ND CUTOFF RANGE)
                int daysInMonth = YearMonth.of(2024, month).lengthOfMonth();

                // FIRST CUTOFF COMPUTATION 1–15
                // total credited hours from attendance records (day 1 to 15)
                double hoursFirstCutoff = getTotalHours(employeeNumber, month, 1, 15);

                // gross = total hours × hourly rate
                double grossFirstCutoff = hoursFirstCutoff * hourlyRate;

                // late deduction computed from login time (after 8:10 grace)
                double lateDedFirstCutoff = getLateDeduction(employeeNumber, month, 1, 15, hourlyRate);

                // net for first cutoff = gross minus late deduction only
                double netFirstCutoff = grossFirstCutoff - lateDedFirstCutoff;

                //DISPLAY FIRST CUTOFF OUTPUT
                System.out.println("\nCutoff Date: " + monthName(month) + " 1 to 15");
                System.out.println("Total Hours Worked: " + hoursFirstCutoff);
                System.out.println("Gross Salary: " + grossFirstCutoff);
                System.out.println("Net Salary: " + netFirstCutoff);

                // SECOND CUTOFF COMPUTATION 16–EOM
                // total credited hours from attendance records (day 16 to end of month)
                double hoursSecondCutoff = getTotalHours(employeeNumber, month, 16, daysInMonth);

                // gross = total hours × hourly rate
                double grossSecondCutoff = hoursSecondCutoff * hourlyRate;

                // late deduction computed from login time (after 8:10 grace)
                double lateDedSecondCutoff = getLateDeduction(employeeNumber, month, 16, daysInMonth, hourlyRate);

                //DISPLAY SECOND CUTOFF BASIC OUTPUT (BEFORE GOV DEDUCTIONS)
                System.out.println("\nCutoff Date: " + monthName(month) + " 16 to " + daysInMonth);
                System.out.println("Total Hours Worked: " + hoursSecondCutoff);
                System.out.println("Gross Salary: " + grossSecondCutoff);

                // second cutoff net before gov deductions = gross minus late deduction only
                double netSecondBeforeGov = grossSecondCutoff - lateDedSecondCutoff;

                // GOV DEDUCTION MONTHLY BASIS
                //add 1st cutoff net + 2nd cutoff net first before computing gov deductions
                double monthlyNetBasis = netFirstCutoff + netSecondBeforeGov;

                // MANDATORY DEDUCTIONS
                // SSS based on bracket table
                double sss = getSSS(monthlyNetBasis);

                // PhilHealth employee share (3% premium / 2, capped base)
                double philHealth = getPhilHealth(monthlyNetBasis);

                // Pag-IBIG employee share (rate + cap)
                double pagIbig = getPagIbig(monthlyNetBasis);

                // taxable income = monthly basis minus mandatory contributions
                double taxableIncome = monthlyNetBasis - (sss + philHealth + pagIbig);

                // withholding tax computed from taxable income using bracket logic
                double withholdingTax = getWithholdingTax(taxableIncome);

                // total of deductions
                double totalDeductions = sss + philHealth + pagIbig + withholdingTax;

                // final net salary (released on 2nd cutoff) = netSecondBeforeGov minus total deductions
                double netSecondCutoff = netSecondBeforeGov - totalDeductions;

                // DISPLAY DEDUCTIONS AND FINAL NET (2ND CUTOFF)
                System.out.println("Each Deduction:");
                System.out.println("    SSS: " + sss);
                System.out.println("    PhilHealth: " + philHealth);
                System.out.println("    Pag-IBIG: " + pagIbig);
                System.out.println("    Tax: " + withholdingTax);
                System.out.println("Total Deductions: " + totalDeductions);
                System.out.println("Net Salary: " + netSecondCutoff);
                System.out.println("---------------------------------------");
            }

            // SUBCHOICE 2: ALL EMPLOYEES

        } else if (subChoice.equals("2")) {

            // prints same payroll format per employee
            printAllEmployeesPayroll();

        } else {

            // invalid subchoice
            System.out.println("Invalid choice.");
        }
    }

    static String[] findEmployee(String employeeNumberToFind) {

        // open employee file and scan row by row
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {

            // skip header row
            br.readLine();

            String line;

            // read each employee row
            while ((line = br.readLine()) != null) {

                // ignore blank lines
                if (line.trim().isEmpty()) continue;

                // split CSV columns
                String[] data = line.split(",");

                // match employee number (column 0)
                if (data[0].trim().equals(employeeNumberToFind)) {
                    return data;
                }
            }

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        // return null if employee number was not found
        return null;
    }

    static double parseHourlyRate(String[] employeeRow) {

        // hourly rate is assumed to be the last column
        try {
            String rateString = employeeRow[employeeRow.length - 1].trim();

            // remove quotes and commas if present
            rateString = rateString.replace("\"", "").replace(",", "");

            return Double.parseDouble(rateString);

        } catch (Exception e) {
            return -1;
        }
    }

    static void printAllEmployeesPayroll() {

        // read each employee from employee file and compute payroll month-by-month
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_FILE))) {

            // skip header row
            br.readLine();

            String line;

            // loop through every employee row
            while ((line = br.readLine()) != null) {

                // ignore blank lines
                if (line.trim().isEmpty()) continue;

                // split employee row
                String[] employeeRow = line.split(",");

                // basic employee details (based on file columns)
                String employeeNumber = employeeRow[0].trim();
                String lastName = employeeRow[1].trim();
                String firstName = employeeRow[2].trim();
                String birthday = employeeRow[3].trim();

                // display header for each employee
                System.out.println("======================================");
                System.out.println("Employee #: " + employeeNumber);
                System.out.println("Employee Name: " + firstName + " " + lastName);
                System.out.println("Birthday: " + birthday);
                System.out.println("======================================");

                // parse hourly rate from the same row
                double hourlyRate = parseHourlyRate(employeeRow);

                // skip employee if hourly rate is invalid
                if (hourlyRate < 0) {
                    System.out.println("Hourly rate not found.");
                    continue;
                }

                // compute payroll for June to December
                for (int month = 6; month <= 12; month++) {

                    int daysInMonth = YearMonth.of(2024, month).lengthOfMonth();

                    // first cutoff
                    double hoursFirstCutoff = getTotalHours(employeeNumber, month, 1, 15);
                    double grossFirstCutoff = hoursFirstCutoff * hourlyRate;
                    double lateDedFirstCutoff = getLateDeduction(employeeNumber, month, 1, 15, hourlyRate);
                    double netFirstCutoff = grossFirstCutoff - lateDedFirstCutoff;

                    System.out.println("\nCutoff Date: " + monthName(month) + " 1 to 15");
                    System.out.println("Total Hours Worked: " + hoursFirstCutoff);
                    System.out.println("Gross Salary: " + grossFirstCutoff);
                    System.out.println("Net Salary: " + netFirstCutoff);

                    // second cutoff
                    double hoursSecondCutoff = getTotalHours(employeeNumber, month, 16, daysInMonth);
                    double grossSecondCutoff = hoursSecondCutoff * hourlyRate;
                    double lateDedSecondCutoff = getLateDeduction(employeeNumber, month, 16, daysInMonth, hourlyRate);

                    System.out.println("\nCutoff Date: " + monthName(month) + " 16 to " + daysInMonth);
                    System.out.println("Total Hours Worked: " + hoursSecondCutoff);
                    System.out.println("Gross Salary: " + grossSecondCutoff);

                    // monthly basis + deductions
                    double netSecondBeforeGov = grossSecondCutoff - lateDedSecondCutoff;
                    double monthlyNetBasis = netFirstCutoff + netSecondBeforeGov;

                    double sss = getSSS(monthlyNetBasis);
                    double philHealth = getPhilHealth(monthlyNetBasis);
                    double pagIbig = getPagIbig(monthlyNetBasis);

                    double taxableIncome = monthlyNetBasis - (sss + philHealth + pagIbig);
                    double withholdingTax = getWithholdingTax(taxableIncome);

                    double totalDeductions = sss + philHealth + pagIbig + withholdingTax;
                    double netSecondCutoff = netSecondBeforeGov - totalDeductions;

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

        // open attendance file and scan rows
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {

            // skip header row
            br.readLine();

            String line;

            // loop through attendance records
            while ((line = br.readLine()) != null) {

                // ignore blank lines
                if (line.trim().isEmpty()) continue;

                // split CSV columns (must match attendance file structure)
                String[] data = line.split(",");

                // keep only rows for the selected employee number
                if (!data[0].trim().equals(employeeNumber)) continue;

                // parse date column (MM/DD/YYYY)
                String[] dateParts = data[3].trim().split("/");
                int recordMonth = Integer.parseInt(dateParts[0]);
                int recordDay = Integer.parseInt(dateParts[1]);
                int recordYear = Integer.parseInt(dateParts[2]);

                // keep only year 2024
                if (recordYear != 2024) continue;

                // keep only the selected month
                if (recordMonth != month) continue;

                // keep only day range of the cutoff
                if (recordDay < dayStart || recordDay > dayEnd) continue;

                // parse login/logout times (H:mm)
                LocalTime login = LocalTime.parse(data[4].trim(), TIME_FORMAT);
                LocalTime logout = LocalTime.parse(data[5].trim(), TIME_FORMAT);

                // add credited hours for this day based on rules
                totalHours += computeHours(login, logout);
            }

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }

        // no rounding (per instruction)
        return totalHours;
    }

    static double computeHours(LocalTime login, LocalTime logout) {

        // constants for shift rules
        LocalTime startTime = LocalTime.of(8, 0);
        LocalTime endTime = LocalTime.of(17, 0);
        LocalTime graceTime = LocalTime.of(8, 10);

        // cap logout at 5:00 PM (no extra hours)
        if (logout.isAfter(endTime)) logout = endTime;

        // if logout is not after 8:00 AM, no valid work time
        if (!logout.isAfter(startTime)) return 0;

        // grace rule: treat login as 8:00 if login is 8:10 or earlier
        if (!login.isAfter(graceTime)) login = startTime;

        // if logout is not after adjusted login, no valid work time
        if (!logout.isAfter(login)) return 0;

        // compute minutes between adjusted login and logout
        long minutesWorked = Duration.between(login, logout).toMinutes();

        // subtract 1 hour lunch if there is enough working time
        if (minutesWorked > 60) minutesWorked -= 60;
        else minutesWorked = 0;

        // convert minutes to hours
        double hours = minutesWorked / 60.0;

        // cap daily credit to 8 hours
        if (hours > 8.0) hours = 8.0;

        return hours;
    }

    static double getLateDeduction(String employeeNumber, int month, int dayStart, int dayEnd, double hourlyRate) {

        double totalLateDeduction = 0;

        // open attendance file and scan rows
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {

            // skip header row
            br.readLine();

            String line;

            // loop through attendance records
            while ((line = br.readLine()) != null) {

                // ignore blank lines
                if (line.trim().isEmpty()) continue;

                // split columns
                String[] data = line.split(",");

                // keep only selected employee
                if (!data[0].trim().equals(employeeNumber)) continue;

                // parse date (MM/DD/YYYY)
                String[] dateParts = data[3].trim().split("/");
                int recordMonth = Integer.parseInt(dateParts[0]);
                int recordDay = Integer.parseInt(dateParts[1]);
                int recordYear = Integer.parseInt(dateParts[2]);

                // keep only year 2024
                if (recordYear != 2024) continue;

                // keep only selected month
                if (recordMonth != month) continue;

                // keep only day range of cutoff
                if (recordDay < dayStart || recordDay > dayEnd) continue;

                // parse login time
                LocalTime login = LocalTime.parse(data[4].trim(), TIME_FORMAT);

                // constants for lateness
                LocalTime startTime = LocalTime.of(8, 0);
                LocalTime graceTime = LocalTime.of(8, 10);

                // not late if login is within grace
                if (!login.isAfter(graceTime)) continue;

                // late minutes counted from 8:00 to actual login
                long lateMinutes = Duration.between(startTime, login).toMinutes();

                // convert late minutes to money deduction using hourly rate
                totalLateDeduction += (lateMinutes / 60.0) * hourlyRate;
            }

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }


        return totalLateDeduction;
    }

    // DEDUCTION FUNCTIONS

    static double getSSS(double monthlyNetBasis) {

        // minimum bracket
        if (monthlyNetBasis < 3250.0) return 135.0;

        // bracket table (employee share values)
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

        // maximum bracket used in the table
        return 1125.0;
    }

    static double getPhilHealth(double monthlyNetBasis) {

        // apply base limits first (10,000 min, 60,000 max)
        double base = monthlyNetBasis;
        if (base < 10000.0) base = 10000.0;
        if (base > 60000.0) base = 60000.0;

        // total premium = base × 3%
        double premium = base * 0.03;

        // employee share is 50% of total premium
        return premium / 2.0;
    }

    static double getPagIbig(double monthlyNetBasis) {

        // rate depends on salary threshold
        double rate;
        if (monthlyNetBasis <= 1500.0) rate = 0.01;
        else rate = 0.02;

        // contribution = salary × rate
        double contribution = monthlyNetBasis * rate;

        // cap employee share at 100
        if (contribution > 100.0) contribution = 100.0;

        return contribution;
    }

    static double getWithholdingTax(double taxableIncome) {

        // tax brackets (based on the table)
        if (taxableIncome <= 20833.0) return 0.0;
        if (taxableIncome < 33333.0) return (taxableIncome - 20833.0) * 0.20;
        if (taxableIncome < 66667.0) return 2500.0 + (taxableIncome - 33333.0) * 0.25;
        if (taxableIncome < 166667.0) return 10833.33 + (taxableIncome - 66667.0) * 0.30;
        if (taxableIncome < 666667.0) return 40833.33 + (taxableIncome - 166667.0) * 0.32;

        return 200833.33 + (taxableIncome - 666667.0) * 0.35;
    }

    static String monthName(int month) {

        // convert numeric month (6–12) into name for printing
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