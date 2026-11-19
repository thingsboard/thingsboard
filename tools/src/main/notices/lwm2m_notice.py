#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Makes the license file of the LwM2M object models that ThingsBoard ships, from the headers of the models.

The models come from the OMNA LwM2M Registry, and each one declares its license in a comment before its first element:
the copyright notices and the text of the license. The file groups the models by the text of their license: each text
once, under its copyright notices and the models that each notice is in. The notices take it as it is: see
license-files-here.json.

What the registry adds before the license in its own models, about the file and the specification, is left out. A text
that is not one of the licenses below stops the script, and so does a license that the SBOM of the models does not give,
in the bundled component lwm2m-registry of application/pom.xml.

Usage: lwm2m_notice.py           write the file
       lwm2m_notice.py --check   check that the file is what the models give, for the notices
"""
import argparse
import os
import re
import sys
import textwrap
import xml.etree.ElementTree as ElementTree

from sbom_to_ort import stop, write_atomically

ROOT = os.path.realpath(os.path.join(os.path.dirname(__file__), '..', '..', '..', '..'))
MODELS = 'application/src/main/data/lwm2m-registry'
NOTICE = f'{MODELS}/NOTICE.txt'
POM = 'application/pom.xml'
COMPONENT = 'lwm2m-registry'

COPYRIGHT = re.compile(r'^\s*Copyright\s*(\(c\)|©)?\s*\d', re.IGNORECASE)
INTRODUCTION = '''\
LwM2M object models from the OMNA LwM2M Registry,
https://github.com/OpenMobileAlliance/lwm2m-registry

Each model declares its license in its header. The headers are grouped below
by the text of their license: each text once, under its copyright notices and
the models that each notice is in.'''


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true', help='check the file instead of writing it')
    args = parser.parse_args()

    groups, without = read(os.path.join(ROOT, MODELS))
    check_sbom({spdx for spdx, _ in groups.values()}, os.path.join(ROOT, POM))
    text = notice(groups)
    path = os.path.join(ROOT, NOTICE)
    if args.check:
        if not os.path.exists(path) or open(path, encoding='utf-8').read() != text:
            raise stop(f'{NOTICE} is not what the headers of the LwM2M object models give. Write it again with '
                       'python3 tools/src/main/notices/lwm2m_notice.py and commit it with the models.')
        return
    write_atomically(path, text.encode())
    models = sum(len(files) for _, (_, files) in groups.items())
    print(f'{NOTICE}: {models} models, {len(groups)} license texts')
    if without:
        print(f'Without a license in the header, so not in the file: {", ".join(without)}')


def read(directory):
    """The models by the text of their license, as {text: (SPDX identifier, {file: (copyright notices, text)})}, and the models
    without a license in their header."""
    groups, without = {}, []
    for name in sorted((name for name in os.listdir(directory) if name.endswith('.xml')), key=order):
        found = header(open(os.path.join(directory, name), 'rb').read().decode('utf-8-sig'))
        if not found:
            without.append(name)
            continue
        copyrights, text = found
        key = ' '.join(text.split())
        if key not in groups:
            groups[key] = (identify(text, name), {})
        groups[key][1][name] = (copyrights, text)
    return groups, without


def order(name):
    """Models by their number: 2.xml before 10.xml."""
    stem = name.removesuffix('.xml')
    return (0, int(stem), '') if stem.isdigit() else (1, 0, stem)


def header(content):
    """The copyright notices and the text of the license in the comment before the first element, or None."""
    content = content.replace('\r\n', '\n').replace('\r', '\n')
    # 507.xml and 508.xml end their comment with !-->
    comment = re.match(r'\s*(<\?xml[^>]*\?>)?\s*<!--(.*?)!?-->', content, re.DOTALL)
    if not comment:
        return None
    lines = comment.group(2).split('\n')
    # What follows <!-- on its line is not indented like the lines below it
    lines[0] = lines[0].lstrip()
    # The models of the registry tell about the file and its specification first, and then give the license
    disclaimer = [i for i, line in enumerate(lines) if line.strip() == 'LEGAL DISCLAIMER']
    if disclaimer:
        lines = lines[disclaimer[0] + 1:]
    copyrights = [' '.join(line.split()) for line in lines if COPYRIGHT.match(line)]
    text = textwrap.dedent('\n'.join(line.rstrip() for line in lines if not COPYRIGHT.match(line))).strip('\n')
    text = re.sub(r'\n{3,}', '\n\n', text)
    return (copyrights, text) if text else None


def identify(text, name):
    """The SPDX identifier of the license that the text is."""
    words = ' '.join(text.split())
    if 'Apache License' in words and 'Version 2.0' in words:
        return 'Apache-2.0'
    if 'Permission is hereby granted, free of charge' in words:
        return 'MIT'
    if 'Redistribution and use in source and binary forms' in words:
        return 'BSD-3-Clause' if 'Neither the name' in words else 'BSD-2-Clause'
    raise stop(f'the header of the LwM2M object model {name} is not a license that lwm2m_notice.py knows: Apache-2.0, '
               'MIT, BSD-2-Clause or BSD-3-Clause. Find out which license it is, add it to identify() in '
               'lwm2m_notice.py and to the license of the component lwm2m-registry in application/pom.xml.')


def check_sbom(licenses, pom):
    """The license of the bundled component lwm2m-registry must name every license of the models, and only those."""
    namespace = {'m': 'http://maven.apache.org/POM/4.0.0'}
    expressions = [component.findtext('m:license', '', namespace)
                   for component in ElementTree.parse(pom).getroot().iterfind('.//m:component', namespace)
                   if component.findtext('m:name', '', namespace) == COMPONENT]
    if len(expressions) != 1:
        raise stop(f'{POM} has {len(expressions)} bundled components {COMPONENT}, and lwm2m_notice.py reads the '
                   'license of the one.')
    given = set(expressions[0].split(' AND ')) if expressions[0] else set()
    if given != licenses:
        expected = ' AND '.join(sorted(licenses))
        raise stop(f'the license of the bundled component {COMPONENT} in {POM} is "{expressions[0]}", and the headers '
                   f'of the models give {expected}. Set it to "{expected}".')


def notice(groups):
    """The text of the file: the groups with the most models first."""
    blocks = [INTRODUCTION]
    for spdx, models in sorted(groups.values(), key=lambda group: (-len(group[1]), order(next(iter(group[1]))))):
        title = f'{spdx} ({len(models)} {"model" if len(models) == 1 else "models"})'
        holders = {}
        for name, (copyrights, _) in models.items():
            for line in copyrights:
                holders.setdefault(line, []).append(name)
        lines = [title, '~' * len(title), '']
        for line, names in holders.items():
            lines.append(line)
            lines += textwrap.wrap(', '.join(names), 76, initial_indent='    ', subsequent_indent='    ',
                                   break_on_hyphens=False)
        # The text as the first of the models has it: the others differ from it only in spaces and line breaks
        lines += ['', next(iter(models.values()))[1]]
        blocks.append('\n'.join(lines))
    return '\n\n\n'.join(blocks) + '\n'


if __name__ == '__main__':
    sys.exit(main())
