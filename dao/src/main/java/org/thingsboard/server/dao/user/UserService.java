/**
 * Copyright © 2016-2017 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.user;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.UserCredentials;

public interface UserService {
	
	public User findUserById(UserId userId);

	public ListenableFuture<User> findUserByIdAsync(UserId userId);

	public User findUserByEmail(String email);
	
	public User saveUser(User user);

	public UserCredentials findUserCredentialsByUserId(UserId userId);	
	
	public UserCredentials findUserCredentialsByActivateToken(String activateToken);

	public UserCredentials findUserCredentialsByResetToken(String resetToken);

	public UserCredentials saveUserCredentials(UserCredentials userCredentials);
	
	public UserCredentials activateUserCredentials(String activateToken, String password);
	
	public UserCredentials requestPasswordReset(String email);

	public void deleteUser(UserId userId);
	
	public TextPageData<User> findTenantAdmins(TenantId tenantId, TextPageLink pageLink);
	
	public void deleteTenantAdmins(TenantId tenantId);
	
	public TextPageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, TextPageLink pageLink);
	    
	public void deleteCustomerUsers(TenantId tenantId, CustomerId customerId);
	
}
