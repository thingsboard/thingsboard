# Chromium credits

`CREDITS.txt` lists the third-party software in the Chromium builds that the web-report packages bundle (see
`../chromium.js`), with its license texts. It is what Chromium shows on its page about:credits. The third-party notices
take it as the license file of the chromium components, through `tools/src/main/notices/license-files-here.json`:

- the notices of the Linux packages get the Linux credits;
- those of the Windows package get the Windows credits;
- the root `THIRD-PARTY-NOTICES` gets the whole file.

The Linux and Windows builds have nearly the same entries: in 156.0.8078.0, all but 9. Those of one build are in a
section at the end of the file, under a line `======== Only in the <platform> build ========`, which the notices of the
other platform leave out.

The file is made by the command below. It is not a script of the build: it runs only when Chromium is updated.

## Why we keep the credits

The snapshot builds that the packages bundle embed only a placeholder about:credits. Chromium generates the real page
only when `generate_about_credits` is true, and that defaults to `is_official_build`, which the snapshot builders
(`linux-archive-rel`, `win-archive-rel`) do not set. `//components/resources:about_credits` then copies
`components/resources/sample_credits.html`.

## Where the texts come from

The page is made by `tools/licenses/licenses.py credits`, with no GN target. It does not read the build graph: it lists
every `third_party` directory of the checkout whose metadata says `Shipped: yes`. So the page depends only on two things:

- the commit;
- which platform's dependencies the checkout has: `checkout_linux` or `checkout_win`.

It does not depend on the build arguments, so an official build of the same code has the same page.

Chrome for Testing is such an official build, and its `resources.pak` holds the page. For Chromium 156.0.8078.0, both
pages were generated from the source at the exact commits of the snapshots (see "From the Chromium source" below). The
pages of Chrome for Testing 156.0.8078.3 are the same, byte for byte:

| Build      | Snapshot | chromium/src commit                        | Entries | Chrome for Testing 156.0.8078.3 |
|------------|----------|--------------------------------------------|---------|---------------------------------|
| Linux x64  | 1706520  | `23b351114f0570483d619b05db197bbc5594dc5a` | 494     | the same                        |
| Windows x64| 1706509  | `7d9824099030f5c28c939ba08654562faaad7c09` | 489     | the same, with CRLF line ends   |

Chrome for Testing is built a few commits later, on the release branch. Its page could differ only if a third-party
directory was added or removed in those commits. Take the earliest Chrome for Testing version of the release. To be
exact, generate the pages from the source.

Two other sources are not right for these builds:

- The Chromium package of Debian takes 37 libraries from other Debian packages (harfbuzz, libjpeg-turbo, opus, zstd,
  dav1d and others), so its credits leave them out.
- `licenses.py license_file --format txt` writes the same components without merging the entries that the page merges:
  520 entries for Linux and 515 for Windows.

## Why the conversion to text loses nothing

`licenses.py` fills `components/webui/about/resources/about_credits_entry.tmpl` once for each entry, with three
fields: the name, the address of the homepage and the license text. It passes each field through Python's
`html.escape()`, which turns `&`, `<`, `>`, `"` and `'` into `&amp;`, `&lt;`, `&gt;`, `&quot;` and `&#x27;`. Nothing
else is changed.

The command takes the three fields back and undoes those five replacements. It then checks its own work: escaped and
filled in again, the entries must be the page, byte for byte, or the command stops. So the text has all that the page
has.

For 156.0.8078.0 we did a stronger check: the whole pages, rebuilt from the fields with Chromium's own templates, are the
original files byte for byte.

In `CREDITS.txt`, each entry is a line of `=`, the name, the address and the license text. The entries of both builds
come first, in the page's order. The entries of one build follow, in its section.

## Updating

When `../chromium.js` moves to another Chromium version:

1. Find the earliest Chrome for Testing version of that release in
   https://googlechromelabs.github.io/chrome-for-testing/known-good-versions.json.
2. Download its two archives, and unpack `resources.pak` from each:
   ```
   https://storage.googleapis.com/chrome-for-testing-public/<version>/linux64/chrome-linux64.zip   chrome-linux64/resources.pak
   https://storage.googleapis.com/chrome-for-testing-public/<version>/win64/chrome-win64.zip       chrome-win64/resources.pak
   ```
3. Run the command below with the two files and the version of `../chromium.js`, in this directory.
4. Change the version in the two chromium entries of `tools/src/main/notices/license-files-here.json`. Until then, the
   notices stop: an entry of another version would otherwise leave Chromium out of them.
5. Regenerate the third-party notices (see `docs/sbom.md`).

The command needs Node.js, which the web-report module already uses. It also takes two `about_credits.html` files in
place of the `resources.pak` files.

