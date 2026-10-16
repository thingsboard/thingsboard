# thingsboard-web-report
Report service for generating ThingsBoard reports

The service renders reports with **Google Chrome installed on the system**; it does not ship its own browser.
By default it uses Chrome at its standard location:

| OS | Default executable |
|---|---|
| Linux | `/usr/bin/google-chrome-stable` |
| Windows | `%ProgramFiles%\Google\Chrome\Application\chrome.exe` |
| macOS (development only) | `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome` |

To use another Chrome or Chromium executable, set `CHROME_EXECUTABLE` (see [Using another browser](#using-another-browser)).
The service logs the browser path and version at startup, and fails to start if the browser is missing.

## Linux

The deb and rpm packages depend on `google-chrome-stable`. Install Chrome first: its package also adds Google's repository,
so Chrome is updated together with the rest of the system.

- Debian / Ubuntu:

```
wget https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb
sudo apt install ./google-chrome-stable_current_amd64.deb
sudo apt install ./tb-web-report.deb
```

- Rocky Linux / AlmaLinux / RHEL:

```
sudo dnf install https://dl.google.com/linux/direct/google-chrome-stable_current_x86_64.rpm
sudo dnf install google-roboto-fonts google-noto-sans-cjk-ttc-fonts google-noto-emoji-color-fonts liberation-fonts
sudo dnf install ./tb-web-report.rpm
```

Install the service with `apt` / `dnf` as shown, not with `dpkg -i` / `rpm -i`, so that dependencies are checked.
Without Chrome the installation stops with an unmet `google-chrome-stable` dependency.

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

Install Google Chrome **for all users** (under `Program Files`): the service runs as LocalSystem and does not see per-user installs.
`install.bat` prints a warning if Chrome is not found at the default location.

## Docker

The image contains Debian's `chromium` (available for both `amd64` and `arm64`) and sets `CHROME_EXECUTABLE=/usr/bin/chromium`.
Nothing needs to be installed.

## Using another browser

`CHROME_EXECUTABLE` overrides the default path. It must point to an existing executable; otherwise the service fails to start.

- **Linux:** uncomment and set `CHROME_EXECUTABLE` in `/etc/tb-web-report/conf/tb-web-report.conf`, then restart the service.
- **Windows:** uncomment the `CHROME_EXECUTABLE` line in `tb-web-report.xml` next to `install.bat`, set the path, then restart the service.

Only Google Chrome is supported. Other Chromium builds usually work, but are not tested. Ubuntu's snap `chromium` cannot be
started by a system service. Microsoft Edge is not supported.

## Chrome updates

Chrome is updated by the operating system, independently of the service. A running service keeps using the browser it
started and picks up the new version on its next restart (on Ubuntu, `needrestart` restarts it automatically after the
update). Rendering is not pixel-identical across Chrome and font versions. If reports break after a Chrome update, upgrade
the service, or keep the previous Chrome version (`sudo apt-mark hold google-chrome-stable`, or `dnf versionlock` on
RHEL-like systems) until you can.

The deb and rpm packages depend on `google-chrome-stable`, so removing Chrome with `apt`/`dnf` also removes `tb-web-report`.

## Upgrading from versions with bundled Chromium

Previous versions shipped Chromium in `/usr/share/tb-web-report/chromium` and pointed `CHROME_EXECUTABLE` to it.

- Install Google Chrome before upgrading; the upgrade stops otherwise and the previous version keeps running.
- If you kept a modified `tb-web-report.conf` during the upgrade, it still contains the old Chromium path. The service ignores
  this path with a warning and uses Google Chrome; remove the line to silence the warning.
