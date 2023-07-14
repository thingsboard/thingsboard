 #### CoAP installation instructions
---
<br />

Install coap client tool on your **Linux/macOS**:

```bash
git clone https://github.com/obgm/libcoap --recursive
{:copy-code}
```
<br />

```bash
cd libcoap
{:copy-code}
```
<br />

```bash
./autogen.sh
{:copy-code}
```
<br />

```bash
./configure --with-openssl --disable-doxygen --disable-manpages --disable-shared
{:copy-code}
```
<br />

```bash
make
{:copy-code}
```
<br />

```bash
sudo make install
{:copy-code}
```