```
node - <Linux credits> <Windows credits> <version> > CREDITS.txt <<'EOF'
const fs = require('fs'), zlib = require('zlib');
const [linux, windows, version] = process.argv.slice(2);

// The credits page: an about_credits.html, or the resource of a resources.pak that holds it
function page(file) {
  const d = fs.readFileSync(file);
  if (d.toString('latin1', 0, 4) === '<!--') return d.toString('utf8');
  for (let i = 0; i < d.readUInt16LE(8); i++) {
    const r = d.subarray(d.readUInt32LE(14 + 6 * i), d.readUInt32LE(20 + 6 * i));
    const t = r[0] === 0x1e && r[1] === 0x9b ? zlib.brotliDecompressSync(r.subarray(8)) : r[0] === 0x1f ? zlib.gunzipSync(r) : r;
    if (t.includes('<!-- Generated by licenses.py')) return t.toString('utf8');
  }
  throw new Error(`no credits page in ${file}`);
}

// licenses.py fills components/webui/about/resources/about_credits_entry.tmpl with each entry, through html.escape()
const ENTRY = '<div class="product">\n<span class="title">{{name}}</span>\n<span class="homepage"><a href="{{url}}">homepage</a></span>\n<label class="show" tabindex="0">\n<input type="checkbox" hidden>\n</label>\n<div class="license">\n<pre>{{license}}</pre>\n</div>\n</div>\n';
const ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#x27;' };
const escape = s => s.replace(/[&<>"']/g, c => ESCAPES[c]);
const unescape = s => s.replace(/&(amp|lt|gt|quot|#x27);/g, e => Object.keys(ESCAPES).find(c => ESCAPES[c] === e));
const [a, b, c, d] = ENTRY.split(/\{\{\w+\}\}/).map(s => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
function entries(file) {
  const html = page(file).replace(/\r\n/g, '\n');
  const list = [...html.matchAll(new RegExp(`${a}(.*?)${b}(.*?)${c}([\\s\\S]*?)${d}`, 'g'))]
    .map(([, name, url, license]) => ({ name: unescape(name), url: unescape(url), license: unescape(license) }));
  // Filled in again, the entries must be the page byte for byte: then the text has all that the page has
  const filled = list.map(e => ENTRY.replace('{{name}}', escape(e.name)).replace('{{url}}', escape(e.url))
    .replace('{{license}}', escape(e.license))).join('\n');
  if (html.substr(html.indexOf('<div class="product">'), filled.length) !== filled
      || html.split('<div class="product">').length - 1 !== list.length) throw new Error(`${file} is not as licenses.py writes it`);
  return list.map(e => `${'='.repeat(80)}\n${e.name}\n${e.url}\n\n${e.license.replace(/\n+$/, '')}\n`);
}

const [l, w] = [entries(linux), entries(windows)];
const only = (these, others, os) => {
  const own = these.filter(e => !others.includes(e));
  return own.length ? [`======== Only in the ${os} build ========\n`, ...own] : [];
};
process.stdout.write([`Chromium ${version}: the third-party software in it, as its page about:credits lists it. Its builds\n`
  + 'for Linux and Windows have the same, except for the sections at the end that name one of them.\n',
  ...l.filter(e => w.includes(e)), ...only(l, w, 'Linux'), ...only(w, l, 'Windows')].join('\n'));
EOF
```

## From the Chromium source

This is the fallback, and the way to be exact: run Chromium's own `licenses.py` at the commits of the snapshots, as the
build would. It does not build Chromium. It needs about 31 GB of disk for a shallow checkout, and a few hours.

1. Find the commit of each snapshot: `got_revision` in the `REVISIONS` file of its directory, such as
   https://commondatastorage.googleapis.com/chromium-browser-snapshots/Linux_x64/1706520/REVISIONS.
2. Work in a throwaway container, in a directory that you delete at the end:
   ```
   mkdir ~/chromium-credits-work
   docker run --rm -it -u "$(id -u):$(id -g)" -e HOME=/work/home -e DEPOT_TOOLS_UPDATE=0 -e DEPOT_TOOLS_METRICS=0 \
     -e PATH=/work/depot_tools:/usr/local/bin:/usr/bin:/bin -v "$HOME/chromium-credits-work:/work" -w /work \
     python:3.12-bookworm bash
   git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git /work/depot_tools
   git config --global --add safe.directory '*'
   ```
3. Check out the Linux commit, shallow, with no hooks, and only the Linux dependencies, as `linux-archive-rel` does:
   ```
   mkdir /work/chromium && cd /work/chromium
   cat > .gclient <<'EOF'
   solutions = [{"name": "src", "url": "https://chromium.googlesource.com/chromium/src.git@<Linux commit>",
                 "managed": False, "custom_deps": {}, "custom_vars": {}}]
   EOF
   gclient sync -D --nohooks --no-history --shallow -j16
   ```
   Dependencies of another platform left on disk change the page: their README.chromium files then find their license
   files.
4. Generate the page as `//components/resources:about_credits` does:
   ```
   cd /work/chromium/src && mkdir -p out/linux/gen/components/resources && cd out/linux
   ../../third_party/cpython3/host/bin/python3 ../../tools/licenses/licenses.py credits --target-os=linux \
     --depfile gen/components/resources/about_credits.d gen/components/resources/about_credits.html
   ```
5. Move the checkout to the Windows commit, with only the Windows dependencies, as on the Windows host of
   `win-archive-rel`. Fetch the commit shallowly first: `gclient sync --revision` on a shallow checkout would download the
   whole history of chromium/src, tens of GB.
   ```
   cd /work/chromium
   cat > .gclient <<'EOF'
   solutions = [{"name": "src", "url": "https://chromium.googlesource.com/chromium/src.git@<Windows commit>",
                 "managed": False, "custom_deps": {}, "custom_vars": {}}]
   target_os = ["win"]
   target_os_only = True
   EOF
   git -C src fetch --no-tags --depth=1 origin <Windows commit>
   git -C src checkout --detach <Windows commit>
   gclient sync -D --nohooks --no-history --shallow -j16
   ```
   The dependencies of the host are still those of Linux, such as `buildtools/linux64`. None of them is a `third_party`
   directory that the page lists.
6. Generate the Windows page as in step 4, in `out/win`, with `--target-os=win`. `gn gen` for Windows does not run on
   Linux without Microsoft's toolchain, and the page does not need it.
7. Run the command above with the two `about_credits.html` files. Then delete `~/chromium-credits-work` and the
   `python:3.12-bookworm` image.
