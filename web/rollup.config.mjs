import resolve from "@rollup/plugin-node-resolve";
import commonjs from "@rollup/plugin-commonjs";

const plugins = [resolve(), commonjs()];

export default [
  {
    input: "dist/index.js",
    output: [
      { file: "dist/grin.js", format: "es", sourcemap: true },
      { file: "dist/grin.cjs", format: "cjs", sourcemap: true, exports: "named" },
      { file: "dist/grin.umd.js", format: "umd", name: "Grin", sourcemap: true },
    ],
    plugins,
  },
  {
    input: "dist/player.js",
    output: [
      { file: "dist/grin-player.js", format: "es", sourcemap: true },
      { file: "dist/grin-player.cjs", format: "cjs", sourcemap: true, exports: "named" },
    ],
    plugins,
  },
];
