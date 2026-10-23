#!/usr/bin/env python3
"""Verifies the Android launcher foreground is a 1:1 conversion of design/logo.svg
and that design/logo.svg is still identical to the canonical landing-page asset (if given)."""
import re, sys, hashlib, xml.etree.ElementTree as ET
root = __file__.rsplit('/tools/', 1)[0]
svg = ET.parse(f'{root}/design/logo.svg').getroot()
sp = next(e for e in svg.iter() if e.tag.endswith('path'))
A = '{http://schemas.android.com/apk/res/android}'
vec = ET.parse(f'{root}/app/src/main/res/drawable/ic_launcher_foreground.xml').getroot()
vp = next(e for e in vec.iter() if e.tag == 'path')
norm = lambda d: re.sub(r'\s+', ' ', d.strip())
checks = {
    'path data': norm(sp.get('d')) == norm(vp.get(A + 'pathData')),
    'stroke colour': sp.get('stroke').lower() == vp.get(A + 'strokeColor').lower(),
    'stroke width': sp.get('stroke-width') == vp.get(A + 'strokeWidth'),
    'line cap': sp.get('stroke-linecap') == vp.get(A + 'strokeLineCap'),
    'line join': sp.get('stroke-linejoin') == vp.get(A + 'strokeLineJoin'),
    'no fill': sp.get('fill') == 'none' and vp.get(A + 'fillColor') == '#00000000',
}
if len(sys.argv) > 1:
    h = lambda p: hashlib.sha256(open(p, 'rb').read()).hexdigest()
    checks['svg identical to ' + sys.argv[1]] = h(sys.argv[1]) == h(f'{root}/design/logo.svg')
for k, v in checks.items(): print(('OK   ' if v else 'FAIL ') + k)
sys.exit(0 if all(checks.values()) else 1)
