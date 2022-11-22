package listeners;

import base.Base;
import base.TestInit;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

@Slf4j
public class TestListener extends Base implements ITestListener {

    WebDriver driver;

    public void onTestSuccess(ITestResult tr) {
        String str = "Test " + tr.getMethod().getMethodName() + " success";
        log.info("*----------------------* " + str + " *----------------------*");
        Allure.getLifecycle().updateTestCase((t) -> {
            t.setStatusDetails(t.getStatusDetails().setMessage(str));
        });
        driver = ((TestInit) tr.getInstance()).getDriver();
        captureScreen(driver, "success");
    }

    public void onTestFailure(ITestResult tr) {
        String str = "Test " + tr.getMethod().getMethodName() + " failure";
        String str1 = "Failed because of - " + tr.getThrowable();
        log.info("*----------------------* " + str + " *----------------------*");
        log.info("*----------------------* " + str1 + " *----------------------*");
        Allure.getLifecycle().updateTestCase((t) -> {
            t.setStatusDetails(t.getStatusDetails().setMessage(str));
            t.setStatusDetails(t.getStatusDetails().setMessage(str1));
        });
        driver = ((TestInit) tr.getInstance()).getDriver();
        captureScreen(driver, "failure");
    }

    public void onTestSkipped(ITestResult tr) {
        String str = "Test " + tr.getMethod().getMethodName() + " skipped";
        String str1 = "Skipped because of - " + tr.getThrowable();
        log.info("*----------------------* " + str + " *----------------------*");
        log.info("*----------------------* " + str1 + " *----------------------*");
        Allure.getLifecycle().updateTestCase((t) -> {
            t.setStatusDetails(t.getStatusDetails().setMessage(str));
            t.setStatusDetails(t.getStatusDetails().setMessage(str1));
        });
        driver = ((TestInit) tr.getInstance()).getDriver();
        captureScreen(driver, "skipped");
    }

    public void onStart(ITestContext testContext) {
        String str = "Test " + testContext.getCurrentXmlTest().getName() + " start";
        log.info("*----------------------* " + str + " *----------------------*");
    }

    public void onFinish(ITestContext testContext) {
        String str = "Test " + testContext.getCurrentXmlTest().getName() + " finish";
        log.info("*----------------------* " + str + " *----------------------*");
    }
}
