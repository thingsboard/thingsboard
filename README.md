# ThingsBoard Setup and Installation Guide

This guide provides detailed instructions for setting up and running ThingsBoard from source code.

## Prerequisites

Before you begin, ensure you have the following installed on your system:

- **Java 11 or higher** (OpenJDK or Oracle JDK)
- **Maven 3.6.0 or higher**
- **Node.js 16.x or higher** (for Angular frontend)
- **npm** (comes with Node.js)
- **PostgreSQL 12 or higher**
- **Git**

## Step 1: Clone the Repository

First, clone the ThingsBoard repository from GitHub:

```bash
git clone https://github.com/thingsboard/thingsboard.git
cd thingsboard
```

## Step 2: Build the Project

Use Maven to build the entire project:

```bash
mvn clean install -DskipTests
```

This command will:
- Clean any previous builds
- Compile the source code
- Package the application
- Skip running tests for faster build

> **Note**: The build process may take several minutes depending on your system performance and internet connection.

## Step 3: Configure Environment Variables

Set up the required environment variables for PostgreSQL connection:

### Linux/macOS:
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/thingsboard
export SPRING_DATASOURCE_USERNAME=your_postgres_username
export SPRING_DATASOURCE_PASSWORD=your_postgres_password
```

### Windows (Command Prompt):
```cmd
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/thingsboard
set SPRING_DATASOURCE_USERNAME=your_postgres_username
set SPRING_DATASOURCE_PASSWORD=your_postgres_password
```

### Windows (PowerShell):
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/thingsboard"
$env:SPRING_DATASOURCE_USERNAME="your_postgres_username"
$env:SPRING_DATASOURCE_PASSWORD="your_postgres_password"
```

## Step 4: Database Setup

### Create PostgreSQL Database

First, create a database in PostgreSQL:

```sql
CREATE DATABASE thingsboard;
```

### Run Database Installation Script

Navigate to the appropriate directory based on your operating system and run the database installation script:

#### For Windows:
```bash
cd application/target/windows
./install-dev-db.bat
```

#### For Linux/macOS:
```bash
cd application/target/bin
./install-dev-db.sh
```

This script will:
- Create the necessary database schema
- Insert initial data
- Set up default system settings

## Step 5: Start the Backend Server

Navigate to the application target directory and start the ThingsBoard backend:

```bash
cd application/target
java -jar thingsboard-*.jar
```

> **Alternative**: You can also use the specific JAR file name, for example:
> ```bash
> java -jar thingsboard-3.x.x.jar
> ```

The backend server will start on port 8080 by default. You should see logs indicating successful startup.

## Step 6: Start the Angular Frontend

Open a new terminal window/tab and navigate to the UI directory:

```bash
cd ui-ngx
```

Install the required npm dependencies:

```bash
npm install
```

Start the Angular development server:

```bash
npm start
```

The frontend will be available at `http://localhost:4200`.

## Step 7: Access ThingsBoard

Once both backend and frontend are running:

1. Open your web browser
2. Navigate to `http://localhost:4200`
3. Use the default credentials:
   - **Username**: `sysadmin@thingsboard.org`
   - **Password**: `sysadmin`

## Additional Configuration

### Memory Settings

If you encounter memory issues, you can adjust JVM heap size:

```bash
java -Xms1g -Xmx2g -jar thingsboard-*.jar
```

### Custom Configuration

You can override default settings by creating an `application.yml` file in the same directory as the JAR file or by setting environment variables.

## Troubleshooting

### Common Issues

1. **Port already in use**: If port 8080 is occupied, you can change it:
   ```bash
   java -jar -Dserver.port=8081 thingsboard-*.jar
   ```

2. **Database connection errors**: Verify that:
   - PostgreSQL is running
   - Database credentials are correct
   - Database `thingsboard` exists

3. **Build failures**: Make sure you have:
   - Correct Java version (11+)
   - Maven 3.6.0+
   - Stable internet connection

4. **Frontend issues**: If npm start fails:
   - Clear npm cache: `npm cache clean --force`
   - Delete node_modules: `rm -rf node_modules`
   - Reinstall dependencies: `npm install`

### Log Files

Check application logs for detailed error information:
- Backend logs appear in the terminal where you started the JAR
- Frontend logs appear in the terminal where you ran `npm start`

## Development Mode

For development, you can run the backend in debug mode:

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar thingsboard-*.jar
```

This allows you to attach a debugger on port 5005.

## Stopping the Application

To stop the services:

1. **Backend**: Press `Ctrl+C` in the terminal running the JAR file
2. **Frontend**: Press `Ctrl+C` in the terminal running `npm start`

## Next Steps

After successful installation, you can:

- Create tenant accounts
- Set up devices and assets
- Configure dashboards
- Explore the REST API documentation at `http://localhost:8080/swagger-ui.html`

## Support

For additional help and documentation, visit:
- [ThingsBoard Documentation](https://thingsboard.io/docs/)
- [Community Forum](https://groups.google.com/forum/#!forum/thingsboard)
- [GitHub Issues](https://github.com/thingsboard/thingsboard/issues)

---

**Note**: This guide assumes you're setting up ThingsBoard for development purposes. For production deployment, additional security and performance configurations are recommended.
