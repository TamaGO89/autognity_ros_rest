const browser_obj = (typeof chrome !== "undefined") ? chrome : browser;


let username;
let password;
let priv_key;

/** Set the visible content in the popup */
const update_popup = function() {
	if (true) if (typeof username !== 'undefined' && typeof password !== 'undefined' && typeof priv_key !== 'undefined')
			document.getElementById("popup_message").innerText = "You're already logged into the app";
		else browser_obj.storage.local.get(['username','password','priv_key'], function(result) {
				if (typeof result.username === 'undefined') document.getElementById("set_key_form").style.visibility="visible";
				else document.getElementById("get_key_form").style.visibility="visible"; });
	else document.getElementById("popup_message").innerText = "Wrong website or the website is corrupted"; };

const popup_set_key = function() {
	username = document.getElementById("set_user").value;
	password = document.getElementById("set_pass").value;
	let master_key = document.getElementById("set_key").value;
	if (username.length < 1 || password.length < 1 || master_key.length < 1) {
		username = password = undefined;
		document.getElementById("popup_message").innerText = "wrong credentials";
	} else browser_obj.tabs.query({active: true, currentWindow: true}, function(tabs) {
		browser_obj.tabs.sendMessage(tabs[0].id,
									 {type:"FROM_POPUP", user:btoa(username), pass:btoa(password), key:btoa(master_key)},
									 function(response){
			document.getElementById("popup_message").innerText = "waiting for approval"; }); });
	update_popup(); };

const popup_get_key = function() {
	let master_key = document.getElementById("get_key").value;
	browser_obj.tabs.query({active: true, currentWindow: true}, function(tabs) {
		browser_obj.tabs.sendMessage(tabs[0].id, {type:"FROM_POPUP", key:btoa(master_key)}, function(response){
			document.getElementById("popup_message").innerText = "waiting for approval"; }); });
	update_popup(); };

document.addEventListener("DOMContentLoaded", function(event) {
	document.getElementById("set_key_form").style.visibility = "hidden";
	document.getElementById("set_key_form").onsubmit = popup_set_key;
	document.getElementById("get_key_form").style.visibility = "visible";
	document.getElementById("get_key_form").onsubmit = popup_get_key;
	update_popup();
});