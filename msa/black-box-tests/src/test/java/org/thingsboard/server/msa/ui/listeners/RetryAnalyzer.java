package listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 2;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            System.out.printf("Retrying test %s for the %d time(s).%n", result.getName(), retryCount + 1);
            retryCount++;
            return true;
        }
        return false;
    }
}
