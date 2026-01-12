# Compatibility Matrix

## Android

- **Minimum API level**: 21 (Android 5.0 Lollipop)
- **Target API level**: 34 (aligns with current Android toolchain defaults)
- **Notes**: Rendering relies on standard Android graphics APIs; no native code required.

## Web Browsers

The web library targets modern evergreen browsers with ES6 module support.

- **Chrome**: latest two major versions
- **Firefox**: latest two major versions
- **Safari**: latest two major versions
- **Edge**: latest two major versions

Legacy browsers without ES6 modules (e.g., IE11) are not supported.

## Node.js

- **Minimum Node.js version**: 18.x (per `web/package.json` engine requirements)
- **Recommended**: latest LTS
