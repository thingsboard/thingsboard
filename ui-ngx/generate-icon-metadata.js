/*
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
const fs = require('fs');
const path = require('path');

const materialIconDir = path.join('.', 'src', 'assets', 'metadata');
const mdiMetadata = path.join('.', 'node_modules', '@mdi', 'svg', 'meta.json');

async function init() {
  const iconsBundle = JSON.parse(await fs.promises.readFile(path.join(materialIconDir, 'material-icons.json')));

  await getMaterialIconMetadataAndUpdated(iconsBundle);
  await getMDIMetadataAndUpdated(iconsBundle);

  await fs.promises.writeFile(path.join(materialIconDir, 'material-icons.json'), JSON.stringify(iconsBundle), 'utf8')
}

async function getMaterialIconMetadataAndUpdated(iconsBundle){
  const iconsResponse = await fetch('https://fonts.google.com/metadata/icons?key=material_symbols&incomplete=true');
  const iconsText = await iconsResponse.text();
  const clearText = iconsText.substring(iconsText.indexOf("\n") + 1);

  const icons = JSON.parse(clearText).icons;

  let prevItem;
  const filterIcons = icons.filter((item) => {
    if (prevItem?.name !== item.name && !item.unsupported_families.includes('Material Icons')) {
      prevItem = item;
      return true;
    }
    return false;
  });

  filterIcons.forEach((item, index) => {
    const findItem = iconsBundle.find((el) => el.name === item.name);
    if (!findItem) {
      let prevIndexIcon = 0;
      if (index === 0) {
        prevIndexIcon = 45;
      } else {
        let iteration = 0;
        while (prevIndexIcon < 45) {
          iteration++;
          const prevIconName = filterIcons[index - iteration].name;
          prevIndexIcon = findPreviousIcon(iconsBundle, prevIconName);
        }
      }
      if (prevIndexIcon >= 0) {
        iconsBundle.splice(prevIndexIcon + 1, 0, {name:item.name, tags:item.tags});
      }
      console.log('Not found icon:', item.name);
      console.count('Not found material icon');
      return;
    }
    if (JSON.stringify(item.tags) !== JSON.stringify(findItem.tags)) {
      findItem.tags = item.tags;
      console.log('Difference tags in', item.name);
      console.count('Difference tags in material icon');
    }
  });
}

async function getMDIMetadataAndUpdated(iconsBundle){
  const mdiBundle = JSON.parse(await fs.promises.readFile(mdiMetadata));

  iconsBundle
    .filter(item => item.name.startsWith('mdi:'))
    .forEach(item => {
      const iconName = item.name.substring(item.name.indexOf(":") + 1);
      const findItem = mdiBundle.find((el) => el.name === iconName);
      if (!findItem) {
        console.error('Delete icon:', item.name);
      }
    });


  mdiBundle.forEach((item, index) => {
    const iconName = `mdi:${item.name}`
    let iconTags = item.tags;
    const iconAliases = item.aliases.map(item => item.replaceAll('-', ' '));
    if (!iconTags.length && item.aliases.length)  {
      iconTags = iconAliases;
    } else if (item.aliases.length) {
      iconTags = iconTags.concat(iconAliases);
    }
    iconTags = iconTags.map(item => item.toLowerCase());

    const findItem = iconsBundle.find((el) => el.name === iconName);
    if (!findItem) {
      let prevIndexIcon;
      if (index === 0) {
        prevIndexIcon = iconsBundle.findIndex(item => item.name.startsWith('mdi:'))
      } else {
        const prevIconName = `mdi:${mdiBundle[index - 1].name}`;
        prevIndexIcon = findPreviousIcon(iconsBundle, prevIconName);
      }
      if (prevIndexIcon >= 0) {
        iconsBundle.splice(prevIndexIcon + 1, 0, {name:iconName, tags:iconTags});
      }
      console.log('Not found icon:', iconName);
      console.count('Not found mdi icon');
      return;
    }
    if (JSON.stringify(iconTags) !== JSON.stringify(findItem.tags)) {
      findItem.tags = iconTags;
      console.log('Difference tags in', iconName);
      console.count('Difference tags in mdi icon');
    }
  });
}

function findPreviousIcon(iconsBundle, findName) {
  return iconsBundle.findIndex(item => item.name === findName);
}

init();
