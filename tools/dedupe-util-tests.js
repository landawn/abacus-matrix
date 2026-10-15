#!/usr/bin/env node
"use strict";
// Compatibility entry point. Matrix sources moved out of the former util package.
require("./organize-tests").main([...process.argv.slice(2), "--dedupe"]);
