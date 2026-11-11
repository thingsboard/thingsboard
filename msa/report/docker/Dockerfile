#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

FROM ${docker.base.image}

# For tb-report PDF renderer Debian Trixie slim fonts should be installed explicitly
# Current fonts libraries:
# ii  fontconfig                    2.14.1-4               amd64        generic font configuration library - support binaries
# ii  fontconfig-config             2.14.1-4               amd64        generic font configuration library - configuration
# ii  fonts-dejavu-core             2.37-6                 all          Vera font family derivate with additional characters
# ii  libfontconfig1:amd64          2.14.1-4               amd64        generic font configuration library - runtime
# ii  libfreetype6:amd64            2.12.1+dfsg-5+deb12u4  amd64        FreeType 2 font engine, shared library files
# ii  libharfbuzz0b:amd64           6.0.0+dfsg-3           amd64        OpenType text shaping engine (shared library)
# Font config list should looks like:
# /usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf: DejaVu Serif:style=Bold
# /usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf: DejaVu Sans Mono:style=Book
# /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf: DejaVu Sans:style=Book
# /usr/share/fonts/truetype/dejavu/DejaVuSansMono-Oblique.ttf: DejaVu Sans Mono:style=Oblique
# /usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf: DejaVu Sans:style=Bold
# /usr/share/fonts/truetype/dejavu/DejaVuSansMono-BoldOblique.ttf: DejaVu Sans Mono:style=Bold Oblique
# /usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf: DejaVu Sans Mono:style=Bold
# /usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf: DejaVu Serif:style=Book
# Harfbuzz library test with ldconfig:
#        libharfbuzz.so.0 (libc6,x86-64) => /lib/x86_64-linux-gnu/libharfbuzz.so.0
RUN echo "Previously installed font libraries:" \
 && (dpkg -l | grep -E 'font|dejavu|harfbuzz' || echo "  (none)") \
 && apt-get update \
 && apt-get install -y --no-install-recommends \
      libharfbuzz0b fontconfig fonts-dejavu-core \
 && rm -rf /var/lib/apt/lists/* \
 && echo "Current fonts libraries:" \
 && (dpkg -l | grep -E 'font|dejavu|harfbuzz') \
 && echo "Font config list:" \
 && fc-list && test -n "$(fc-list)" \
 && (fc-list | grep -i dejavu) \
 && echo "Harfbuzz library test with ldconfig:" \
 && (ldconfig -p | grep harfbuzz)

COPY start-tb-report.sh ${pkg.name}.deb /tmp/

RUN chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb-report.sh /usr/bin && \
    (yes | dpkg -i /tmp/${pkg.name}.deb) && \
    rm /tmp/${pkg.name}.deb && \
    (systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :) && \
    chown -R ${pkg.user}:${pkg.user} /tmp && \
    chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

CMD ["start-tb-report.sh"]