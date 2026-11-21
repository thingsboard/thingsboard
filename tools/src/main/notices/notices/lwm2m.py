#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

"""Generates the LwM2M registry license notice on the fly from the XML model headers.

Also verifies that the bundled component `lwm2m-registry` in application/pom.xml declares the
exact set of licenses present in the model headers, so no generated NOTICE.txt needs to be
committed to Git or checked separately.
"""
import os
import re
import textwrap
import xml.etree.ElementTree as ElementTree

from notices import ROOT, stop
from notices.scanner import clean_text

MODELS_REL = 'application/src/main/data/lwm2m-registry'
POM_REL = 'application/pom.xml'
COMPONENT_NAME = 'lwm2m-registry'

COPYRIGHT_RE = re.compile(r'^\s*Copyright\s*(\(c\)|©)?\s*\d', re.IGNORECASE)
INTRODUCTION = '''\
LwM2M object models from the OMNA LwM2M Registry,
https://github.com/OpenMobileAlliance/lwm2m-registry

Each model declares its license in its header. The headers are grouped below
by the text of their license: each text once, under its copyright notices and
the models that each notice is in.'''


def generate_lwm2m_notice(root=ROOT):
    """Generates the LwM2M registry notice text and checks application/pom.xml matches."""
    models_dir = os.path.join(root, MODELS_REL)
    pom_path = os.path.join(root, POM_REL)
    groups, _ = read_models(models_dir)
    check_pom_license({spdx for spdx, _, _ in groups.values()}, pom_path)
    return format_notice(groups), MODELS_REL


def read_models(directory):
    """Groups model XML headers by normalized license text."""
    groups, without = {}, []
    for name in sorted((n for n in os.listdir(directory) if n.endswith('.xml')), key=_model_order):
        with open(os.path.join(directory, name), 'rb') as file:
            found = parse_header(clean_text(file.read()))
        if not found:
            without.append(name)
            continue
        copyrights, text = found
        key = ' '.join(text.split())
        if key not in groups:
            groups[key] = (identify_license(text, name), text, {})
        groups[key][2][name] = copyrights
    return groups, without


def _model_order(name):
    stem = name.removesuffix('.xml')
    return (0, int(stem), '') if stem.isdigit() else (1, 0, stem)


def parse_header(content):
    """Extracts (copyright_lines, license_text) from the leading XML comment, or None."""
    comment = re.match(r'\s*(<\?xml[^>]*\?>)?\s*<!--(.*?)!?-->', content, re.DOTALL)
    if not comment:
        return None
    lines = comment.group(2).split('\n')
    lines[0] = lines[0].lstrip()
    disclaimer = [i for i, line in enumerate(lines) if line.strip() == 'LEGAL DISCLAIMER']
    if disclaimer:
        lines = lines[disclaimer[0] + 1:]
    copyrights = [' '.join(line.split()) for line in lines if COPYRIGHT_RE.match(line)]
    text = textwrap.dedent('\n'.join(line for line in lines if not COPYRIGHT_RE.match(line))).strip('\n')
    text = re.sub(r'\n{3,}', '\n\n', text)
    return (copyrights, text) if text else None


def identify_license(text, name):
    words = ' '.join(text.split())
    if 'Apache License' in words and 'Version 2.0' in words:
        return 'Apache-2.0'
    if 'Permission is hereby granted, free of charge' in words:
        return 'MIT'
    if 'Redistribution and use in source and binary forms' in words:
        return 'BSD-3-Clause' if 'Neither the name' in words else 'BSD-2-Clause'
    raise stop(
        f'the header of the LwM2M object model {name} is not a license that notices/lwm2m.py knows: '
        'Apache-2.0, MIT, BSD-2-Clause or BSD-3-Clause. Find out which license it is, add it to '
        f'identify_license() and to the license of {COMPONENT_NAME} in {POM_REL}.'
    )


def check_pom_license(licenses, pom_path):
    ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
    expressions = [
        comp.findtext('m:license', '', ns)
        for comp in ElementTree.parse(pom_path).getroot().iterfind('.//m:component', ns)
        if comp.findtext('m:name', '', ns) == COMPONENT_NAME
    ]
    if len(expressions) != 1:
        raise stop(
            f'{POM_REL} has {len(expressions)} bundled components {COMPONENT_NAME}, and expected exactly 1.'
        )
    given = set(expressions[0].split(' AND ')) if expressions[0] else set()
    if given != licenses:
        expected = ' AND '.join(sorted(licenses))
        raise stop(
            f'the license of the bundled component {COMPONENT_NAME} in {POM_REL} is "{expressions[0]}", '
            f'and the headers of the models give {expected}. Set it to "{expected}".'
        )


def format_notice(groups):
    blocks = [INTRODUCTION]
    for spdx, text, models in sorted(groups.values(), key=lambda g: (-len(g[2]), _model_order(next(iter(g[2]))))):
        title = f'{spdx} ({len(models)} {"model" if len(models) == 1 else "models"})'
        holders = {}
        for name, copyrights in models.items():
            for line in copyrights:
                holders.setdefault(line, []).append(name)
        lines = [title, '~' * len(title), '']
        for line, names in holders.items():
            lines.append(line)
            lines += textwrap.wrap(', '.join(names), 76, initial_indent='    ', subsequent_indent='    ',
                                   break_on_hyphens=False)
        lines += ['', text]
        blocks.append('\n'.join(lines))
    return '\n\n\n'.join(blocks)
