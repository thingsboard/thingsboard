 #### MQTT client tool installation instructions
---
<section style="margin: 18px 0">
  <div style="margin: 0 32px">
    <tb-toggle-header #publishCommandSteps value="ubuntu" name="publishCommandSteps" useSelectOnMdLg="false" appearance="fill">
      <tb-toggle-option value="ubuntu">Ubuntu</tb-toggle-option>
      <tb-toggle-option value="macos">MacOS</tb-toggle-option>
      <tb-toggle-option value="windows">Windows</tb-toggle-option>
    </tb-toggle-header>
  </div>
  <ng-container [ngSwitch]="publishCommandSteps.value">
    <ng-template [ngSwitchCase]="'ubuntu'">
      <p style="margin-top: 18px; padding: 0 32px">Install mqtt client tool:</p>
      <tb-markdown usePlainMarkdown containerClass="tb-getting-started-code" data="
      ```bash
      sudo apt-get install curl mosquitto-clients
      {:copy-code}
      ```
      "></tb-markdown>
    </ng-template>
    <ng-template [ngSwitchCase]="'macos'">
      <p style="margin-top: 18px; padding: 0 32px">Install mqtt client tool:</p>
      <tb-markdown usePlainMarkdown containerClass="tb-getting-started-code" data="
      ```bash
      brew install mosquitto-clients
      {:copy-code}
      ```
      "></tb-markdown>
    </ng-template>
    <ng-template [ngSwitchCase]="'windows'">
      <p style="margin-top: 18px; padding: 0 32px">Install mqtt client tool:</p>
      <a mat-stroked-button color="primary" class="ignore-style-a"
        style="margin: 8px 32px"    
        href="https://thingsboard.io/docs/getting-started-guides/helloworld/?connectdevice=mqtt-windows#step-2-connect-device" target="_blank">
        <mat-icon>description</mat-icon>How to install MQTT Box</a>
    </ng-template>
  </ng-container>
</section>
