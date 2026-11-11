// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.solutiontemplates;

import io.qameta.allure.Epic;
import org.testng.annotations.BeforeClass;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.AssetPageElements;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DashboardPageHelper;
import org.thingsboard.server.msa.ui.pages.DevicePageElements;
import org.thingsboard.server.msa.ui.pages.InstancesPageElements;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.RolesPageElements;
import org.thingsboard.server.msa.ui.pages.RuleChainTemplatesPageElements;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SchedulerPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.pages.SolutionTemplateDetailsPageHelper;
import org.thingsboard.server.msa.ui.pages.SolutionTemplatesHomePageElements;
import org.thingsboard.server.msa.ui.pages.SolutionTemplatesInstalledViewHelper;
import org.thingsboard.server.msa.ui.pages.UsersPageElements;

@Epic("Solution templates")
abstract public class AbstractSolutionTemplateTest extends AbstractDriverBaseTest {
    protected SideBarMenuViewHelper sideBarMenuView;
    protected SolutionTemplatesHomePageElements solutionTemplatesHomePage;
    protected SolutionTemplateDetailsPageHelper solutionTemplateDetailsPage;
    protected SolutionTemplatesInstalledViewHelper solutionTemplatesInstalledView;
    protected RuleChainsPageHelper ruleChainsPage;
    protected RolesPageElements rolesPage;
    protected CustomerPageHelper customerPage;
    protected ProfilesPageHelper profilesPage;
    protected DevicePageElements devicePage;
    protected DashboardPageHelper dashboardPage;
    protected UsersPageElements usersPage;
    protected AssetPageElements assetPage;
    protected SchedulerPageHelper schedulerPage;
    protected InstancesPageElements instancesPage;
    protected RuleChainTemplatesPageElements ruleChainTemplatesPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        solutionTemplatesHomePage = new SolutionTemplatesHomePageElements(driver);
        solutionTemplateDetailsPage = new SolutionTemplateDetailsPageHelper(driver);
        solutionTemplatesInstalledView = new SolutionTemplatesInstalledViewHelper(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
        rolesPage = new RolesPageElements(driver);
        customerPage = new CustomerPageHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
        devicePage = new DevicePageElements(driver);
        dashboardPage = new DashboardPageHelper(driver);
        usersPage = new UsersPageElements(driver);
        assetPage = new AssetPageElements(driver);
        schedulerPage = new SchedulerPageHelper(driver);
        instancesPage = new InstancesPageElements(driver);
        ruleChainTemplatesPage = new RuleChainTemplatesPageElements(driver);
    }

    @BeforeClass
    public void deletePublicCustomer() {
        deleteCustomerByName("Public");
    }
}
