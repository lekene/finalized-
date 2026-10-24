#!/usr/bin/env python3
import xml.etree.ElementTree as ET
import re

# Read all 616 keys from values-en/strings.xml
en_tree = ET.parse('app/src/main/res/values-en/strings.xml')
all_elements = en_tree.getroot().findall('string')
all_keys = [e.get('name') for e in all_elements]

print(f"Total keys to map: {len(all_keys)}")
