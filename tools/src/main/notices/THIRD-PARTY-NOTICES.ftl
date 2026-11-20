[#ftl]
[#--
    SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
    SPDX-License-Identifier: BUSL-1.1

    Third-party notices of ThingsBoard, grouped by license.

    Each section names one license, lists the packages under it with their copyright statements, and prints the
    license text once. ort-config/license-classifications.yml says what else each license gets: a "Source code:" line,
    or the written offer at the end. A license that is not in that file stops the notices.
--]
[#-- Readable titles for licenses that have no SPDX identifier: those that carry a scanner label instead, and those
     whose text is ours to give, in ort-config/custom-license-texts. --]
[#assign titles = {
    "LicenseRef-scancode-unicode": "Unicode License Agreement for Data Files and Software",
    "LicenseRef-scancode-unicode-icu-58": "Unicode License (ICU 58 and later)",
    "LicenseRef-scancode-red-hat-attribution": "Red Hat attribution notice",
    "LicenseRef-scancode-paulo-soares": "Paulo Soares font metrics notice",
    "LicenseRef-scancode-w3c-docs-20021231": "W3C Document License (2002-12-31)",
    "LicenseRef-scancode-iso-8879": "ISO 8879 character entity sets notice",
    "LicenseRef-scancode-google-patent-license-webm": "Google additional patent grant for WebM",
    "LicenseRef-scancode-adobe-postscript": "Adobe PostScript mapping files notice",
    "LicenseRef-thingsboard-beerware-googlemutant": "Beerware (leaflet.gridlayer.googlemutant)"
}]
[#function title name][#return titles[name]!name][/#function]
[#assign view = LicenseView.CONCLUDED_OR_DECLARED_AND_DETECTED]
[#assign shipped = packages?filter(p -> !p.excluded)]
[#assign rule = "=============================================================================="]
[#function inCategory resolved category][#return helper.filterForCategory([resolved], category)?has_content][/#function]
[#function isSourceLink resolved][#return inCategory(resolved, "source-link")][/#function]
[#function isOffer resolved][#return inCategory(resolved, "written-offer")][/#function]
[#function isForbidden resolved][#return inCategory(resolved, "forbidden")][/#function]
[#-- Every license of a package, without what is no license: NONE, and NOASSERTION, which the scanner gives for a
     sentence that points to a license elsewhere. --]
[#function allLicenses package][#return view.filter(package.license.filterExcluded(), package.licenseChoices).licenses?filter(l -> l.license?string != "NONE" && !l.license?string?starts_with("NOASSERTION"))][/#function]
[#-- The licenses of a package that the notices list: all but those of the category "nothing". --]
[#function licensesOf package][#return allLicenses(package)?filter(l -> !inCategory(l, "nothing"))][/#function]
[#-- The copyright statements of a package with a concluded license that the scan found next to a license the package
     does not have. The scan misread the license of the file; the statement is still one of the package, and nothing
     says which of its licenses it goes with. --]
[#function otherCopyrights package]
    [#if !(package.license.licenseInfo.concludedLicenseInfo.concludedLicense)??][#return []][/#if]
    [#local listed = []]
    [#list allLicenses(package) as resolved][#local listed = listed + resolved.getCopyrights(true)][/#list]
    [#local others = []]
    [#list package.license.filterExcluded().licenses as resolved]
        [#local others = others + resolved.getCopyrights(true)?filter(c -> !listed?seq_contains(c) && !others?seq_contains(c))]
    [/#list]
    [#return others?sort]
[/#function]
[#-- The packages whose other copyright statements are printed already: they are printed once, where the package is
     listed first. --]
[#assign withOthers = []]
[#function coordinates id][#return (id.namespace?has_content)?then(id.namespace + ":", "") + id.name + " " + id.version][/#function]
[#import "stop.ftl" as stop]
[#-- A license ThingsBoard cannot ship under: no entry in a decision file makes it acceptable. --]
[#assign forbidden = []]
[#list shipped as package]
    [#list allLicenses(package)?filter(isForbidden) as resolved]
        [#assign forbidden = forbidden + [resolved.license?string + " (" + package.id.toCoordinates() + ")"]]
    [/#list]
[/#list]
[#if forbidden?has_content]
    [#stop stop.message("these packages are under licenses that ThingsBoard cannot ship.\n"
        + stop.lines(forbidden) + "\n"
        + "Strong copyleft would put ThingsBoard itself under that license, and the others forbid commercial use.\n"
        + "No change to the configuration makes them acceptable: remove the dependency, or replace it with one\n"
        + "under another license. Only if the scan misread the license, and the package is not under it, add a\n"
        + "concluded license to tools/src/main/notices/ort-config/curations/curations.json, with where you\n"
        + "checked, for the versions you checked. The owners of that file have to approve it.")]
[/#if]
[#-- Nobody has said what the notices owe for a license that is not in license-classifications.yml. --]
[#assign unclassified = []]
[#list shipped as package]
    [#list allLicenses(package) as resolved]
        [#if !helper.filterForClassified([resolved])?has_content]
            [#assign unclassified = unclassified + [resolved.license?string + " (" + package.id.toCoordinates() + ")"]]
        [/#if]
    [/#list]
[/#list]
[#if unclassified?has_content]
    [#stop stop.message("these licenses are new to the product, and nobody has said what they require of us.\n"
        + stop.lines(unclassified) + "\n"
        + "We must know what a license obliges us to ship before we ship it.\n"
        + "To fix: add the license to tools/src/main/notices/ort-config/license-classifications.yml\nunder one category:\n"
        + "  notice         the copyright lines and the license text (most permissive licenses: MIT, BSD, Apache)\n"
        + "  source-link    as notice, and where the source is (EPL, MPL, CDDL)\n"
        + "  written-offer  as notice, and our offer of the source (GPL and LGPL with the conditions there)\n"
        + "  nothing        no obligation (public domain)\n"
        + "  forbidden      we cannot ship it (strong copyleft such as GPL or AGPL, no commercial use)\n"
        + "If you are not sure which, ask before adding it: a copyleft license may need legal review.")]
[/#if]
[#-- A package without any license would not be in the notices, and nobody would know. --]
[#assign unlicensed = shipped?filter(p -> !allLicenses(p)?has_content)?map(p -> p.id.toCoordinates())]
[#if unlicensed?has_content]
    [#stop stop.message("no license was found for these packages, so they would be left out of the notices silently.\n"
        + stop.lines(unlicensed) + "\n"
        + "To fix: find the license yourself (its repository, its README, its package page). Then add to\n"
        + "tools/src/main/notices/ort-config/curations/curations.json:\n"
        + "  {\"id\": \"" + unlicensed[0] + "\",\n"
        + "   \"curations\": {\"comment\": \"<where you found it>\", \"concluded_license\": \"<identifier>\"}}\n"
        + "The id may give a range of versions instead, such as [1.0,2.0[, if you checked them all.\n"
        + "If it truly has no license, we may not be allowed to ship it: raise it before merging.")]
[/#if]
[#-- A choice that license-choices.yml does not decide is still in the license of the package after the choices. ORT
     would list the package under every license of the choice, as if all of them applied. --]
[#assign undecided = []]
[#list shipped as package]
    [#assign effective = (package.license.filterExcluded().effectiveLicense(view, package.licenseChoices)?string)!""]
    [#if effective?contains(" OR ")]
        [#assign undecided = undecided + [{"id": package.id.toCoordinates(), "given": effective}]]
    [/#if]
[/#list]
[#if undecided?has_content]
    [#stop stop.message("these packages let us choose a license, and nobody has chosen.\n"
        + stop.lines(undecided?map(u -> u.id + ": " + u.given)) + "\n"
        + "To fix: add the choice to tools/src/main/notices/license-choices.yml. We take the most permissive one,\n"
        + "and of copyleft licenses EPL, CDDL or MPL before LGPL or GPL:\n"
        + "    - given: \"" + undecided[0].given + "\"\n"
        + "      choice: \"<the license we take>\"\n"
        + "The rule then applies to every package that offers the same choice.")]
[/#if]
[#-- ThingsBoard is derived from ThingsBoard Community Edition, which is under Apache-2.0, so every notices file has the
     section of Apache-2.0 and opens it with that, with or without components under it. --]
[#assign ce = "Apache-2.0"]
[#-- Count the packages of every license. --]
[#assign names = [ce]]
[#assign counts = {ce: 0}]
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

ThingsBoard contains the third-party software components listed below.
The components are grouped by license. Each section lists the components under
that license with their copyright notices, followed by the license text.

Licenses in this file:

[#list sections as section]
  ${title(section.name)}[#if section.count > 0] (${section.count} ${(section.count == 1)?then("component", "components")})[/#if]
[/#list]

[#list sections as section]
${rule}
${title(section.name)}
${rule}

    [#if section.name != ce]
The following components are licensed under ${title(section.name)}:

    [#else]
ThingsBoard is derived from ThingsBoard Community Edition, Copyright The
ThingsBoard Authors, which is licensed under Apache-2.0.[#if section.count > 0] The components below
are licensed under Apache-2.0 as well:
[/#if]

    [/#if]
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
            [#if !withOthers?seq_contains(package.id) && otherCopyrights(package)?has_content]
                [#assign withOthers = withOthers + [package.id]]
      ${resolved.getCopyrights(true)?has_content?then("Other copyright", "Copyright")} notices in this component:
                [#list otherCopyrights(package) as copyright]
          ${copyright}
                [/#list]
            [/#if]
            [#if isSourceLink(resolved)]
                [#assign source = (ortResult.getPackage(package.id).metadata.sourceArtifact.url)!""]
                [#if !source?has_content][#assign source = (ortResult.getPackage(package.id).metadata.vcsProcessed.url)!""][/#if]
                [#if source?has_content]
      Source code: ${source}
                [/#if]
            [/#if]
            [#if isOffer(resolved)]
      Source code: see "Written offer for source code" at the end of this file
            [/#if]
        [/#list]
    [/#list]
    [#-- A section without components, which only that of Apache-2.0 can be, has no package to take the text from. --]
    [#if !text?has_content][#assign text = licenseFactProvider.getLicenseText(section.name)!""][/#if]

    [#if text?has_content]
${text}
        [#if exceptionText?has_content]

${exceptionText}
        [/#if]
    [#else]
(The text of this license is not available to the notice generator.)
    [/#if]

[/#list]
[#-- The license files of the components that are not scanned, as license_files.py lays them out. --]
[#assign licenseFiles][#include "license-files.txt" parse=false][/#assign]
[#if licenseFiles?has_content]
${rule}
License files of the components
${rule}

Some components come with a license file of their own, which covers them and
the software they contain. Each file below is given as it is, once, under the
components it belongs to and where it was taken from: an address, or a file of
the ThingsBoard source.

${licenseFiles}
[/#if]
[#-- The NOTICE files of the components, as notice_files.py lays them out. --]
[#assign noticeFiles][#include "notice-files.txt" parse=false][/#assign]
[#if noticeFiles?has_content]
${rule}
NOTICE files of the components
${rule}

Some components come with a NOTICE file. Each text below is given as it is in
the component, once, under the components that carry it and the name of the
file in each.

${noticeFiles}
[/#if]
[#-- The written offer is printed only when at least one component needs it. --]
[#assign offered = []]
[#list shipped as package]
    [#list licensesOf(package) as resolved]
        [#if isOffer(resolved)]
            [#assign offered = offered + [coordinates(package.id) + " (" + resolved.license?string + ")"]]
        [/#if]
    [/#list]
[/#list]
[#if offered?has_content]
${rule}
Written offer for source code
${rule}

This product contains the software components listed below, each under the
license shown beside it.

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
