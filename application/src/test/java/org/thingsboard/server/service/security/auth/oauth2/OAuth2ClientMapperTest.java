// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.security.auth.oauth2;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.controller.AbstractControllerTest;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.oauth2.OAuth2User;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DaoSqlTest
public class OAuth2ClientMapperTest extends AbstractControllerTest {

    @Autowired
    private BasicOAuth2ClientMapper basicOAuth2ClientMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private CustomerService customerService;

    @Test
    public void testShouldNotFindUserOfOtherTenantForTenantOauth2Client() throws Exception {
        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        // the email attribute is controlled by whoever runs the identity provider behind the client
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(SYS_ADMIN_EMAIL);

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, tenantClient));
        assertThat(exception.getMessage()).isEqualTo("User not found: " + SYS_ADMIN_EMAIL);

        User sysAdmin = userService.findUserByEmail(TenantId.SYS_TENANT_ID, SYS_ADMIN_EMAIL);
        assertThat(sysAdmin.getTenantId()).isEqualTo(TenantId.SYS_TENANT_ID);
        assertThat(sysAdmin.getAuthority()).isEqualTo(Authority.SYS_ADMIN);
    }

    @Test
    public void testShouldFindExistingUsersOfOwnTenantForTenantOauth2Client() throws Exception {
        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        // the everyday flow: existing users of the client's own tenant must still resolve, at tenant and at customer level
        OAuth2User tenantAdmin = new OAuth2User();
        tenantAdmin.setEmail(TENANT_ADMIN_EMAIL);
        SecurityUser tenantAdminSecurityUser = basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(tenantAdmin, tenantClient);
        assertThat(tenantAdminSecurityUser.getId()).isEqualTo(tenantAdminUserId);

        OAuth2User customerUser = new OAuth2User();
        customerUser.setEmail(CUSTOMER_USER_EMAIL);
        SecurityUser customerSecurityUser = basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(customerUser, tenantClient);
        assertThat(customerSecurityUser.getId()).isEqualTo(customerUserId);
        assertThat(customerSecurityUser.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    public void testShouldNotCreateCustomerUnderOtherTenantForTenantOauth2Client() throws Exception {
        loginDifferentTenantCustomer();

        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        // a custom mapper endpoint can name any customer as the parent of the customer it asks to create
        String email = "userB@corporation.gmail.com";
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(email);
        oAuth2User.setParentCustomerId(differentTenantCustomerId);
        oAuth2User.setCustomerName("Grafted Customer");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, tenantClient));
        assertThat(exception.getCause().getMessage())
                .isEqualTo("Customer with id '" + differentTenantCustomerId.getId() + "' is not found");

        assertThat(customerService.findCustomerByTenantIdAndTitle(tenantId, "Grafted Customer")).isEmpty();
        assertThat(userService.findUserByEmail(TenantId.SYS_TENANT_ID, email)).isNull();
    }

    @Test
    public void testShouldNotCreateCustomerUnderUnknownParentForTenantOauth2Client() throws Exception {
        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        // a dangling parent id would leave a customer whose owner chain cannot be resolved at all
        CustomerId unknownCustomerId = new CustomerId(UUID.randomUUID());
        String email = "userC@corporation.gmail.com";
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(email);
        oAuth2User.setParentCustomerId(unknownCustomerId);
        oAuth2User.setCustomerName("Dangling Customer");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, tenantClient));
        assertThat(exception.getCause().getMessage())
                .isEqualTo("Customer with id '" + unknownCustomerId.getId() + "' is not found");

        assertThat(customerService.findCustomerByTenantIdAndTitle(tenantId, "Dangling Customer")).isEmpty();
        assertThat(userService.findUserByEmail(TenantId.SYS_TENANT_ID, email)).isNull();
    }

    @Test
    public void testShouldCreateUserInCustomerWithinClientScope() throws Exception {
        loginCustomerAdminUser();
        OAuth2Client customerClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "customer client"), OAuth2Client.class);

        // the client owner itself is in scope
        String ownerUserEmail = "userInOwner@corporation.gmail.com";
        OAuth2User userInOwner = new OAuth2User();
        userInOwner.setEmail(ownerUserEmail);
        userInOwner.setCustomerId(customerId);

        SecurityUser ownerSecurityUser = basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(userInOwner, customerClient);
        assertThat(ownerSecurityUser.getCustomerId()).isEqualTo(customerId);

        // and so is a customer below it
        String subUserEmail = "userInSubCustomer@corporation.gmail.com";
        OAuth2User userInSubCustomer = new OAuth2User();
        userInSubCustomer.setEmail(subUserEmail);
        userInSubCustomer.setCustomerId(subCustomerId);

        SecurityUser subSecurityUser = basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(userInSubCustomer, customerClient);
        assertThat(subSecurityUser.getCustomerId()).isEqualTo(subCustomerId);

        assertThat(userService.findUserByEmail(TenantId.SYS_TENANT_ID, ownerUserEmail)).isNotNull();
        assertThat(userService.findUserByEmail(TenantId.SYS_TENANT_ID, subUserEmail)).isNotNull();
    }

    @Test
    public void testShouldCreateCustomerFromMapperNameUnderClientOwner() throws Exception {
        loginCustomerAdminUser();
        OAuth2Client customerClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "customer client"), OAuth2Client.class);

        // customers named by the mapper must be created inside the client owner, not at tenant root
        String email = "userD@corporation.gmail.com";
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(email);
        oAuth2User.setParentCustomerName("Mapper Parent Customer");
        oAuth2User.setCustomerName("Mapper Child Customer");

        SecurityUser securityUser = basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, customerClient);

        Customer parentCustomer = customerService.findCustomerByTenantIdAndTitle(tenantId, "Mapper Parent Customer").orElseThrow();
        Customer childCustomer = customerService.findCustomerByTenantIdAndTitle(tenantId, "Mapper Child Customer").orElseThrow();
        assertThat(parentCustomer.getParentCustomerId()).isEqualTo(customerId);
        assertThat(childCustomer.getParentCustomerId()).isEqualTo(parentCustomer.getId());
        assertThat(securityUser.getCustomerId()).isEqualTo(childCustomer.getId());
    }

    @Test
    public void testShouldCreateCustomerUserForCustomerOauth2ClientWithoutCustomerName() throws Exception {
        loginCustomerAdminUser();
        OAuth2Client customerClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "customer client"), OAuth2Client.class);

        // a mapper that names no customer must still land the user in the client's own customer, not fail
        String email = "userWithoutCustomerName@corporation.gmail.com";
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(email);

        SecurityUser securityUser = basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, customerClient);
        assertThat(securityUser.getCustomerId()).isEqualTo(customerId);
        assertThat(securityUser.getAuthority()).isEqualTo(Authority.CUSTOMER_USER);

        User created = userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);
        assertThat(created.getAuthority()).isEqualTo(Authority.CUSTOMER_USER);
        assertThat(created.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    public void testShouldNotCreateUserInExistingOtherCustomerByName() throws Exception {
        loginTenantAdmin();
        Customer otherCustomer = new Customer();
        otherCustomer.setTitle("Other Customer");
        Customer savedOtherCustomer = doPost("/api/customer", otherCustomer, Customer.class);

        loginCustomerAdminUser();
        OAuth2Client customerClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "customer client"), OAuth2Client.class);

        // the by-name twin of the by-id escape hatch: an existing customer outside the client owner's subtree.
        // Only the name is supplied, so the rejection can only come from the guard in getCustomerId
        String email = "userE@corporation.gmail.com";
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(email);
        oAuth2User.setCustomerName("Other Customer");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, customerClient));
        assertThat(exception.getCause().getMessage())
                .isEqualTo("Customer with id '" + savedOtherCustomer.getId().getId() + "' is not owned by OAuth2 client owner");

        assertThat(userService.findUserByEmail(TenantId.SYS_TENANT_ID, email)).isNull();
    }

    @Test
    public void testShouldNotCreateParentCustomerWhenSuppliedCustomerIdIsRejected() throws Exception {
        loginDifferentTenantCustomer();

        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        // the parent name would be created on the way to a customer id that is then refused
        String email = "userF@corporation.gmail.com";
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(email);
        oAuth2User.setParentCustomerName("Orphan Parent Customer");
        oAuth2User.setCustomerId(differentTenantCustomerId);

        assertThrows(
                RuntimeException.class,
                () -> basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, tenantClient));

        // a refused login must leave nothing behind
        assertThat(customerService.findCustomerByTenantIdAndTitle(tenantId, "Orphan Parent Customer")).isEmpty();
        assertThat(userService.findUserByEmail(TenantId.SYS_TENANT_ID, email)).isNull();
    }

    @Test
    public void testShouldNotFindTenantAdminForCustomerOauth2Client() throws Exception {
        loginCustomerAdminUser();
        OAuth2Client customerClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "customer client"), OAuth2Client.class);

        // the tenant admin sits above the client owner
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(TENANT_ADMIN_EMAIL);

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, customerClient));
        assertThat(exception.getMessage()).isEqualTo("User not found: " + TENANT_ADMIN_EMAIL);
    }

    @Test
    public void testShouldFindSubCustomerUserForCustomerOauth2Client() throws Exception {
        loginCustomerAdminUser();
        OAuth2Client customerClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "customer client"), OAuth2Client.class);

        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(SUB_CUSTOMER_ADMIN_USER_EMAIL);

        // the sub-customer user is owned by the client owner, so it must still resolve
        SecurityUser securityUser = basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, customerClient);
        assertThat(securityUser.getId()).isEqualTo(subCustomerAdminUserId);
        assertThat(securityUser.getCustomerId()).isEqualTo(subCustomerId);
    }

    @Test
    public void testShouldNotCreateUserInOtherCustomerForCustomerOauth2Client() throws Exception {
        loginTenantAdmin();
        Customer otherCustomer = new Customer();
        otherCustomer.setTitle("Other Customer");
        Customer savedOtherCustomer = doPost("/api/customer", otherCustomer, Customer.class);

        loginCustomerAdminUser();
        OAuth2Client customerClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "customer client"), OAuth2Client.class);

        // a custom mapper endpoint can name any customer, here one outside the client owner's subtree
        String email = "userA@corporation.gmail.com";
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(email);
        oAuth2User.setCustomerId(savedOtherCustomer.getId());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, customerClient));
        assertThat(exception.getCause().getMessage())
                .isEqualTo("Customer with id '" + savedOtherCustomer.getId().getId() + "' is not owned by OAuth2 client owner");

        assertThat(userService.findUserByEmail(TenantId.SYS_TENANT_ID, email)).isNull();
    }

    @Test
    public void testShouldCreateUserInClientTenant() throws Exception {
        loginDifferentTenant();
        loginTenantAdmin();
        OAuth2Client tenantClient = doPost("/api/oauth2/client", createOauth2Client(tenantId, "tenant client"), OAuth2Client.class);

        // a custom mapper endpoint may return any tenant id; the client's own tenant must win
        String email = "userB@corporation.gmail.com";
        OAuth2User oAuth2User = new OAuth2User();
        oAuth2User.setEmail(email);
        oAuth2User.setTenantId(differentTenantId);

        basicOAuth2ClientMapper.getOrCreateSecurityUserFromOAuth2User(oAuth2User, tenantClient);

        User created = userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);
        assertThat(created.getTenantId()).isEqualTo(tenantId);

        loginSysAdmin();
        deleteDifferentTenant();
    }

}
