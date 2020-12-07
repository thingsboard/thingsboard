<#--

    Copyright © 2016-2020 The Thingsboard Authors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
		"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
	  style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;">
<head>
	<meta name="viewport" content="width=device-width"/>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
	<title>Thingsboard - Api Usage State</title>


	<style type="text/css">
		img {
			max-width: 100%;
		}

		body {
			-webkit-font-smoothing: antialiased;
			-webkit-text-size-adjust: none;
			width: 100% !important;
			height: 100%;
			line-height: 1.6em;
		}

		body {
			background-color: #f6f6f6;
		}

		@media only screen and (max-width: 640px) {
			body {
				padding: 0 !important;
			}

			h1 {
				font-weight: 800 !important;
				margin: 20px 0 5px !important;
			}

			h2 {
				font-weight: 800 !important;
				margin: 20px 0 5px !important;
			}

			h3 {
				font-weight: 800 !important;
				margin: 20px 0 5px !important;
			}

			h4 {
				font-weight: 800 !important;
				margin: 20px 0 5px !important;
			}

			h1 {
				font-size: 22px !important;
			}

			h2 {
				font-size: 18px !important;
			}

			h3 {
				font-size: 16px !important;
			}

			.container {
				padding: 0 !important;
				width: 100% !important;
			}

			.content {
				padding: 0 !important;
			}

			.content-wrap {
				padding: 10px !important;
			}

			.invoice {
				width: 100% !important;
			}
		}
	</style>
</head>

<body itemscope itemtype="http://schema.org/EmailMessage"
	  style="font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; -webkit-font-smoothing: antialiased; -webkit-text-size-adjust: none; width: 100% !important; height: 100%; line-height: 1.6em; background-color: #f6f6f6; margin: 0;"
	  bgcolor="#f6f6f6">

<table class="main" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 14px; box-sizing: border-box; border-radius: 3px; width: 100%; background-color: #f6f6f6; margin: 0px auto;" cellspacing="0" cellpadding="0" bgcolor="#f6f6f6">
	<tbody>
	<tr style="box-sizing: border-box; margin: 0px;">
		<td class="content-wrap" style="box-sizing: border-box; vertical-align: top; margin: 0px; padding: 20px;" align="center" valign="top">
			<table style="box-sizing: border-box; border: 1px solid #e9e9e9; border-radius: 3px; margin: 0px; height: 367px; background-color: #ffffff; width: 600px; max-width: 600px !important;" width="600" cellspacing="0" cellpadding="0">
				<tbody>
				<tr style="font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;">
					<td class="content-block" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #348eda; box-sizing: border-box; font-size: 14px; vertical-align: top; margin: 0px; padding: 0px; height: 110px;" valign="top"><img src="https://media.thingsboard.io/email/head.png" alt="" width="598" height="91" /></td>
				</tr>
				<tr style="font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 16px; margin: 0;">
					<td class="content-block" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #000000; box-sizing: border-box; font-size: 16px; margin: 0px; padding: 0px 32px; height: 66px; vertical-align: middle;" valign="middle"><strong>Warning:</strong> your ThingsBoard account feature may be disabled soon</td>
				</tr>
				<tr style="font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; line-height: 24px; margin: 0;">
					<td class="content-block" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; box-sizing: border-box; font-size: 14px; margin: 0px; padding: 0px 32px; height: 93px; vertical-align: top;" valign="top">
						<div style="padding: 16px; margin-bottom: 24px; border: solid 2px #F2994A; border-radius: 6px; background: rgba(242, 153, 74, 0.05);"><img style="vertical-align: middle; padding-right: 6px;" src="https://media.thingsboard.io/email/warning.png" alt="" width="20" height="20" />
							<div style="display: inline; vertical-align: middle;">ThingsBoard has already&nbsp;${apiValueLabel}.<br />${apiFeature} will be <strong>disabled</strong> for your account once the limit will be reached.</div>
						</div>
					</td>
				</tr>
				<tr style="font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;">
					<td class="content-block" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; box-sizing: border-box; font-size: 14px; vertical-align: top; margin: 0px; padding: 0px 32px; height: 59px;" valign="top">Please contact your system administrator to resolve the issue.</td>
				</tr>
				<tr style="font-family: 'Helvetica Neue',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;">
					<td class="content-block" style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; box-sizing: border-box; font-size: 14px; vertical-align: top; margin: 0px; padding: 0px 32px; height: 40px;" valign="top">&mdash; The ThingsBoard</td>
				</tr>
				</tbody>
			</table>
		</td>
	</tr>
	</tbody>
</table>
<table style="color: #999999; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 14px; box-sizing: border-box; margin: 0px auto; height: 64px; background-color: #f6f6f6; width: 100%;" cellpadding="0px 0px 20px">
	<tbody>
	<tr style="box-sizing: border-box; margin: 0px;">
		<td class="aligncenter content-block" style="box-sizing: border-box; font-size: 12px; margin: 0px; padding: 0px 0px 20px; width: 600px; text-align: center; vertical-align: middle;" align="center" valign="top">This email was sent to&nbsp;<a style="box-sizing: border-box; color: #999999; margin: 0px;" href="mailto:${targetEmail}">${targetEmail}</a>&nbsp;by ThingsBoard.</td>
	</tr>
	</tbody>
</table>
</body>
</html>
