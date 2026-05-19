# SmartInventorySystem

Simple desktop inventory and billing system (Java + Swing). This repository was upgraded to target Java 21.

How to build locally (Windows PowerShell):

```powershell
# Install/open JDK 21 and Maven, then:
$env:JAVA_HOME='C:\Users\Lenovo\AppData\Local\jdks\jdk-21.0.10'
& 'C:\Users\Lenovo\.maven\maven-3.9.16\bin\mvn.cmd' -B clean package
```
# Smart Inventory & Billing System

Desktop inventory and billing application built with Java Swing, Maven, and MySQL. The app provides product management, billing, PDF receipt generation, receipt history, CSV export, and a simple analytics dashboard.

## Features

- Secure login screen with role-based access for admin and cashier users.
- Inventory management for adding, updating, deleting, and viewing products.
- Billing workflow with product search, quantity entry, discounts, and checkout.
- Automatic stock updates and sales history storage in MySQL.
- PDF receipt generation on checkout.
- Receipt history with open and re-print support.
- Analytics dashboard with revenue, transaction count, and top-selling products.
- CSV export for the inventory table.

## Tech Stack

- Java 11
- Swing UI
- Maven
- MySQL
- FlatLaf for theming
- OpenPDF for receipt generation
- JFreeChart for analytics charts

## Prerequisites

- Java Development Kit 11 or newer
- Apache Maven
- MySQL Server
- A database named `inventory_db`

## Database Setup

The application connects through `src/main/java/com/inventory/DbConnection.java`.

Before running, make sure the database settings in that file match your local MySQL instance:

- JDBC URL
- Username
- Password

On first launch, the app creates the required tables automatically:

- `products`
- `sales_history`
- `users`

It also inserts demo credentials when the `users` table is empty.

## Default Demo Users

- Admin: `admin` / `admin123`
- Cashier: `cashier` / `cash123`

## Run the Application

### From an IDE

1. Open the project in IntelliJ IDEA, Eclipse, or VS Code.
2. Ensure Maven dependencies are downloaded.
3. Run `com.inventory.Main`.

### From the command line

1. Build the project:

   ```bash
   mvn clean package
   ```

2. Run the `com.inventory.Main` class from your IDE or with a Java command that includes the compiled classes and Maven dependencies on the classpath.

## Generated Files

- `inventory.csv` is created when you export the inventory table.
- `receipt_<id>.pdf` is generated after checkout.
- `receipts/reprint_<id>.pdf` is generated when a receipt is re-printed.

## Project Structure

- `src/main/java/com/inventory/Main.java` - app entry point and window setup
- `src/main/java/com/inventory/LoginFrame.java` - login screen
- `src/main/java/com/inventory/InventoryScreen.java` - product management
- `src/main/java/com/inventory/BillingScreen.java` - billing and receipt generation
- `src/main/java/com/inventory/DashboardScreen.java` - sales analytics
- `src/main/java/com/inventory/ReceiptHistoryScreen.java` - past receipts and reprint/open actions
- `src/main/java/com/inventory/DbConnection.java` - database connection settings

## Notes

- The app expects MySQL to be available locally.
- Receipt and analytics data depend on the `sales_history` table.
- If you change the database name or credentials, update `DbConnection.java` accordingly.