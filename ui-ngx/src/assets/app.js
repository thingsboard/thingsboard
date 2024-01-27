/*
 * Copyright © 2016-2023 The Thingsboard Authors
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
window.addEventListener("load", (event) => {
  //checkLicense();
});

function checkLicense() {
  var settings = {
    url: "https://license.viphap.com/license/validate-ui",
    method: "POST",
    timeout: 0,
    headers: {
      "Content-Type": "application/json",
    },
    data: JSON.stringify({
      Tenant: document.title.split("|")[0].trim(),
      Domain: window.location.hostname,
    }),
  };

  $.ajax(settings)
    .done(function (response, status, jqXHR) {
      if (jqXHR.status != 200) {
        let content = `
                  <div class="license">
                      <div class="message">
                          <img src="https://newsense.viphap.com/assets/logo_text_white.png" alt="VIPHAP NewSense">
                          <p>Hi,</p>
                          <p>would like to inform you that your license for our software/product is about to expire.</p>
                          <p>To continue using our software/product, you will need to renew your license before the expiration date. Failure to do so may result in the software/product becoming
                              inactive.</p>
                          <p>Renewal options and instructions can be found on our website at <a href="https://viphap.com">VIPHAP</a>. If you have any questions or need further
                              assistance, please contact our support team at <a href="mail:info@viphap.com">info@viphap.com</a>.</p>
                          <p>Thank you for being a valued customer and we look forward to providing you with continued access to our software/product.</p>
                      </div>
                  </div>
              `;
        $("body").empty();
        $("body").append(content);
      }
    })
    .error(function (jqXHR, status, error) {
      let content = `
          <div class="license">
              <div class="message">
                  <img src="https://newsense.viphap.com/assets/logo_text_white.png" alt="Viphap NewSense">
                  <p>Hi,</p>
                  <p>would like to inform you that your license for our software/product is about to expire.</p>
                  <p>To continue using our software/product, you will need to renew your license before the expiration date. Failure to do so may result in the software/product becoming
                      inactive.</p>
                  <p>Renewal options and instructions can be found on our website at <a href="https://viphap.com">VIPHAP</a>. If you have any questions or need further
                      assistance, please contact our support team at <a href="mail:info@viphap.com">info@viphap.com</a>.</p>
                  <p>Thank you for being a valued customer and we look forward to providing you with continued access to our software/product.</p>
              </div>
          </div>
          `;
      $("body").empty();
      $("body").append(content);
    });
}
