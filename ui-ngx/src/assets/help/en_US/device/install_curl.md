#### cURL installation instructions
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
      <p style="margin-top: 18px; padding: 0 32px">Install cURL tool:</p>
      <tb-markdown usePlainMarkdown containerClass="tb-getting-started-code" data="
      ```bash
      sudo apt-get install curl
      {:copy-code}
      ```
      "></tb-markdown>
    </ng-template>
    <ng-template [ngSwitchCase]="'macos'">
      <p style="margin-top: 18px; padding: 0 32px">Install cURL tool:</p>
      <tb-markdown usePlainMarkdown containerClass="tb-getting-started-code" data="
      ```bash
      brew install curl
      {:copy-code}
      ```
      "></tb-markdown>
    </ng-template>
    <ng-template [ngSwitchCase]="'windows'">
      <div style="margin-top: 18px; padding: 0 32px; width: 404px;">Starting Windows 10 b17063, cURL is available by default.</div>
    </ng-template>
  </ng-container>
</section>
