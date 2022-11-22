package utils;

import base.Base;

public class Const extends Base {

    public static final String URL = "http://localhost:8080/";
    public static final String TENANT_EMAIL = "tenant@thingsboard.org";
    public static final String TENANT_PASSWORD = "tenant";
    public static final String ENTITY_NAME = "Az!@#$%^&*()_-+=~`" + getRandomNumber();
    public static final String ROOT_RULE_CHAIN_NAME = "Root Rule Chain";
    public static final String IMPORT_RULE_CHAIN_NAME = "Rule Chain from Import";
    public static final String IMPORT_RULE_CHAIN_FILE_NAME = "forImport.json";
    public static final String IMPORT_TXT_FILE_NAME = "forImport.txt";
    public static final String EMPTY_IMPORT_MESSAGE = "No file selected";
    public static final String EMPTY_RULE_CHAIN_MESSAGE = "Rule chain name should be specified!";
    public static final String EMPTY_CUSTOMER_MESSAGE = "Customer title should be specified!";
    public static final String DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE = "The rule chain referenced by the device profiles cannot be deleted!";
    public static final String SAME_NAME_WARNING_CUSTOMER_MESSAGE = "Customer with such title already exists!";
    public static final String PHONE_NUMBER_ERROR_MESSAGE = "Phone number is invalid or not possible";
}