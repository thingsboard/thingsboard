package utils;

import org.testng.annotations.DataProvider;

import static base.Base.getRandomSymbol;
import static utils.Const.ENTITY_NAME;

public class DataProviderCredential {

    private static final String SYMBOL = String.valueOf(getRandomSymbol());
    private static final String NAME = ENTITY_NAME;
    private static final String NUMBER = "1";
    private static final String LONG_PHONE_NUMBER = "20155501231";
    private static final String SHORT_PHONE_NUMBER = "201555011";
    private static final String RULE_CHAIN_SECOND_WORD_NAME_PATH = "Rule";
    private static final String CUSTOMER_SECOND_WORD_NAME_PATH = "Customer";
    private static final String RULE_CHAIN_FIRST_WORD_NAME_PATH = "Root";
    private static final String CUSTOMER_FIRST_WORD_NAME_PATH = "A";

    @DataProvider
    public static Object[][] ruleChainNameForSearchByFirstAndSecondWord() {
        return new Object[][]{
                {RULE_CHAIN_SECOND_WORD_NAME_PATH},
                {RULE_CHAIN_FIRST_WORD_NAME_PATH}};
    }

    @DataProvider
    public static Object[][] nameForSearchBySymbolAndNumber() {
        return new Object[][]{
                {NAME, ENTITY_NAME.split("`")[1]},
                {NAME, String.valueOf(getRandomSymbol())}};
    }

    @DataProvider
    public static Object[][] nameForSort() {
        return new Object[][]{
                {NAME},
                {SYMBOL},
                {NUMBER}};
    }

    @DataProvider
    public static Object[][] nameForAllSort() {
        return new Object[][]{
                {NAME, SYMBOL, NUMBER}};
    }

    @DataProvider
    public static Object[][] incorrectPhoneNumber() {
        return new Object[][]{
                {LONG_PHONE_NUMBER},
                {SHORT_PHONE_NUMBER},
                {ENTITY_NAME}};
    }

    @DataProvider
    public static Object[][] customerNameForSearchByFirstAndSecondWord() {
        return new Object[][]{
                {CUSTOMER_FIRST_WORD_NAME_PATH},
                {CUSTOMER_SECOND_WORD_NAME_PATH}};
    }
}
