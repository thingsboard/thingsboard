# thingsboard-web-report
Report service for generating ThingsBoard reports

The deb, rpm and Windows packages render reports with the **browser installed on the system**; they do not ship their own.
The Docker image keeps shipping the Chromium build that matches the bundled Playwright.

At startup the service picks the first browser that launches:

1. `CHROME_EXECUTABLE`, if set and the file exists (see [Using another browser](#using-another-browser)).
2. Google Chrome at its standard location, resolved by Playwright (`channel: 'chrome'`): `/opt/google/chrome/chrome` on Linux,
   `Program Files`, `Program Files (x86)` or the per-user install on Windows, `/Applications/Google Chrome.app` on macOS.
3. Linux only: distro Chromium, `/usr/bin/chromium` or `/usr/bin/chromium-browser`.

It then checks the browser before accepting requests, and fails to start with a clear error otherwise:

- the Chrome major version must be at least one below the Chromium the bundled `playwright-core` was built against
  (the Stable Chrome on the day of that Playwright release); there is no upper bound;
- the browser must render a test PDF, so a newer Chrome that broke compatibility is caught at startup rather than on the first report.

The selected browser and its version are logged.

## Linux

The deb package depends on `google-chrome-stable | chromium (>= 1)`, the rpm package on `(google-chrome-stable or chromium)`
(RHEL 8+). Either browser satisfies the dependency. The version in the deb dependency excludes Ubuntu's `chromium-browser`:
it is a snap transitional package that only provides `chromium` without a version, and a snap browser cannot be started
by a system service. On Ubuntu, install Google Chrome.

- Debian / Ubuntu, with Google Chrome (its package also adds Google's repository, so Chrome is updated with the system):

```
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo apt install ./google-chrome-stable_current_amd64.deb
sudo apt install ./tb-web-report.deb
```

  On Debian, `sudo apt install ./tb-web-report.deb` alone installs the distro `chromium` instead.

- Rocky Linux / AlmaLinux / RHEL, with Google Chrome (or `chromium` from EPEL):

```
sudo dnf install https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
sudo dnf install google-roboto-fonts google-noto-sans-cjk-ttc-fonts google-noto-emoji-color-fonts liberation-fonts
sudo dnf install ./tb-web-report.rpm
```

Install the service with `apt` / `dnf` as shown, not with `dpkg -i` / `rpm -i`, so that dependencies are checked.

### Fonts

For correct Roboto, CJK and emoji output the following fonts are recommended. The deb package lists them as `Recommends`,
so `apt` installs them by default (unless `--no-install-recommends` is used); on RHEL-like systems install them as shown above.

| Debian / Ubuntu | Rocky / Alma |
|---|---|
| `fonts-roboto` | `google-roboto-fonts` |
| `fonts-noto-cjk` | `google-noto-sans-cjk-ttc-fonts` |
| `fonts-noto-color-emoji` | `google-noto-emoji-color-fonts` |
| `fonts-liberation` | `liberation-fonts` |

## Windows

Install Google Chrome before starting the service. The service runs as LocalSystem, so prefer an install for all users.

## Docker

The image is published for `linux/amd64` and `linux/arm64`. It contains the Chromium build that matches the bundled
`playwright-core`, installed for the image platform, and sets `CHROME_EXECUTABLE` to it. Nothing needs to be installed.

## Using another browser

`CHROME_EXECUTABLE` points the service to another Chrome or Chromium executable. If the file does not exist, the service
logs a warning and falls back to the browsers listed above.

- **Linux:** uncomment and set `CHROME_EXECUTABLE` in `/etc/tb-web-report/conf/tb-web-report.conf`, then restart the service.
- **Windows:** add `<env name="CHROME_EXECUTABLE" value="C:\path\to\chrome.exe" />` to `tb-web-report.xml` next to
  `install.bat`, then restart the service.

Microsoft Edge is not supported.

## Browser updates

The system browser is updated by the operating system, independently of the service. A running service keeps using the
browser it started and picks up the new version on its next restart (on Ubuntu, `needrestart` restarts it automatically
after the update), where the startup check runs again. Keep the browser updated: it executes widget JavaScript, so it
should not run without security updates. Rendering is not pixel-identical across browser and font versions.

## Upgrading from versions with bundled Chromium

Previous versions shipped Chromium in `/usr/share/tb-web-report/chromium` and pointed `CHROME_EXECUTABLE` to it.

- Install Google Chrome (or distro Chromium) before upgrading.
- If you kept a modified `tb-web-report.conf` during the upgrade, it still points to the old Chromium path. The service
  logs a warning and falls back to the system browser; remove the line to silence the warning.
