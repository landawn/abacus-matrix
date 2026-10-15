#!/usr/bin/env node
"use strict";
require("./organize-tests").main([...process.argv.slice(2), "--dedupe"]);
