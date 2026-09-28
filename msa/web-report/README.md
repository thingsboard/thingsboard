# thingsboard-web-report
Report service for generating ThingsBoard reports

## Rocky Linux 9, AlmaLinux 9, Fedora 40

```
sudo yum install pango.x86_64 libXcomposite.x86_64 libXcursor.x86_64 libXdamage.x86_64 libXext.x86_64 libXi.x86_64 libXtst.x86_64 cups-libs.x86_64 libXScrnSaver.x86_64 libXrandr.x86_64 GConf2.x86_64 alsa-lib.x86_64 atk.x86_64 gtk3.x86_64 ipa-gothic-fonts xorg-x11-fonts-100dpi xorg-x11-fonts-75dpi xorg-x11-utils xorg-x11-fonts-cyrillic xorg-x11-fonts-Type1 xorg-x11-fonts-misc unzip nss -y
```
- Enable EPEL:

```
sudo yum install wget -y
```
```
wget http://dl.fedoraproject.org/pub/epel/epel-release-latest-7.noarch.rpm
sudo rpm -ivh epel-release-latest-7.noarch.rpm
```

- Get Roboto fonts:

```
sudo yum install google-roboto-fonts -y
```

- Get Noto fonts (Japanese, Chinese, etc.):

```
sudo yum install google-noto-sans-cjk-ttc-fonts google-noto-serif-cjk-ttc-fonts -y
```

- Install Web Report service:

```
sudo rpm -Uvh [--force] tb-web-report.rpm
```
```
sudo systemctl daemon-reload
```

## Ubuntu 24.04

```
sudo apt install -yq gconf-service libasound2 libatk1.0-0 libc6 libcairo2 libcups2 libdbus-1-3 \
     libexpat1 libfontconfig1 libgcc1 libgconf-2-4 libgdk-pixbuf2.0-0 libglib2.0-0 libgtk-3-0 libnspr4 \
     libpango-1.0-0 libpangocairo-1.0-0 libstdc++6 libx11-6 libx11-xcb1 libxcb1 libxcomposite1 \
     libxcursor1 libxdamage1 libxext6 libxfixes3 libxi6 libxrandr2 libxrender1 libxss1 libxtst6 \
     ca-certificates fonts-liberation libappindicator1 libnss3 lsb-release xdg-utils unzip wget
```

- Get Roboto fonts:

```
sudo apt install fonts-roboto
```

- Get Noto fonts (Japanese, Chinese, etc.):

```
sudo apt install fonts-noto-cjk-extra
```

- Install Web Report service:

```
sudo dpkg -i tb-web-report.deb
```
