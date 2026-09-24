#!/usr/bin/env python3
"""Check resource coverage, format arguments and technical identifiers (stdlib only)."""
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

RES = Path(__file__).resolve().parents[1] / 'manager/src/main/res'
# Locale resource directories use a language/region qualifier or a BCP 47 tag.
# Configuration-only directories such as values-night and values-v31 inherit strings.
LOCALE_DIRECTORY = re.compile(r'values-(?:[a-z]{2}(?:-r[A-Z]{2})?|b\+[a-z]{2,3}(?:\+[A-Za-z0-9]+)*)')
FORMAT = re.compile(r'%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z%])')
IDENTIFIERS = re.compile(
    r'https?://[^\s<>"\)]+|(?:/[\w.-]+){2,}|'
    r'\b[a-zA-Z_]\w*(?:\.[a-zA-Z_]\w*)+\b|'
    r'PackageInfo\(Parcel\)|android:usesCleartextTraffic|ReVanced GmsCore|'
    r'\b(?:NPatch|LSPatch|LSPosed|Xposed|Shizuku|Vector|AppComponentFactory|'
    r'getPackageArchiveInfo|hasSigningCertificate|openat64|openat|Binder|MicroG)\b'
)
errors = []


def read(directory):
    entries = {}
    for path in sorted(directory.glob('*.xml')):
        for node in ET.parse(path).getroot():
            if node.tag not in ('string', 'plurals'):
                continue
            key = (node.tag, node.attrib['name'])
            if key in entries:
                errors.append(f'{directory.name}: duplicate {key}')
            entries[key] = node
    return entries


def formats(text):
    implicit = 0
    args = []
    for index, kind in FORMAT.findall(text):
        if kind == '%':
            continue
        if not index:
            implicit += 1
            index = str(implicit)
        args.append((int(index), kind.lower()))
    return Counter(args)


def check_text(locale, name, source, translated):
    if formats(source) != formats(translated):
        errors.append(f'{locale}/{name}: changed format arguments')
    for match in IDENTIFIERS.finditer(source):
        token = match.group().rstrip('.')
        if token in ('e.g', 'i.e'):
            continue
        if token not in translated:
            errors.append(f'{locale}/{name}: missing identifier {token!r}')
    if re.search(r'ZXQ\d|QXZ|⟦\d', translated):
        errors.append(f'{locale}/{name}: unrestored translation token')


source = read(RES / 'values')
required = {key: node for key, node in source.items() if node.get('translatable') != 'false'}
locales = {
    p.name: read(p) for p in sorted(RES.glob('values-*'))
    if p.is_dir() and LOCALE_DIRECTORY.fullmatch(p.name)
}
for locale, own in locales.items():
    base = locale.split('-r', 1)[0]
    inherited = source if base == 'values-en' else locales.get(base, {})
    available = inherited | own
    for key, original in required.items():
        translated = available.get(key)
        if translated is None:
            errors.append(f'{locale}: missing {key}')
            continue
        if key[0] == 'string':
            check_text(locale, key[1], original.text or '', translated.text or '')
        else:
            original_forms = {item.get('quantity'): item.text or '' for item in original}
            for item in translated:
                form = item.attrib['quantity']
                check_text(locale, key[1] + '/' + form,
                           original_forms.get(form, original_forms['other']), item.text or '')
if errors:
    print('\n'.join(errors), file=sys.stderr)
    sys.exit(1)
print(f'OK: {len(locales)} locale directories; {len(required)} translatable resources each; '
      'format arguments and technical identifiers preserved.')
