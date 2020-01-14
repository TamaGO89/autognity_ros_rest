const browser_obj = (typeof chrome !== "undefined") ? chrome : browser;
let forge_elem = document.createElement("script");
forge_elem.src = browser_obj.extension.getURL("forge/forge.all.min.js");
let script_init = setInterval(function() {
    if (document.querySelector("head") !== null) {
		document.querySelector("head").appendChild(forge_elem);
        clearInterval(script_init);
    } }, 2000);
