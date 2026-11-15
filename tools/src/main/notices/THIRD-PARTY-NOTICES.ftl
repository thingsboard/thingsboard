[#ftl]
[#--
    SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
    SPDX-License-Identifier: BUSL-1.1

    Third-party notices of ThingsBoard, grouped by license.

    Each section names one license, lists the packages under it with their copyright statements, and prints the
    license text once. Packages under EPL, CDDL, MPL or APSL get a "Source code:" line. Packages under GPL or LGPL are
    covered by the written offer at the end.
--]
[#-- Readable titles for licenses that have no SPDX identifier and carry a scanner label instead. --]
[#assign titles = {
    "LicenseRef-scancode-unicode": "Unicode License Agreement for Data Files and Software",
    "LicenseRef-scancode-unicode-icu-58": "Unicode License (ICU 58 and later)",
    "LicenseRef-scancode-red-hat-attribution": "Red Hat attribution notice",
    "LicenseRef-scancode-paulo-soares": "Paulo Soares font metrics notice",
    "LicenseRef-scancode-w3c-docs-20021231": "W3C Document License (2002-12-31)",
    "LicenseRef-scancode-iso-8879": "ISO 8879 character entity sets notice",
    "LicenseRef-scancode-google-patent-license-webm": "Google additional patent grant for WebM",
    "LicenseRef-scancode-adobe-postscript": "Adobe PostScript mapping files notice",
    "LicenseRef-scancode-amazon-sl": "Amazon Software License"
}]
[#function title name][#return titles[name]!name][/#function]
[#-- Scanner labels that are not licenses: pointer sentences (NOASSERTION) and public-domain statements, which carry no obligation. --]
[#function dropped name][#return name == "NONE" || name?starts_with("NOASSERTION") || name?starts_with("LicenseRef-scancode-public-domain") || name == "CC-PDDC"][/#function]
[#assign view = LicenseView.CONCLUDED_OR_DECLARED_AND_DETECTED]
[#assign shipped = packages?filter(p -> !p.excluded)]
[#assign rule = "=============================================================================="]
[#function isSourceLink name][#return name?starts_with("EPL-") || name?starts_with("CDDL-") || name?starts_with("MPL-") || name?starts_with("APSL-")][/#function]
[#function isOffer name][#return name?starts_with("GPL-") || name?starts_with("LGPL-")][/#function]
[#-- The licenses of a package that the notices list. --]
[#function licensesOf package][#return view.filter(package.license.filterExcluded(), package.licenseChoices).licenses?filter(l -> !dropped(l.license?string))][/#function]
[#function coordinates id][#return (id.namespace?has_content)?then(id.namespace + ":", "") + id.name + " " + id.version][/#function]
[#-- Count the packages of every license. --]
[#assign names = []]
[#assign counts = {}]
[#list shipped as package]
    [#list licensesOf(package) as resolved]
        [#assign name = resolved.license?string]
        [#if counts[name]??]
            [#assign counts = counts + {name: counts[name] + 1}]
        [#else]
            [#assign names = names + [name]]
            [#assign counts = counts + {name: 1}]
        [/#if]
    [/#list]
[/#list]
[#assign sections = []]
[#list names as name][#assign sections = sections + [{"name": name, "count": counts[name]}]][/#list]
[#-- The licenses with the most components first, and those with as many by name. --]
[#assign sections = sections?sort_by("name")?reverse?sort_by("count")?reverse]
THIRD-PARTY NOTICES

[#-- Nothing below is printed without content: no introduction or index without components, no heading without entries. --]
[#if sections?has_content]
ThingsBoard contains the third-party software components listed below.
The components are grouped by license. Each section lists the components under
that license with their copyright notices, followed by the license text.

Licenses in this file:

[#list sections as section]
  ${title(section.name)} (${section.count} ${(section.count == 1)?then("component", "components")})
[/#list]

[/#if]
[#list sections as section]
${rule}
${title(section.name)}
${rule}

The following components are licensed under ${title(section.name)}:

    [#assign text = ""]
    [#assign exceptionText = ""]
    [#list shipped as package]
        [#list licensesOf(package) as resolved]
            [#if resolved.license?string != section.name][#continue][/#if]
            [#if !text?has_content]
                [#assign text = licenseFactProvider.getLicenseText(resolved.license.simpleLicense())!""]
                [#assign exceptionText = licenseFactProvider.getLicenseText(resolved.license.exception()!"")!""]
            [/#if]
  ${coordinates(package.id)}
            [#list resolved.getCopyrights(true) as copyright]
      ${copyright}
            [/#list]
            [#if isSourceLink(section.name)]
                [#assign source = (ortResult.getPackage(package.id).metadata.sourceArtifact.url)!""]
                [#if !source?has_content][#assign source = (ortResult.getPackage(package.id).metadata.vcsProcessed.url)!""][/#if]
                [#if source?has_content]
      Source code: ${source}
                [/#if]
            [/#if]
            [#if isOffer(section.name)]
      Source code: see "Written offer for source code" at the end of this file
            [/#if]
        [/#list]
    [/#list]

    [#if text?has_content]
${text}
        [#if exceptionText?has_content]

${exceptionText}
        [/#if]
    [#else]
(The text of this license is not available to the notice generator.)
    [/#if]

[/#list]
[#-- The written offer is printed only when at least one component needs it. --]
[#assign offered = []]
[#list shipped as package]
    [#list licensesOf(package) as resolved]
        [#if isOffer(resolved.license?string)]
            [#assign offered = offered + [coordinates(package.id) + " (" + resolved.license?string + ")"]]
        [/#if]
    [/#list]
[/#list]
[#if offered?has_content]
${rule}
Written offer for source code
${rule}

This product contains the software components listed below, which are licensed
under the GNU General Public License version 2 with the Classpath Exception or
under the GNU Lesser General Public License.

ThingsBoard, Inc. will give any third party a complete machine-readable copy of
the corresponding source code of these components, on a medium customarily used
for software interchange, for a charge no more than our cost of physically
performing the distribution.

This offer is valid for three years from the date on which we last distributed
this version of the product.

To request the source code, write to legal@thingsboard.io and state the product
name, the product version and the component.

Components covered by this offer:

[#list offered as line]
  ${line}
[/#list]
[/#if]
